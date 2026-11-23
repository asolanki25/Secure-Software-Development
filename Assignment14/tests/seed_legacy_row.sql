-- Simulates hostile data that reached the table some other way (an old import
-- or an admin tool), to prove the page still shows it as plain text.
-- Usage: sudo -u postgres psql -d customer_portal -f tests/seed_legacy_row.sql
INSERT INTO customers (full_name, email, city)
VALUES ('Legacy <img src=x onerror=alert(1)>', 'legacy@example.com', 'Newark');
