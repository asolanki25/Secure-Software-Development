-- =====================================================================
-- Assignment 5, Question 1: A simple database with a name and address table
-- Course : MSCS-535 Secure Software Development (Fall 2026)
-- Author : Ananya Solanki
-- DBMS   : PostgreSQL 16 (run with: psql -U postgres -f create_customer_db.sql)
-- =====================================================================

-- Step 1 (DDL): create the database itself and connect to it.
DROP DATABASE IF EXISTS customer_db;
CREATE DATABASE customer_db;
\connect customer_db

-- Step 2 (DDL): create one table that stores a name and an address.
-- The layout follows the customer table in Richardson and Thies (2012, Chapter 5).
CREATE TABLE customer (
    cust_num    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name  VARCHAR(30)  NOT NULL,
    last_name   VARCHAR(30)  NOT NULL,
    street      VARCHAR(60)  NOT NULL,
    city        VARCHAR(30)  NOT NULL,
    state       CHAR(2)      NOT NULL CHECK (state ~ '^[A-Z]{2}$'),
    zip         CHAR(5)      NOT NULL CHECK (zip ~ '^[0-9]{5}$')
);

-- Step 3 (DCL): an application account that can read and add rows only.
-- In practice the password is supplied from a secrets store, not written in a script.
DROP ROLE IF EXISTS customer_app;
CREATE ROLE customer_app LOGIN PASSWORD 'change_me_outside_source_control';
REVOKE ALL ON customer FROM PUBLIC;
GRANT SELECT, INSERT ON customer TO customer_app;

-- Step 4 (DML): add sample rows.
INSERT INTO customer (first_name, last_name, street, city, state, zip)
VALUES ('John',    'Smith', '112 Anywhere St', 'Miami',   'FL', '33333'),
       ('Christy', 'Jones', '2 Singleton Ave', 'Orlando', 'FL', '32801');

-- Step 5 (DML): confirm the table structure and contents.
\d customer
SELECT cust_num, first_name, last_name, street, city, state, zip
FROM customer
ORDER BY cust_num;

-- Step 6: show that the CHECK constraint rejects a malformed ZIP code.
INSERT INTO customer (first_name, last_name, street, city, state, zip)
VALUES ('Test', 'User', '1 Main St', 'Tampa', 'FL', '3360A');
