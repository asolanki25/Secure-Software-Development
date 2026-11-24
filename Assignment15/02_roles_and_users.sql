-- Steps 2 to 6. Run as the DBA, passing passwords in from the environment:
--   sudo -u postgres psql -v pw_rgarcia="$PW_RGARCIA" -v pw_jlee="$PW_JLEE" \
--        -v pw_mkhan="$PW_MKHAN" -f 02_roles_and_users.sql
\set ON_ERROR_STOP on

-- Step 2: Connect to the new database
\connect smithtown_pd
\conninfo

-- Objects the roles will protect (owned by the DBA, not by any user)
CREATE SCHEMA pd AUTHORIZATION postgres;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE pd.residents (
    resident_id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    street        VARCHAR(120) NOT NULL,
    phone         VARCHAR(20),
    date_of_birth DATE                          -- sensitive
);
CREATE TABLE pd.watch_orders (
    order_id     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resident_id  INTEGER NOT NULL REFERENCES pd.residents,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL CHECK (end_date >= start_date),
    notes        VARCHAR(500),
    entered_by   NAME NOT NULL DEFAULT session_user   -- who added the row
);
-- A view that hides phone numbers and birth dates
CREATE VIEW pd.resident_directory WITH (security_barrier) AS
    SELECT resident_id, full_name, street FROM pd.residents;

-- Step 3: Create users (login accounts for individual people)
CREATE USER rgarcia WITH PASSWORD :'pw_rgarcia'
    VALID UNTIL '2027-02-28' CONNECTION LIMIT 2;   -- records clerk
CREATE USER jlee    WITH PASSWORD :'pw_jlee'
    VALID UNTIL '2027-02-28' CONNECTION LIMIT 2;   -- patrol officer
CREATE USER mkhan   WITH PASSWORD :'pw_mkhan'
    VALID UNTIL '2027-02-28' CONNECTION LIMIT 2;   -- records supervisor

-- Step 4: Create roles (groups of privileges that cannot log in)
CREATE ROLE record_entry       NOLOGIN;   -- add and update records
CREATE ROLE records_viewer     NOLOGIN;   -- read the directory only
CREATE ROLE records_supervisor NOLOGIN;   -- record_entry plus delete

-- Step 5: Grant permissions to roles (never directly to users)
GRANT CONNECT ON DATABASE smithtown_pd
      TO record_entry, records_viewer, records_supervisor;
GRANT USAGE ON SCHEMA pd TO record_entry, records_viewer, records_supervisor;

GRANT SELECT, INSERT, UPDATE ON pd.residents, pd.watch_orders TO record_entry;
GRANT SELECT ON pd.resident_directory TO records_viewer;   -- view, not table

GRANT record_entry TO records_supervisor;                  -- role hierarchy
GRANT DELETE ON pd.watch_orders TO records_supervisor;

-- Step 6: Assign roles to users
GRANT record_entry       TO rgarcia;
GRANT records_viewer     TO jlee;
GRANT records_supervisor TO mkhan;

-- Sample data entered by the DBA
INSERT INTO pd.residents (full_name, street, phone, date_of_birth)
VALUES ('Ana Ortiz', '12 Elm St', '555-0101', '1960-04-02');
