# Assignment 5: Simple Customer Database in SQL

MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.

## Files

- `create_customer_db.sql`: PostgreSQL 16 script (tested). It creates the `customer_db` database and a `customer` table with name and address fields, adds CHECK constraints on state and ZIP code, creates a least-privilege `customer_app` role (SELECT and INSERT only), inserts two sample rows, and shows that an invalid ZIP code is rejected.
- `create_customer_oracle_freesql.sql`: the same table in Oracle syntax, for Oracle FreeSQL (https://freesql.com, formerly Oracle Live SQL). The textbook's examples use Oracle. FreeSQL provides a ready-made schema, so the script creates the table directly and skips the role statements.

- `CustomerDatabase.java`: Java JDBC version. It creates the same database and table from Java, inserts rows with a PreparedStatement (including an SQL injection attempt that is stored as plain text), and shows that an invalid ZIP code is rejected. It reads connection settings from environment variables, so no credentials are in the code. GitHub Actions compiles and runs it on every push (`.github/workflows/build.yml`).
- `postgresql_output.txt`: the real output of `create_customer_db.sql`.

## Run (PostgreSQL)

```
psql -U postgres -f create_customer_db.sql
```

The last statement ends with an expected `check constraint "customer_zip_check"` error. It demonstrates that the constraint works.

To confirm the restricted role:

```
psql -U customer_app -d customer_db -c "SELECT count(*) FROM customer;" -c "DELETE FROM customer;"
```

The count returns 2, and the DELETE fails with `permission denied for table customer`.

The role password in the script is a placeholder. Do not commit real credentials.

## Screenshots (Oracle FreeSQL)

- `freesql_query_result.png`: the two inserted rows returned by the SELECT statement.
- `freesql_script_output.png`: the script output ending with the expected ORA-02290 error for ZIP code 3360A.
