-- V6: papeis do usuario passam a ser so OWNER/MEMBER (dono/membro da casa).
-- ADMIN era quem criava o tenant -> OWNER. USER e VIEWER nao tem mais
-- distincao nessa proposta -> MEMBER.

UPDATE users SET role = 'OWNER' WHERE role = 'ADMIN';
UPDATE users SET role = 'MEMBER' WHERE role IN ('USER', 'VIEWER');

ALTER TABLE users ALTER COLUMN role SET DEFAULT 'MEMBER';
