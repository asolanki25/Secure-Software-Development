-- Remove access. As the DBA: sudo -u postgres psql -f 04_revoke_and_disable.sql
\connect smithtown_pd
REVOKE record_entry FROM rgarcia;      -- rgarcia changes jobs: role removed
ALTER ROLE jlee NOLOGIN;               -- jlee leaves: account disabled at once
-- Who holds which role now?
SELECT r.rolname AS user_name, r.rolcanlogin AS can_login,
       COALESCE(string_agg(g.rolname, ', '), '(none)') AS member_of
FROM pg_roles r
LEFT JOIN pg_auth_members m ON m.member = r.oid
LEFT JOIN pg_roles g ON g.oid = m.roleid
WHERE r.rolname IN ('rgarcia', 'jlee', 'mkhan')
GROUP BY r.rolname, r.rolcanlogin ORDER BY r.rolname;
