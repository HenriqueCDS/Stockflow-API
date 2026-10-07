-- V8: autoria do movimento de estoque ("cada movimento mostra quem fez").
-- Nullable porque movimentos antigos (antes desta coluna existir) nao tem autor conhecido.

ALTER TABLE stock_movements ADD COLUMN created_by UUID REFERENCES users(id);

CREATE INDEX idx_stock_movements_created_by ON stock_movements(created_by) WHERE created_by IS NOT NULL;
