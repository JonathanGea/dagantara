BEGIN;

CREATE TABLE tenants (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    owner_user_id uuid NOT NULL
);

ALTER TABLE roles ADD CONSTRAINT roles_tenant_fk
    FOREIGN KEY (tenant_id) REFERENCES tenants (id);

ALTER TABLE users
    ADD CONSTRAINT users_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    ADD CONSTRAINT users_tenant_id_key UNIQUE (tenant_id, id),
    ADD COLUMN phone_number varchar(32),
    ADD COLUMN password_change_required boolean NOT NULL DEFAULT false,
    ADD COLUMN session_version integer NOT NULL DEFAULT 0;

ALTER TABLE tenants ADD CONSTRAINT tenants_owner_fk
    FOREIGN KEY (id, owner_user_id) REFERENCES users (tenant_id, id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE tenant_domains (
    hostname varchar(253) NOT NULL,
    shop_path varchar(64) NOT NULL,
    tenant_id uuid NOT NULL REFERENCES tenants (id),
    PRIMARY KEY (hostname, shop_path)
);

CREATE TABLE tenant_applications (
    id uuid PRIMARY KEY,
    store_name varchar(255) NOT NULL,
    phone_number varchar(32) NOT NULL,
    proposed_domain varchar(253) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'IN_REVIEW',
    tenant_id uuid UNIQUE REFERENCES tenants (id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tenant_applications_state CHECK (
        (status IN ('IN_REVIEW', 'CLOSED') AND tenant_id IS NULL)
        OR (status = 'PROVISIONED' AND tenant_id IS NOT NULL)
    )
);

CREATE TABLE permissions (
    id uuid PRIMARY KEY,
    code varchar(128) NOT NULL UNIQUE
);

CREATE TABLE role_permissions (
    role_id uuid NOT NULL REFERENCES roles (id),
    permission_id uuid NOT NULL REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO permissions (id, code) VALUES (gen_random_uuid(), 'tenants.provision');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.tenant_id IS NULL AND r.code = 'SUPERADMIN' AND p.code = 'tenants.provision';

COMMIT;
