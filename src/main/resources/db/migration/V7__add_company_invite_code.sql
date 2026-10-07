-- V7: codigo de convite da casa. O dono compartilha esse codigo para que
-- outras pessoas entrem na casa (POST /api/v1/auth/join), sem CNPJ/aprovacao manual.

ALTER TABLE companies ADD COLUMN invite_code VARCHAR(10);

-- Backfill para casas ja existentes (ex.: seed de demo), que nao tinham esse conceito.
UPDATE companies SET invite_code = upper(substr(md5(random()::text || id::text), 1, 8))
WHERE invite_code IS NULL;

ALTER TABLE companies ALTER COLUMN invite_code SET NOT NULL;
CREATE UNIQUE INDEX idx_companies_invite_code ON companies(invite_code);
