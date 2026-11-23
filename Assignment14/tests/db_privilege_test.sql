-- Connect as the web app's account and try things it must not be able to do.
-- Usage: PGPASSWORD="$APP_DB_PASSWORD" psql -h 127.0.0.1 -U customer_app \
--          -d customer_portal -f tests/db_privilege_test.sql
\echo '--- allowed: read public columns'
SELECT full_name, city FROM customers ORDER BY customer_id LIMIT 2;
\echo '--- denied: read email addresses'
SELECT email FROM customers;
\echo '--- denied: delete rows'
DELETE FROM customers;
\echo '--- denied: change rows'
UPDATE customers SET city = 'Nowhere';
\echo '--- denied: drop the table'
DROP TABLE customers;
