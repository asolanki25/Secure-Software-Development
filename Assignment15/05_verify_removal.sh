#!/usr/bin/env bash
# Run after 04_revoke_and_disable.sql to confirm access was really removed.
as() { PGPASSWORD="$2" psql -w -h 127.0.0.1 -d smithtown_pd -U "$1" -X -t -A \
       -c "$3" 2>&1 | sed 's/^/    /'; }
echo "== rgarcia after REVOKE record_entry"
echo "  insert a watch order:"; as rgarcia "$PW_RGARCIA" \
  "INSERT INTO pd.watch_orders (resident_id, start_date, end_date) VALUES (1, '2027-01-05', '2027-01-06');"
echo "== jlee after ALTER ROLE ... NOLOGIN"
echo "  log in:"; as jlee "$PW_JLEE" "SELECT 1;"
echo "== mkhan (unchanged)"
echo "  insert a watch order:"; as mkhan "$PW_MKHAN" \
  "INSERT INTO pd.watch_orders (resident_id, start_date, end_date) VALUES (1, '2027-01-05', '2027-01-06');"
