\set ON_ERROR_STOP on
\set superadmin_username ''
\set superadmin_email ''
\set superadmin_full_name ''
\set superadmin_password_hash ''
\getenv superadmin_username DAGANTARA_SUPERADMIN_USERNAME
\getenv superadmin_email DAGANTARA_SUPERADMIN_EMAIL
\getenv superadmin_full_name DAGANTARA_SUPERADMIN_FULL_NAME
\getenv superadmin_password_hash DAGANTARA_SUPERADMIN_PASSWORD_HASH

SELECT char_length(btrim(:'superadmin_username')) BETWEEN 1 AND 128
   AND char_length(:'superadmin_username') <= 128
   AND char_length(:'superadmin_email') <= 254
   AND :'superadmin_email' ~ '^[^[:space:]@]+@[^[:space:]@]+$'
   AND char_length(btrim(:'superadmin_full_name')) BETWEEN 1 AND 255
   AND char_length(:'superadmin_full_name') <= 255
   AND :'superadmin_password_hash' ~ '^[$]2[aby][$]12[$][./A-Za-z0-9]{53}$' AS valid_inputs
\gset

\if :valid_inputs
BEGIN;

SELECT id AS superadmin_role_id FROM roles WHERE code = 'SUPERADMIN' FOR UPDATE
\gset

INSERT INTO users (
    id, role_id, username, email, full_name, password_hash, account_type, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), :'superadmin_role_id', :'superadmin_username', :'superadmin_email',
       :'superadmin_full_name', :'superadmin_password_hash', 'PLATFORM', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role_id = :'superadmin_role_id');

COMMIT;
\else
DO $$
BEGIN
    RAISE EXCEPTION 'Set the four DAGANTARA_SUPERADMIN environment variables with valid account data and a BCrypt cost-12 hash';
END;
$$;
\endif
