-- Apply to a disposable database after migrations 01–11 and before migration 12.
\getenv fixture_password DAGANTARA_TM_TEST_PASSWORD

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO tenants (id, name, owner_user_id)
VALUES ('5a0b121c-013b-4e8d-bd14-a42e8f721001', 'TM Legacy Store',
        '5a0b121c-013b-4e8d-bd14-a42e8f721002');

INSERT INTO tenant_domains (hostname, shop_path, tenant_id)
VALUES ('localhost', '/tm-legacy', '5a0b121c-013b-4e8d-bd14-a42e8f721001');

INSERT INTO roles (id, tenant_id, code, name)
VALUES ('5a0b121c-013b-4e8d-bd14-a42e8f721003', '5a0b121c-013b-4e8d-bd14-a42e8f721001',
        'TENANT_ADMIN', 'Admin Tenant');

INSERT INTO users (id, tenant_id, role_id, username, password_hash)
VALUES ('5a0b121c-013b-4e8d-bd14-a42e8f721002', '5a0b121c-013b-4e8d-bd14-a42e8f721001',
        '5a0b121c-013b-4e8d-bd14-a42e8f721003', 'tm-legacy-owner', crypt(:'fixture_password', gen_salt('bf', 12)));

INSERT INTO users (id, role_id, username, password_hash, is_initial_platform_admin)
SELECT gen_random_uuid(), id, 'tm-admin', crypt(:'fixture_password', gen_salt('bf', 12)), true
FROM roles WHERE tenant_id IS NULL AND code = 'SUPERADMIN';

COMMIT;
