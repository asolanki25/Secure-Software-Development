# Assignment 15: Secure Role Setup in a Database (PostgreSQL 16)

Based on the Smithtown Police Department example in Richardson & Thies, Chapter 15. Three people get three job roles. Privileges are granted only to roles, and roles are granted to users.

| File | Step |
|---|---|
| `01_create_database.sql` | 1. Create the database and remove PUBLIC's default CONNECT and TEMP rights |
| `00_connect_commands.txt` | 2. Ways to connect (DBA socket, `\connect`, user over TCP, TLS `verify-full`) |
| `02_roles_and_users.sql` | 2 to 6. Connect, create tables and a view, users, roles, grants, and role assignment |
| `03_verify_access.sh` | Logs in as each user and tries allowed and forbidden actions |
| `04_revoke_and_disable.sql` | Revokes a role and disables a login |
| `05_verify_removal.sh` | Confirms the removed access no longer works |
| `06_show_grants.sql` | Summary of what each role holds |
| `out_*.txt` | Real output from the run |

## Run it on Linux

```bash
sudo apt install postgresql            # if needed
export PW_RGARCIA=$(openssl rand -hex 12) PW_JLEE=$(openssl rand -hex 12) PW_MKHAN=$(openssl rand -hex 12)
sudo -u postgres psql -f 01_create_database.sql
sudo -u postgres psql -v pw_rgarcia="$PW_RGARCIA" -v pw_jlee="$PW_JLEE" \
     -v pw_mkhan="$PW_MKHAN" -f 02_roles_and_users.sql
bash 03_verify_access.sh
sudo -u postgres psql -f 04_revoke_and_disable.sql
bash 05_verify_removal.sh
sudo -u postgres psql -f 06_show_grants.sql
```

If `sudo -u postgres` can't read a file in your home folder, copy the `.sql` files to `/tmp` first. Passwords come from environment variables and never appear in the scripts.
