-- =====================================================================
-- CampusMarket - Migración V2 (ajuste de S1-T13 pedido por E08 / S1-T11)
-- Tabla sesiones: el id pasa a ser el HASH SHA-256 (hex, 64 caracteres) del
-- token que viaja en la cookie HttpOnly.
--
-- Por qué: si alguien accede a la base de datos, solo verá hashes y no podrá
-- reutilizarlos como cookie. El token en claro vive únicamente en la cookie.
--
-- Cambios respecto a V1:
--   1. id: UUID -> VARCHAR(64) (hash SHA-256 en hexadecimal minúsculas).
--   2. Se elimina ck_sesiones_rev_coherente: el backend puede revocar con
--      solo `revocada = true`; fecha_revocacion pasa a ser informativa/opcional.
--   3. Se agrega ck_sesiones_id_hash para que el id sea siempre un SHA-256 válido.
--
-- Las sesiones existentes se eliminan: sus ids (UUID) ya no coinciden con
-- ningún hash, así que serían inutilizables. Los usuarios solo vuelven a
-- iniciar sesión. Esto no afecta a usuarios ni tokens.
-- =====================================================================

DELETE FROM sesiones;

ALTER TABLE sesiones DROP CONSTRAINT ck_sesiones_rev_coherente;

ALTER TABLE sesiones ALTER COLUMN id DROP DEFAULT;
ALTER TABLE sesiones ALTER COLUMN id TYPE VARCHAR(64) USING id::text;

ALTER TABLE sesiones
    ADD CONSTRAINT ck_sesiones_id_hash CHECK (id ~ '^[0-9a-f]{64}$');

COMMENT ON TABLE  sesiones    IS 'Sesiones de servidor. La cookie HttpOnly lleva el token; aquí solo se guarda su hash SHA-256.';
COMMENT ON COLUMN sesiones.id IS 'SHA-256 (hex minúsculas, 64 chars) del token de la cookie.';
COMMENT ON COLUMN sesiones.fecha_revocacion IS 'Opcional/informativa. La validez la determina "revocada".';
