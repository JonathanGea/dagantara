BEGIN;

CREATE TABLE product_media (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    FOREIGN KEY (tenant_id, product_id) REFERENCES products (tenant_id, id),
    UNIQUE (product_id, id)
);

CREATE INDEX product_media_order ON product_media (tenant_id, product_id, created_at, id);

CREATE TABLE media_option_values (
    product_id UUID NOT NULL,
    media_id UUID NOT NULL,
    option_value_id UUID NOT NULL,
    PRIMARY KEY (media_id, option_value_id),
    FOREIGN KEY (product_id, media_id) REFERENCES product_media (product_id, id),
    FOREIGN KEY (product_id, option_value_id) REFERENCES product_option_values (product_id, id)
);

CREATE INDEX media_option_values_by_value ON media_option_values (product_id, option_value_id);

COMMIT;
