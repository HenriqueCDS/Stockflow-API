-- V5: pivot de "empresa com CNPJ" para "casa" (sem CNPJ).
-- CNPJ deixa de ser exigido/armazenado para o tenant (companies). O CNPJ do
-- FORNECEDOR na nota fiscal (invoices.supplier_cnpj) nao e afetado: continua
-- sendo o mercado onde a compra foi feita, nao a identidade da casa.
-- Destrutivo por decisao explicita: ambiente hoje so tem o CNPJ demo da seed.

DROP INDEX IF EXISTS idx_companies_cnpj;

ALTER TABLE companies DROP COLUMN cnpj;
