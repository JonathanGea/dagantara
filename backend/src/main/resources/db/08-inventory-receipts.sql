BEGIN;

INSERT INTO permissions (id, code)
VALUES (gen_random_uuid(), 'inventory.read'),
       (gen_random_uuid(), 'inventory.receive'),
       (gen_random_uuid(), 'inventory.correct')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.tenant_id IS NOT NULL AND r.code = 'TENANT_ADMIN'
  AND p.code IN ('inventory.read', 'inventory.receive', 'inventory.correct')
ON CONFLICT (role_id, permission_id) DO NOTHING;

CREATE TABLE inventory_warehouses (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL UNIQUE REFERENCES tenants (id),
    UNIQUE (tenant_id, id)
);

CREATE TABLE inventory_receipts (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    received_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT statement_timestamp(),
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, received_by, id),
    FOREIGN KEY (tenant_id, warehouse_id) REFERENCES inventory_warehouses (tenant_id, id),
    FOREIGN KEY (tenant_id, received_by) REFERENCES users (tenant_id, id)
);

CREATE INDEX inventory_receipts_history_idx ON inventory_receipts (tenant_id, created_at DESC, id DESC);

CREATE TABLE inventory_receipt_lines (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    variant_id uuid NOT NULL,
    sku varchar(255) NOT NULL,
    quantity bigint NOT NULL CHECK (quantity > 0),
    UNIQUE (receipt_id, variant_id),
    UNIQUE (tenant_id, receipt_id, id),
    FOREIGN KEY (tenant_id, receipt_id) REFERENCES inventory_receipts (tenant_id, id),
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants (tenant_id, id)
);

CREATE TABLE inventory_stock_balances (
    tenant_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    variant_id uuid NOT NULL,
    quantity bigint NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (warehouse_id, variant_id),
    FOREIGN KEY (tenant_id, warehouse_id) REFERENCES inventory_warehouses (tenant_id, id),
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants (tenant_id, id)
);

CREATE TABLE inventory_receipt_keys (
    tenant_id uuid NOT NULL,
    account_id uuid NOT NULL,
    idempotency_key varchar(255) NOT NULL,
    request_hash bytea NOT NULL CHECK (octet_length(request_hash) = 32),
    receipt_id uuid NOT NULL,
    PRIMARY KEY (tenant_id, account_id, idempotency_key),
    FOREIGN KEY (tenant_id, account_id, receipt_id) REFERENCES inventory_receipts (tenant_id, received_by, id)
        DEFERRABLE INITIALLY DEFERRED
);

COMMIT;
