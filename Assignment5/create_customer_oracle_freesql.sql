-- =====================================================================
-- Assignment 5, Question 1 (Oracle version for Oracle FreeSQL / Live SQL)
-- Course : MSCS-535 Secure Software Development (Fall 2026)
-- Author : Ananya Solanki
-- Run at https://freesql.com (formerly Oracle Live SQL). The site gives each
-- user a ready-made schema, so the table is created there directly.
-- =====================================================================

-- Step 1 (DDL): remove an earlier copy of the table if one exists.
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE customer PURGE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

-- Step 2 (DDL): create one table that stores a name and an address.
CREATE TABLE customer (
    cust_num    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name  VARCHAR2(30) NOT NULL,
    last_name   VARCHAR2(30) NOT NULL,
    street      VARCHAR2(60) NOT NULL,
    city        VARCHAR2(30) NOT NULL,
    state       CHAR(2)      NOT NULL CONSTRAINT customer_state_chk CHECK (REGEXP_LIKE(state, '^[A-Z]{2}$')),
    zip         CHAR(5)      NOT NULL CONSTRAINT customer_zip_chk   CHECK (REGEXP_LIKE(zip, '^[0-9]{5}$'))
);

-- Step 3 (DML): add sample rows.
INSERT INTO customer (first_name, last_name, street, city, state, zip)
VALUES ('John', 'Smith', '112 Anywhere St', 'Miami', 'FL', '33333');

INSERT INTO customer (first_name, last_name, street, city, state, zip)
VALUES ('Christy', 'Jones', '2 Singleton Ave', 'Orlando', 'FL', '32801');

COMMIT;

-- Step 4 (DML): confirm the table contents.
SELECT cust_num, first_name, last_name, street, city, state, zip
FROM customer
ORDER BY cust_num;

-- Step 5: show that the CHECK constraint rejects a malformed ZIP code.
-- Expected result: ORA-02290: check constraint (...CUSTOMER_ZIP_CHK) violated
INSERT INTO customer (first_name, last_name, street, city, state, zip)
VALUES ('Test', 'User', '1 Main St', 'Tampa', 'FL', '3360A');
