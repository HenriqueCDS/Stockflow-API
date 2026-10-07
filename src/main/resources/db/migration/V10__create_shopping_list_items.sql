-- V10: lista de compras compartilhada (itens abaixo do minimo + itens manuais).

CREATE TABLE shopping_list_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    product_id  UUID REFERENCES products(id),
    name        VARCHAR(255) NOT NULL,
    quantity    NUMERIC(15,4),
    checked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_shopping_list_items_tenant_open ON shopping_list_items(tenant_id, checked) WHERE deleted_at IS NULL;
CREATE INDEX idx_shopping_list_items_product ON shopping_list_items(product_id) WHERE product_id IS NOT NULL;
