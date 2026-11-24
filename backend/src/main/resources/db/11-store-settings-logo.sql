BEGIN;

ALTER TABLE store_settings
    ADD COLUMN logo_id uuid UNIQUE,
    ADD COLUMN logo_content_type text,
    ADD CONSTRAINT store_settings_logo_check CHECK (
        (logo_id IS NULL AND logo_content_type IS NULL)
        OR (logo_id IS NOT NULL AND logo_content_type IS NOT NULL
            AND logo_content_type IN ('image/png', 'image/jpeg', 'image/webp'))
    );

COMMIT;
