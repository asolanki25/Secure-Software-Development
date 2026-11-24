-- Summary of what each role may do (run as the DBA).
\connect smithtown_pd
SELECT grantee AS role_name, table_name AS object,
       string_agg(privilege_type, ', ' ORDER BY privilege_type) AS privileges
FROM information_schema.role_table_grants
WHERE grantee IN ('record_entry', 'records_viewer', 'records_supervisor')
GROUP BY grantee, table_name ORDER BY grantee, table_name;
SELECT r.rolname AS role_name, g.rolname AS member_of
FROM pg_auth_members m
JOIN pg_roles r ON r.oid = m.member JOIN pg_roles g ON g.oid = m.roleid
WHERE g.rolname IN ('record_entry', 'records_viewer', 'records_supervisor')
ORDER BY 2, 1;
