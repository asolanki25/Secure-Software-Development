-- Assignment 14: database objects for the secure customer directory.
-- Run as a database administrator, for example:
--   sudo -u postgres psql -v app_password="$APP_DB_PASSWORD" -f sql/schema.sql
\set ON_ERROR_STOP on

DROP DATABASE IF EXISTS customer_portal;
DROP ROLE IF EXISTS customer_app;
CREATE ROLE customer_app LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE;   -- the web app's only account
CREATE DATABASE customer_portal OWNER postgres;
\connect customer_portal

REVOKE ALL ON SCHEMA public FROM PUBLIC;   -- no default rights for anyone
GRANT USAGE ON SCHEMA public TO customer_app;

CREATE TABLE customers (
    customer_id  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name    VARCHAR(100) NOT NULL
                 CHECK (char_length(btrim(full_name)) BETWEEN 1 AND 100),
    email        VARCHAR(254) NOT NULL UNIQUE
                 CHECK (email ~ '^[^@[:space:]]+@[^@[:space:]]+\.[A-Za-z]{2,}$'),
    city         VARCHAR(60)  NOT NULL
                 CHECK (city ~ '^[A-Za-z][A-Za-z .''-]{0,59}$'),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Least privilege: the app may read and add customers, nothing else.
REVOKE ALL ON customers FROM PUBLIC;
GRANT SELECT (customer_id, full_name, city, created_at) ON customers
      TO customer_app;                     -- email is not readable by the app
GRANT INSERT (full_name, email, city) ON customers TO customer_app;

-- Seed row so a search has something to find.
INSERT INTO customers (full_name, email, city)
VALUES ('Mary O''Brien', 'mary.obrien@example.com', 'Hoboken');
