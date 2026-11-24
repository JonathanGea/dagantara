BEGIN;

ALTER TABLE inventory_receipts ADD UNIQUE (tenant_id, id, warehouse_id);

CREATE TABLE inventory_corrections (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    corrected_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT statement_timestamp(),
    correction_order bigint GENERATED ALWAYS AS IDENTITY,
    reason text NOT NULL,
    UNIQUE (tenant_id, receipt_id, id),
    UNIQUE (tenant_id, corrected_by, id),
    FOREIGN KEY (tenant_id, receipt_id, warehouse_id) REFERENCES inventory_receipts (tenant_id, id, warehouse_id),
    FOREIGN KEY (tenant_id, corrected_by) REFERENCES users (tenant_id, id)
);

CREATE INDEX inventory_corrections_history_idx ON inventory_corrections (tenant_id, receipt_id, correction_order);

CREATE TABLE inventory_correction_lines (
    tenant_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    correction_id uuid NOT NULL,
    receipt_line_id uuid NOT NULL,
    previous_quantity bigint NOT NULL CHECK (previous_quantity >= 0),
    quantity bigint NOT NULL CHECK (quantity >= 0),
    delta bigint GENERATED ALWAYS AS (quantity - previous_quantity) STORED,
    PRIMARY KEY (correction_id, receipt_line_id),
    FOREIGN KEY (tenant_id, receipt_id, correction_id) REFERENCES inventory_corrections (tenant_id, receipt_id, id),
    FOREIGN KEY (tenant_id, receipt_id, receipt_line_id) REFERENCES inventory_receipt_lines (tenant_id, receipt_id, id)
);

CREATE INDEX inventory_correction_lines_receipt_idx
    ON inventory_correction_lines (tenant_id, receipt_id, receipt_line_id);

CREATE TABLE inventory_correction_keys (
    tenant_id uuid NOT NULL,
    account_id uuid NOT NULL,
    idempotency_key varchar(255) NOT NULL,
    request_hash bytea NOT NULL CHECK (octet_length(request_hash) = 32),
    correction_id uuid NOT NULL,
    PRIMARY KEY (tenant_id, account_id, idempotency_key),
    FOREIGN KEY (tenant_id, account_id, correction_id) REFERENCES inventory_corrections (tenant_id, corrected_by, id)
        DEFERRABLE INITIALLY DEFERRED
);

COMMIT;
