BEGIN;

CREATE TABLE tenant_admin_recovery_audit (
    id uuid PRIMARY KEY,
    operator_id uuid NOT NULL REFERENCES users (id),
    tenant_id uuid NOT NULL,
    target_user_id uuid NOT NULL,
    verification_method varchar(32) NOT NULL CHECK (verification_method IN (
        'MATCHED_INCOMING_NUMBER', 'CALLBACK_TO_REGISTERED_NUMBER', 'KNOWN_REQUESTER'
    )),
    reset_at timestamptz NOT NULL DEFAULT statement_timestamp(),
    FOREIGN KEY (tenant_id, target_user_id) REFERENCES users (tenant_id, id),
    CHECK (operator_id <> target_user_id)
);

COMMIT;
