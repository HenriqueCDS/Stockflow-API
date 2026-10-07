-- V9: suporte a revisao de itens da nota antes de confirmar
-- (renomear, religar a outro produto, ajustar quantidade, ignorar).
-- "ignored" marca o item como excluido da confirmacao: nao gera movimento de estoque.

ALTER TABLE invoice_items ADD COLUMN ignored BOOLEAN NOT NULL DEFAULT FALSE;
