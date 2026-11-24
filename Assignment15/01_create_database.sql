-- Step 1: Create the database (run as a database administrator).
--   sudo -u postgres psql -f 01_create_database.sql
\set ON_ERROR_STOP on
DROP DATABASE IF EXISTS smithtown_pd;           -- allows the script to be rerun
DROP ROLE IF EXISTS rgarcia, jlee, mkhan,
    record_entry, records_viewer, records_supervisor;
CREATE DATABASE smithtown_pd
    OWNER    postgres
    ENCODING 'UTF8';
-- By default every role may connect and create temporary tables. Remove that.
REVOKE CONNECT, TEMPORARY ON DATABASE smithtown_pd FROM PUBLIC;
