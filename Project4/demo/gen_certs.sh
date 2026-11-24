#!/usr/bin/env bash
# Creates every certificate the demo needs, in ./certs (private keys stay local).
set -euo pipefail
mkdir -p certs && cd certs
key() { openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$1" 2>/dev/null; }
ca()  { key "$1.key"; openssl req -x509 -new -key "$1.key" -sha256 -days 30 \
          -subj "/CN=$2" -addext "basicConstraints=critical,CA:TRUE" \
          -addext "keyUsage=critical,keyCertSign,cRLSign" -out "$1.crt"; }
leaf() {  # $1 name  $2 CA  $3 CN  $4 SAN  $5 extendedKeyUsage
  key "$1.key"
  openssl req -new -key "$1.key" -subj "/CN=$3" -out "$1.csr"
  printf "subjectAltName=%s\nextendedKeyUsage=%s\nbasicConstraints=CA:FALSE\n" \
         "$4" "$5" > "$1.ext"
  openssl x509 -req -in "$1.csr" -CA "$2.crt" -CAkey "$2.key" -CAcreateserial \
          -days 30 -sha256 -extfile "$1.ext" -out "$1.crt" 2>/dev/null
}
ca  trusted_ca   "Demo Trusted Root CA"          # the CA clients trust
ca  device_ca    "Demo Device CA"                # issues client certificates
ca  attacker_ca  "Attacker Root CA"              # NOT trusted by clients
leaf server       trusted_ca  localhost "DNS:localhost,IP:127.0.0.1" serverAuth
leaf client       device_ca   device-001 "DNS:device-001" clientAuth
leaf mitm_rogue   attacker_ca localhost "DNS:localhost,IP:127.0.0.1" serverAuth
leaf wrong_name   trusted_ca  evil.example "DNS:evil.example" serverAuth
leaf misissued    trusted_ca  localhost "DNS:localhost,IP:127.0.0.1" serverAuth
rm -f *.csr *.ext *.srl
echo "Certificates created in $(pwd)"
