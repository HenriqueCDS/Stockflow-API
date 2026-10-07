-- V4: vincula cada produto ao usuario (conta) que o cadastrou.
-- Produtos anteriores ficam com created_by NULL (cadastrados antes do vinculo).

ALTER TABLE products ADD COLUMN created_by UUID;

ALTER TABLE products
    ADD CONSTRAINT fk_products_created_by FOREIGN KEY (created_by) REFERENCES users(id);

CREATE INDEX idx_products_tenant_created_by ON products(tenant_id, created_by) WHERE deleted_at IS NULL;
