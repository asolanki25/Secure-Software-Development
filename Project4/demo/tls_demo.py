"""Project 4 demo: a TLS message server, a client that follows the pseudocode,
and five man-in-the-middle (MITM) attempts. Run gen_certs.sh first.
Usage: python3 tls_demo.py
"""
import base64, hashlib, socket, ssl, threading, time, warnings
from cryptography import x509
from cryptography.hazmat.primitives import serialization

warnings.filterwarnings("ignore", category=DeprecationWarning)  # TLS 1.1 test
C = "certs/"
HOST = "localhost"
captured_by_attacker = []          # anything the attacker manages to read


def spki_hash(der_cert):
    """Base64 SHA-256 of the certificate's public key (Figure 5, SpkiHash)."""
    cert = x509.load_der_x509_certificate(der_cert)
    spki = cert.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo)
    return base64.b64encode(hashlib.sha256(spki).digest()).decode()


with open(C + "server.crt", "rb") as f:          # pin the real server's key
    PINS = {HOST: [spki_hash(ssl.PEM_cert_to_DER_cert(f.read().decode()))]}


# ---------------------------------------------------------------- servers
def server_context(cert, key, require_client_cert, old_protocol=False):
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    if old_protocol:                               # attacker forcing a downgrade
        ctx.minimum_version = ssl.TLSVersion.MINIMUM_SUPPORTED
        ctx.maximum_version = ssl.TLSVersion.TLSv1_1
        ctx.set_ciphers("DEFAULT:@SECLEVEL=0")
    else:                                          # Figure 2
        ctx.minimum_version = ssl.TLSVersion.TLSv1_2
        ctx.set_ciphers("ECDHE+AESGCM:ECDHE+CHACHA20")
        ctx.options |= ssl.OP_NO_COMPRESSION | ssl.OP_NO_RENEGOTIATION
    ctx.load_cert_chain(C + cert, C + key)
    if require_client_cert:                        # mutual TLS
        ctx.verify_mode = ssl.CERT_REQUIRED
        ctx.load_verify_locations(C + "device_ca.crt")
    return ctx


def serve(port, ctx, handler):
    lsock = socket.create_server(("127.0.0.1", port))
    def loop():
        while True:
            raw, _ = lsock.accept()
            try:
                with ctx.wrap_socket(raw, server_side=True) as tls:
                    handler(tls)
            except (ssl.SSLError, OSError):
                pass                                # failed handshakes end here
    threading.Thread(target=loop, daemon=True).start()


def message_server(tls):                           # Figure 1, ServerHandleMessage
    peer = tls.getpeercert()
    device = dict(x[0] for x in peer["subject"])["commonName"]
    msg = tls.recv(4096).decode()
    tls.sendall(f"201 Delivered (from {device}): {msg}".encode())


def attacker(tls):                                 # MITM reads and fakes a reply
    captured_by_attacker.append(tls.recv(4096).decode())
    tls.sendall(b"201 Delivered")


# ---------------------------------------------------------------- clients
def secure_send(port, message, client_cert=True):
    """Follows Figure 4: verify chain, hostname, minimum version, then the pin."""
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)  # CERT_REQUIRED + hostname check
    ctx.minimum_version = ssl.TLSVersion.TLSv1_2
    ctx.load_verify_locations(C + "trusted_ca.crt")
    if client_cert:
        ctx.load_cert_chain(C + "client.crt", C + "client.key")
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=5) as raw:
            with ctx.wrap_socket(raw, server_hostname=HOST) as tls:
                if spki_hash(tls.getpeercert(binary_form=True)) not in PINS[HOST]:
                    return "REFUSED by client: server key does not match the pin"
                tls.sendall(message.encode())
                reply = tls.recv(4096).decode()
                if not reply:
                    return "REFUSED by server: connection closed"
                return f"OK {tls.version()} {tls.cipher()[0]} | {reply}"
    except ssl.SSLCertVerificationError as e:
        return f"REFUSED by client: {e.verify_message}"
    except ssl.SSLError as e:
        return f"REFUSED: {e.reason}"
    except OSError as e:
        return f"REFUSED: {e.__class__.__name__}"


def insecure_send(port, message):
    """The classic mistake: certificate checks turned off."""
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    with socket.create_connection(("127.0.0.1", port), timeout=5) as raw:
        with ctx.wrap_socket(raw, server_hostname=HOST) as tls:
            tls.sendall(message.encode())
            return "OK (client thinks) | " + tls.recv(4096).decode()


# ---------------------------------------------------------------- run
if __name__ == "__main__":
    serve(8443, server_context("server.crt", "server.key", True), message_server)
    serve(8444, server_context("mitm_rogue.crt", "mitm_rogue.key", False), attacker)
    serve(8445, server_context("wrong_name.crt", "wrong_name.key", False), attacker)
    serve(8446, server_context("misissued.crt", "misissued.key", False), attacker)
    try:
        serve(8447, server_context("mitm_rogue.crt", "mitm_rogue.key", False,
                                   old_protocol=True), attacker)
    except ssl.SSLError as e:
        print("note: TLS 1.1 server could not start:", e.reason)
    time.sleep(0.5)

    MSG = "Meet at 3 pm. Door code 4471."
    print(f"Pinned server key (SPKI SHA-256): {PINS[HOST][0]}\n")
    tests = [
        ("1. Secure client -> real server", lambda: secure_send(8443, MSG)),
        ("2. INSECURE client -> MITM (rogue CA)", lambda: insecure_send(8444, MSG)),
        ("3. Secure client -> MITM (rogue CA)", lambda: secure_send(8444, MSG)),
        ("4. Secure client -> wrong host name", lambda: secure_send(8445, MSG)),
        ("5. Secure client -> mis-issued cert", lambda: secure_send(8446, MSG)),
        ("6. Secure client -> TLS 1.1 downgrade", lambda: secure_send(8447, MSG)),
        ("7. No client cert -> real server",
         lambda: secure_send(8443, MSG, client_cert=False)),
    ]
    for name, run in tests:
        before = len(captured_by_attacker)
        result = run()
        leaked = captured_by_attacker[before:]
        print(name)
        print("   result:  ", result)
        print("   attacker read:", leaked[0] if leaked else "nothing")
