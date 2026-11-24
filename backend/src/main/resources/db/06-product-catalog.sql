BEGIN;

INSERT INTO permissions (id, code)
VALUES (gen_random_uuid(), 'catalog.read'), (gen_random_uuid(), 'catalog.manage')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.tenant_id IS NOT NULL AND r.code = 'TENANT_ADMIN'
  AND p.code IN ('catalog.read', 'catalog.manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

CREATE TABLE categories (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants (id),
    name varchar(255) NOT NULL,
    UNIQUE (tenant_id, id)
);

CREATE TABLE products (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants (id),
    name varchar(255) NOT NULL,
    description text,
    price_idr bigint CHECK (price_idr >= 0),
    product_code text CHECK (product_code ~ '^[A-Z0-9]+$'),
    primary_category_id uuid NOT NULL,
    status varchar(9) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CHECK (status <> 'PUBLISHED' OR price_idr IS NOT NULL),
    UNIQUE (tenant_id, id)
);

CREATE TABLE product_categories (
    tenant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    category_id uuid NOT NULL,
    PRIMARY KEY (product_id, category_id),
    FOREIGN KEY (tenant_id, product_id) REFERENCES products (tenant_id, id),
    FOREIGN KEY (tenant_id, category_id) REFERENCES categories (tenant_id, id)
);

ALTER TABLE products ADD CONSTRAINT products_primary_category_fk
    FOREIGN KEY (id, primary_category_id) REFERENCES product_categories (product_id, category_id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE product_options (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES products (id),
    name varchar(255) NOT NULL,
    position integer NOT NULL CHECK (position >= 0),
    UNIQUE (product_id, id),
    UNIQUE (product_id, position)
);

CREATE TABLE product_option_values (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL,
    option_id uuid NOT NULL,
    label varchar(255) NOT NULL,
    code text CHECK (code ~ '^[A-Z0-9]+$'),
    FOREIGN KEY (product_id, option_id) REFERENCES product_options (product_id, id),
    UNIQUE (product_id, id),
    UNIQUE (product_id, option_id, id)
);

CREATE INDEX product_option_values_order ON product_option_values (product_id, option_id, id);

CREATE TABLE product_variants (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    sku varchar(255) NOT NULL CHECK (sku ~ '^[A-Z0-9._-]+$'),
    sku_mode varchar(6) NOT NULL CHECK (sku_mode IN ('AUTO', 'MANUAL')),
    combination_key uuid[] NOT NULL,
    first_stock_recorded_at timestamptz,
    FOREIGN KEY (tenant_id, product_id) REFERENCES products (tenant_id, id),
    UNIQUE (tenant_id, id),
    UNIQUE (product_id, id),
    CONSTRAINT product_variants_tenant_sku UNIQUE (tenant_id, sku),
    CONSTRAINT product_variants_combination UNIQUE (product_id, combination_key)
);

CREATE TABLE product_variant_option_values (
    product_id uuid NOT NULL,
    variant_id uuid NOT NULL,
    option_id uuid NOT NULL,
    option_value_id uuid NOT NULL,
    PRIMARY KEY (variant_id, option_id),
    FOREIGN KEY (product_id, variant_id) REFERENCES product_variants (product_id, id),
    FOREIGN KEY (product_id, option_id, option_value_id) REFERENCES product_option_values (product_id, option_id, id)
);

CREATE INDEX categories_tenant_list ON categories (tenant_id, name, id);
CREATE INDEX products_tenant_list ON products (tenant_id, name, id);

COMMIT;
