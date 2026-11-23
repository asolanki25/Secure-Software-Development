#!/usr/bin/env bash
# Sends normal and hostile requests to the running site and prints the results.
# Usage: bash tests/attack_tests.sh http://127.0.0.1:8080
BASE="${1:-http://127.0.0.1:8080}"
JAR="$(mktemp)"
say() { printf '\n=== %s ===\n' "$1"; }

say "1. Get a CSRF token (starts a session)"
TOKEN=$(curl -s -c "$JAR" -b "$JAR" "$BASE/api.php?action=csrf" \
        | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
echo "token length: ${#TOKEN}"

post() {  # $1 = JSON body, $2 = token
  curl -s -w '  [HTTP %{http_code}]\n' -b "$JAR" -c "$JAR" \
       -H 'Content-Type: application/json' -H "X-CSRF-Token: $2" \
       --data "$1" "$BASE/api.php?action=add"
}

say "2. Normal insert (apostrophe in name)"
post '{"full_name":"Sean D'"'"'Souza","email":"sean@example.com","city":"Jersey City"}' "$TOKEN"

say "3. Insert without a CSRF token (forged request)"
post '{"full_name":"Eve","email":"eve@example.com","city":"Newark"}' ""

say "4. SQL injection in the name field"
post '{"full_name":"x'"'"'); DROP TABLE customers;--","email":"a@example.com","city":"Newark"}' "$TOKEN"

say "5. Stored XSS attempt in the city field"
post '{"full_name":"Alex Kim","email":"alex@example.com","city":"<img src=x onerror=alert(1)>"}' "$TOKEN"

say "6. Oversized input (5,000 characters)"
post "{\"full_name\":\"$(printf 'A%.0s' {1..5000})\",\"email\":\"b@example.com\",\"city\":\"Hoboken\"}" "$TOKEN"

say "7. Duplicate email"
post '{"full_name":"Sean Again","email":"sean@example.com","city":"Hoboken"}' "$TOKEN"

search() { curl -s -w '  [HTTP %{http_code}]\n' -b "$JAR" --get \
           --data-urlencode "action=search" --data-urlencode "q=$1" "$BASE/api.php"; }

say "8. Normal search for O'Brien"
search "O'Brien"

say "9. SQL injection in search: ' OR '1'='1"
search "' OR '1'='1"

say "10. UNION injection in search"
search "' UNION SELECT email,email,email FROM customers--"

say "11. Wildcard-only search: %"
search "%"

say "12. Reflected XSS in search text"
search "<script>alert(1)</script>"

say "13. Stored hostile name comes back escaped inside JSON"
search "Legacy"

say "14. Security headers on the page"
curl -s -D - -o /dev/null "$BASE/" | grep -i -E 'content-security|x-content|referrer'

say "15. Files outside the allow-list"
curl -s -o /dev/null -w '/../src/db.php -> HTTP %{http_code}\n' "$BASE/../src/db.php"
curl -s -o /dev/null -w '/router.php -> HTTP %{http_code}\n' "$BASE/router.php"
rm -f "$JAR"
