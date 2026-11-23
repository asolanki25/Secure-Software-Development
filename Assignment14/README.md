# Assignment 14: Secure Website With HTML, JavaScript, PHP, and SQL

A small customer directory (add a customer, search by name) built to show secure code in each of the four layers. Course: MSCS-535-B01 Secure Software Development.

| Layer | File | Main protections |
|---|---|---|
| HTML | `public/index.html` | No inline script; `maxlength`, `pattern`, and `required` limits; labels; `no-referrer`; a POST form |
| JavaScript | `public/app.js` | Output written with `textContent` only; CSRF token sent in a header; `encodeURIComponent`; client-side checks for usability only |
| PHP | `public/api.php`, `src/db.php` | PDO prepared statements (emulation off); length-first allow-list validation; CSRF check with `hash_equals`; HttpOnly + SameSite cookie; JSON hex-escaping; generic errors; credentials from the environment |
| SQL | `sql/schema.sql` | Dedicated login role; `REVOKE ... FROM PUBLIC`; column-level `GRANT` (the app can't read emails, can't update, delete, or drop); `CHECK` and `UNIQUE` constraints |

`router.php` is only for PHP's built-in development server. It sends the security headers (CSP, nosniff, Referrer-Policy) and serves only the four allowed files. In production, Apache or Nginx does this job.

## Run it on Linux (tested with PHP 8.3.6, PostgreSQL 16, Chrome)

```bash
sudo apt install php-cli php-pgsql postgresql      # if not installed
export APP_DB_PASSWORD="$(openssl rand -hex 16)"    # never hard-code it

# 1. Create the role, database, and table
sudo -u postgres psql -v app_password="$APP_DB_PASSWORD" -f sql/schema.sql

# 2. Start the site (the password is passed through the environment)
DB_PASSWORD="$APP_DB_PASSWORD" php -S 127.0.0.1:8080 -t public router.php
# open http://127.0.0.1:8080 in a browser
```

## Run the tests (in a second terminal, same folder)

```bash
export APP_DB_PASSWORD=...   # the same value as above
sudo -u postgres psql -d customer_portal -f tests/seed_legacy_row.sql   # hostile row
bash tests/attack_tests.sh http://127.0.0.1:8080          # 15 HTTP tests
pip install playwright && python3 tests/browser_check.py http://127.0.0.1:8080
PGPASSWORD="$APP_DB_PASSWORD" psql -h 127.0.0.1 -U customer_app \
    -d customer_portal -f tests/db_privilege_test.sql      # least privilege
```

The saved results of these runs are in `tests/attack_output.txt`, `tests/browser_output.txt`, `tests/db_privilege_output.txt`, and the two screenshots.
