BEGIN;

CREATE TABLE tenants (
    id uuid PRIMARY KEY,
    code varchar NOT NULL UNIQUE,
    name varchar NOT NULL,
    status varchar NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE tenant_domains (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants(id),
    hostname varchar NOT NULL UNIQUE,
    domain_kind varchar NOT NULL,
    status varchar NOT NULL
);

CREATE TABLE roles (
    id uuid PRIMARY KEY,
    tenant_id uuid REFERENCES tenants(id),
    code varchar NOT NULL,
    name varchar NOT NULL,
    scope varchar NOT NULL,
    is_system boolean NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE TABLE permissions (
    id uuid PRIMARY KEY,
    code varchar NOT NULL UNIQUE,
    module varchar NOT NULL,
    description text
);

CREATE TABLE role_permissions (
    role_id uuid NOT NULL REFERENCES roles(id),
    permission_id uuid NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id uuid PRIMARY KEY,
    tenant_id uuid REFERENCES tenants(id),
    role_id uuid NOT NULL REFERENCES roles(id),
    username varchar(128) NOT NULL,
    email varchar(254) NOT NULL,
    full_name varchar(255) NOT NULL,
    password_hash varchar NOT NULL,
    account_type varchar NOT NULL,
    status varchar NOT NULL,
    created_by uuid REFERENCES users(id),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT users_login_identity UNIQUE NULLS NOT DISTINCT (tenant_id, username),
    CONSTRAINT users_account_type_matches_tenant CHECK (
        (tenant_id IS NULL AND account_type = 'PLATFORM')
        OR (tenant_id IS NOT NULL AND account_type = 'TENANT')
    )
);

COMMIT;
