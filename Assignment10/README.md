# Assignment 10: Static Analysis Results and a Secure Client-Server Application

MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.

Chapter 10 (Application Review and Testing), Richardson and Thies; Chapter 12 (Security Testing), Kohnfelder.

## Files

- `SecureServer.java`, `SecureClient.java` — Question 2. A TLS server and client built with the Java Secure Socket Extension (JSSE). The server presents a certificate from its keystore; the client validates it against its truststore, giving an encrypted, server-authenticated channel.
- `run_output.txt` — real output of the handshake and message exchange (Java 21, TLS 1.3 negotiated).

The keystore and truststore are **not** committed, because a keystore holds a private key and secrets do not belong in source control. Generate them locally with the commands below.

## Generate the keystore and truststore, then run

```
# 1. Server keystore (self-signed cert for localhost)
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -validity 365 \
  -keystore server.keystore -storetype JKS -storepass password -keypass password \
  -dname "CN=localhost, OU=MSCS-535, O=University of the Cumberlands, C=US" -ext SAN=dns:localhost

# 2. Export the server cert and import it into the client truststore
keytool -exportcert -alias server -keystore server.keystore -storepass password -file server.cer
keytool -importcert -alias server -keystore client.truststore -storetype JKS -storepass password -file server.cer -noprompt

# 3. Compile and run (server in one terminal, client in another)
javac SecureServer.java SecureClient.java
java SecureServer      # terminal 1
java SecureClient      # terminal 2
```

Requires Java 8 or later. The hardcoded `password` and the JKS format are kept to match the provided code; the write-up explains why both should be changed (externalized secret, PKCS12 format) in production.
