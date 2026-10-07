-- V11: remove custo medio / valor de estoque.
-- Fora do escopo da v1 (controle domestico de estoque, nao precificacao/contabilidade).

ALTER TABLE products DROP COLUMN average_cost;
ALTER TABLE stock_movements DROP COLUMN unit_cost;
