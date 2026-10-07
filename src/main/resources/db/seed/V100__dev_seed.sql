-- V100: Dev-only seed (loaded via application-dev.yml). Never runs in prod.
-- Tenant demo (criado no V3). Senha dos usuarios extras: Demo@1234

DO $$
DECLARE
    t        UUID := '00000000-0000-0000-0000-000000000001';
    inv_a    UUID := gen_random_uuid();
    inv_b    UUID := gen_random_uuid();
    inv_id   UUID;
    pid      UUID;
    p        RECORD;
    stock    NUMERIC(15,4);
    day_off  INT := 0;
BEGIN
    IF EXISTS (SELECT 1 FROM products WHERE tenant_id = t) THEN
        RETURN;
    END IF;

    -- O hash do admin no V3 nao confere com Demo@1234; redefine so em dev
    UPDATE users SET password_hash = crypt('Demo@1234', gen_salt('bf', 12)) WHERE email = 'demo@stockflow.com';

    -- Usuarios extras (BCrypt via pgcrypto, compativel com BCryptPasswordEncoder)
    INSERT INTO users (name, email, password_hash, role, tenant_id) VALUES
        ('Maria Operadora', 'user@stockflow.com',   crypt('Demo@1234', gen_salt('bf', 12)), 'MEMBER', t),
        ('Joao Consulta',   'viewer@stockflow.com', crypt('Demo@1234', gen_salt('bf', 12)), 'MEMBER', t)
    ON CONFLICT (email) DO NOTHING;

    -- Notas fiscais confirmadas (total_value e recalculado ao final)
    INSERT INTO invoices (id, tenant_id, invoice_key, supplier_name, supplier_cnpj, purchase_date, total_value, qr_code_url, status, created_at) VALUES
        (inv_a, t, '35260911222333000181650010000012341000012345', 'Atacadao Bom Preco LTDA', '11222333000181', CURRENT_DATE - 20, 0, 'https://www.nfce.fazenda.sp.gov.br/qrcode?p=demoA', 'CONFIRMED', NOW() - INTERVAL '20 days'),
        (inv_b, t, '35260944555666000172650020000056781000056789', 'Distribuidora Sul S.A.',  '44555666000172', CURRENT_DATE - 10, 0, 'https://www.nfce.fazenda.sp.gov.br/qrcode?p=demoB', 'CONFIRMED', NOW() - INTERVAL '10 days');

    -- Notas em outros estados do fluxo
    INSERT INTO invoices (tenant_id, invoice_key, supplier_name, supplier_cnpj, purchase_date, total_value, qr_code_url, status) VALUES
        (t, NULL, NULL, NULL, NULL, NULL, 'https://www.nfce.fazenda.sp.gov.br/qrcode?p=demoPending', 'PENDING'),
        (t, '35260977888999000163650030000099991000099999', 'Mercado Central ME', '77888999000163', CURRENT_DATE - 1, 89.70, 'https://www.nfce.fazenda.sp.gov.br/qrcode?p=demoFetched', 'FETCHED');

    -- Produtos: nome, ean, categoria, unidade, qtd comprada, custo unit., qtd saida, estoque minimo, nota
    FOR p IN
        SELECT * FROM (VALUES
            ('Arroz Tipo 1 5kg',        '7891000100103', 'Mercearia',  'UN',  120, 24.90,  85, 30, 'A'),
            ('Feijao Carioca 1kg',      '7891000100202', 'Mercearia',  'UN',  150,  7.80, 110, 40, 'A'),
            ('Acucar Refinado 1kg',     '7891000100301', 'Mercearia',  'UN',  100,  4.25,  60, 25, 'A'),
            ('Oleo de Soja 900ml',      '7891000100400', 'Mercearia',  'UN',   90,  6.90,  82, 20, 'A'),
            ('Cafe Torrado 500g',       '7891000100509', 'Mercearia',  'UN',   60, 18.50,  38, 15, 'A'),
            ('Leite Integral 1L',       '7891000100608', 'Laticinios', 'UN',  240,  4.60, 215, 60, 'A'),
            ('Macarrao Espaguete 500g', '7891000100707', 'Mercearia',  'UN',  130,  3.90,  70, 30, 'A'),
            ('Farinha de Trigo 1kg',    '7891000100806', 'Mercearia',  'UN',   80,  5.10,  44, 20, 'A'),
            ('Detergente 500ml',        '7891000200105', 'Limpeza',    'UN',  200,  2.35, 120, 50, 'B'),
            ('Sabao em Po 1kg',         '7891000200204', 'Limpeza',    'UN',   70, 11.90,  41, 20, 'B'),
            ('Papel Higienico 12un',    '7891000200303', 'Higiene',    'PCT',  50, 19.80,  47, 15, 'B'),
            ('Sabonete 90g',            '7891000200402', 'Higiene',    'UN',  180,  1.89,  95, 40, 'B'),
            ('Refrigerante Cola 2L',    '7891000300101', 'Bebidas',    'UN',  160,  7.40, 140, 40, 'B'),
            ('Agua Mineral 1,5L',       '7891000300200', 'Bebidas',    'UN',  300,  1.95, 180, 80, 'B'),
            ('Banana Prata',            NULL,            'Hortifruti', 'KG',   45,  5.20,  30, 10, 'B')
        ) AS v(name, ean, category, unit, bought, cost, sold, min_stock, inv)
    LOOP
        INSERT INTO products (tenant_id, name, ean, category, unit, current_stock, minimum_stock, created_at)
        VALUES (t, p.name, p.ean, p.category, p.unit, 0, p.min_stock, NOW() - INTERVAL '30 days')
        RETURNING id INTO pid;

        inv_id := CASE p.inv WHEN 'A' THEN inv_a ELSE inv_b END;
        day_off := day_off + 1;

        INSERT INTO invoice_items (invoice_id, product_id, product_name, product_ean, quantity, unit_value, total_value, unit)
        VALUES (inv_id, pid, p.name, p.ean, p.bought, p.cost, ROUND(p.bought * p.cost, 2), p.unit);

        -- Entrada via nota fiscal
        INSERT INTO stock_movements (tenant_id, product_id, type, quantity, stock_before, stock_after, reference, notes, created_at)
        VALUES (t, pid, 'ENTRY', p.bought, 0, p.bought,
                'NFC-e ' || CASE p.inv WHEN 'A' THEN '12341' ELSE '56781' END, 'Entrada por nota fiscal',
                NOW() - CASE p.inv WHEN 'A' THEN INTERVAL '20 days' ELSE INTERVAL '10 days' END);
        stock := p.bought;

        -- Saida (consumo)
        INSERT INTO stock_movements (tenant_id, product_id, type, quantity, stock_before, stock_after, reference, notes, created_at)
        VALUES (t, pid, 'USED', p.sold, stock, stock - p.sold, NULL, 'Consumo acumulado',
                NOW() - INTERVAL '3 days' - (day_off || ' hours')::INTERVAL);
        stock := stock - p.sold;

        UPDATE products SET current_stock = stock WHERE id = pid;
    END LOOP;

    -- Um ajuste e um descarte para variar os tipos de movimento
    SELECT id, current_stock INTO pid, stock FROM products WHERE tenant_id = t AND name = 'Sabonete 90g';
    INSERT INTO stock_movements (tenant_id, product_id, type, quantity, stock_before, stock_after, reference, notes)
    VALUES (t, pid, 'ADJUSTMENT', -2, stock, stock - 2, 'Inventario', 'Correcao de contagem');
    UPDATE products SET current_stock = stock - 2 WHERE id = pid;

    SELECT id, current_stock INTO pid, stock FROM products WHERE tenant_id = t AND name = 'Leite Integral 1L';
    INSERT INTO stock_movements (tenant_id, product_id, type, quantity, stock_before, stock_after, reference, notes)
    VALUES (t, pid, 'DISCARDED', 1, stock, stock - 1, NULL, 'Venceu');
    UPDATE products SET current_stock = stock - 1 WHERE id = pid;

    -- Totais das notas = soma dos itens
    UPDATE invoices i SET total_value = (SELECT COALESCE(SUM(total_value), 0) FROM invoice_items WHERE invoice_id = i.id)
    WHERE i.id IN (inv_a, inv_b);
END $$;
