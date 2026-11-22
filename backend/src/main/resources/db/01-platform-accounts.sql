BEGIN;

CREATE TABLE roles (
    id uuid PRIMARY KEY,
    tenant_id uuid,
    code varchar(128) NOT NULL,
    name varchar(255) NOT NULL,
    CONSTRAINT roles_scope_code_unique UNIQUE NULLS NOT DISTINCT (tenant_id, code)
);

CREATE TABLE users (
    id uuid PRIMARY KEY,
    tenant_id uuid,
    role_id uuid NOT NULL REFERENCES roles (id),
    username varchar(128) NOT NULL,
    password_hash varchar NOT NULL,
    is_initial_platform_admin boolean NOT NULL DEFAULT false,
    CONSTRAINT users_scope_username_unique UNIQUE NULLS NOT DISTINCT (tenant_id, username),
    CONSTRAINT users_initial_admin_is_platform CHECK (
        NOT is_initial_platform_admin OR tenant_id IS NULL
    )
);

CREATE UNIQUE INDEX users_one_initial_platform_admin
    ON users (is_initial_platform_admin) WHERE is_initial_platform_admin;

INSERT INTO roles (id, code, name)
VALUES (gen_random_uuid(), 'SUPERADMIN', 'Admin Platform');

COMMIT;
