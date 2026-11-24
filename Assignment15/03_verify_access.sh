#!/usr/bin/env bash
# Logs in as each user and tries actions that should succeed or fail.
# Usage: PW_RGARCIA=... PW_JLEE=... PW_MKHAN=... bash 03_verify_access.sh
as() {  # $1 user, $2 password, $3 SQL
  PGPASSWORD="$2" psql -w -h 127.0.0.1 -d smithtown_pd -U "$1" -X -t -A \
       -c "$3" 2>&1 | sed 's/^/    /'
}
echo "== rgarcia (record_entry)"
echo "  insert a watch order:";  as rgarcia "$PW_RGARCIA" \
  "INSERT INTO pd.watch_orders (resident_id, start_date, end_date, notes)
   VALUES (1, '2026-12-20', '2027-01-03', 'Away for holidays')
   RETURNING order_id, entered_by;"
echo "  update the order:";  as rgarcia "$PW_RGARCIA" \
  "UPDATE pd.watch_orders SET notes = 'Away; neighbor has key' WHERE order_id = 1;"
echo "  delete the order:";  as rgarcia "$PW_RGARCIA" "DELETE FROM pd.watch_orders;"
echo "  drop a table:";      as rgarcia "$PW_RGARCIA" "DROP TABLE pd.residents;"
echo "  create a user:";     as rgarcia "$PW_RGARCIA" "CREATE USER intruder;"

echo "== jlee (records_viewer)"
echo "  read the directory view:"; as jlee "$PW_JLEE" "SELECT * FROM pd.resident_directory;"
echo "  read the base table (phone, birth date):"; as jlee "$PW_JLEE" \
  "SELECT phone, date_of_birth FROM pd.residents;"
echo "  insert a watch order:";  as jlee "$PW_JLEE" \
  "INSERT INTO pd.watch_orders (resident_id, start_date, end_date) VALUES (1, '2027-01-01', '2027-01-02');"

echo "== mkhan (records_supervisor, inherits record_entry)"
echo "  read watch orders:";  as mkhan "$PW_MKHAN" "SELECT order_id, notes, entered_by FROM pd.watch_orders;"
echo "  delete a watch order:"; as mkhan "$PW_MKHAN" "DELETE FROM pd.watch_orders WHERE order_id = 1;"
echo "  delete a resident:";  as mkhan "$PW_MKHAN" "DELETE FROM pd.residents;"
