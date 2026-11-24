# Project 4: Secure Online Communication With TLS and MITM Protection

`pseudocode/` holds the five pseudocode figures from the report. `demo/` holds a small runnable Python version that proves the man-in-the-middle (MITM) defenses work.

| Pseudocode file | Figure | Purpose |
|---|---|---|
| `01_secure_messaging_system.txt` | 1 | Client send and server handling: fail closed, token auth, replay protection |
| `02_tls_server_setup.txt` | 2 | TLS 1.2+ only, ECDHE + AEAD suites, OCSP stapling, mutual TLS, HTTP→HTTPS, HSTS |
| `03_tls13_handshake.txt` | 3 | What a TLS 1.3 handshake does (key share, certificate, CertificateVerify, Finished, forward secrecy) |
| `04_tls_client_connect.txt` | 4 | Client: verify chain, hostname, revocation, minimum version, then pin; no "continue anyway" |
| `05_mitm_defenses.txt` | 5 | Public-key pinning with a backup pin, client-certificate checks, Certificate Transparency monitoring |

## Run the demo on Linux (Python 3.10+)

```bash
pip install cryptography
cd demo
bash gen_certs.sh        # creates certs/ (private keys; never commit them)
python3 tls_demo.py      # runs a real server, four attacker servers, and the clients
```

`demo/out_tls_demo.txt` holds the saved output. The secure client delivers its message over TLS 1.3. The insecure client (certificate checks off) leaks the message to the attacker. The secure client refuses a rogue CA, a wrong host name, a mis-issued certificate (caught by the pin), and a TLS 1.1 downgrade. The server refuses a client with no device certificate.
