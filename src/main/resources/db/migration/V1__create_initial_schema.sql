-- V1: Initial schema for Stockflow SaaS

-- Extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- COMPANIES
-- =============================================
CREATE TABLE companies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    cnpj        VARCHAR(14) NOT NULL UNIQUE,
    tenant_id   UUID NOT NULL UNIQUE,
    email       VARCHAR(100),
    phone       VARCHAR(20),
    address     VARCHAR(255),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

-- =============================================
-- USERS
-- =============================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30) NOT NULL DEFAULT 'USER',
    tenant_id     UUID NOT NULL,
    refresh_token TEXT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    deleted_at    TIMESTAMP,
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES companies(tenant_id)
);

-- =============================================
-- PRODUCTS
-- =============================================
CREATE TABLE products (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    name          VARCHAR(255) NOT NULL,
    ean           VARCHAR(14),
    category      VARCHAR(100),
    unit          VARCHAR(20),
    current_stock NUMERIC(15,4) NOT NULL DEFAULT 0,
    average_cost  NUMERIC(15,4) NOT NULL DEFAULT 0,
    minimum_stock NUMERIC(15,4) NOT NULL DEFAULT 0,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    deleted_at    TIMESTAMP
);

-- =============================================
-- INVOICES
-- =============================================
CREATE TABLE invoices (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    invoice_key   VARCHAR(44),
    supplier_name VARCHAR(255),
    supplier_cnpj VARCHAR(14),
    purchase_date DATE,
    total_value   NUMERIC(15,2),
    qr_code_url   VARCHAR(2048),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    deleted_at    TIMESTAMP
);

-- =============================================
-- INVOICE ITEMS
-- =============================================
CREATE TABLE invoice_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id   UUID NOT NULL,
    product_id   UUID,
    product_name VARCHAR(255) NOT NULL,
    product_ean  VARCHAR(14),
    quantity     NUMERIC(15,4) NOT NULL,
    unit_value   NUMERIC(15,4) NOT NULL,
    total_value  NUMERIC(15,2) NOT NULL,
    unit         VARCHAR(20),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP,
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =============================================
-- STOCK MOVEMENTS
-- =============================================
CREATE TABLE stock_movements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    product_id   UUID NOT NULL,
    type         VARCHAR(20) NOT NULL,
    quantity     NUMERIC(15,4) NOT NULL,
    unit_cost    NUMERIC(15,4),
    stock_before NUMERIC(15,4),
    stock_after  NUMERIC(15,4),
    reference    VARCHAR(255),
    notes        VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP,
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products(id)
);
