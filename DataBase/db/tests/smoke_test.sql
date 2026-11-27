-- =====================================================================
-- CampusMarket - Smoke test de base de datos (S1-T14)
-- Verifica que (1) las tablas existen, (2) las restricciones protegen los
-- datos por sí solas y (3) el flujo básico registro -> verificación ->
-- sesión -> logout funciona a nivel de datos.
-- Todo corre dentro de una transacción que se REVIERTE: no deja basura.
-- Ejecutar con:  ./scripts/smoke_test.sh
-- =====================================================================
\set ON_ERROR_STOP on
\set QUIET on
BEGIN;
\o /dev/null

-- Helper: ejecuta una sentencia que DEBE fallar con un SQLSTATE concreto.
--   23505 unique_violation | 23503 foreign_key_violation | 23514 check_violation
CREATE FUNCTION pg_temp.debe_fallar(nombre text, sentencia text, esperado text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  BEGIN
    EXECUTE sentencia;
  EXCEPTION WHEN OTHERS THEN
    IF SQLSTATE = esperado THEN
      RAISE NOTICE 'OK    %  (rechazado con %)', nombre, SQLSTATE;
      RETURN;
    END IF;
    RAISE EXCEPTION 'FALLO % : se esperaba % y llegó % (%)', nombre, esperado, SQLSTATE, SQLERRM;
  END;
  RAISE EXCEPTION 'FALLO % : la sentencia NO fue rechazada', nombre;
END $$;

-- Helper: aserción simple.
CREATE FUNCTION pg_temp.afirmar(nombre text, condicion boolean)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF condicion IS TRUE THEN
    RAISE NOTICE 'OK    %', nombre;
  ELSE
    RAISE EXCEPTION 'FALLO %', nombre;
  END IF;
END $$;

-- ---------------------------------------------------------------------
-- 1) Existencia de tablas (Sprint 1 y Sprint 2)
-- ---------------------------------------------------------------------
SELECT pg_temp.afirmar('tabla usuarios existe',            to_regclass('public.usuarios')            IS NOT NULL);
SELECT pg_temp.afirmar('tabla tokens existe',              to_regclass('public.tokens')              IS NOT NULL);
SELECT pg_temp.afirmar('tabla sesiones existe',            to_regclass('public.sesiones')            IS NOT NULL);
SELECT pg_temp.afirmar('tabla tokens_recuperacion existe', to_regclass('public.tokens_recuperacion') IS NOT NULL);
SELECT pg_temp.afirmar('tabla contactos existe',           to_regclass('public.contactos')           IS NOT NULL);
SELECT pg_temp.afirmar('tabla categorias existe',          to_regclass('public.categorias')          IS NOT NULL);
SELECT pg_temp.afirmar('tabla productos existe',           to_regclass('public.productos')           IS NOT NULL);
SELECT pg_temp.afirmar('tabla imagenes existe',            to_regclass('public.imagenes')            IS NOT NULL);

-- Para no chocar con datos sembrados, el test usa correos propios.
-- ---------------------------------------------------------------------
-- 2) Restricciones de USUARIOS
-- ---------------------------------------------------------------------
INSERT INTO usuarios (correo, nombre, password_hash)
VALUES ('smoke.base@gmail.com', 'Smoke Base', '$2a$10$hashdeprueba');

SELECT pg_temp.debe_fallar('correo duplicado exacto (CA-01-02)',
  $q$INSERT INTO usuarios (correo, nombre, password_hash)
     VALUES ('smoke.base@gmail.com', 'Otro', 'x')$q$, '23505');

SELECT pg_temp.debe_fallar('correo con mayúsculas es rechazado',
  $q$INSERT INTO usuarios (correo, nombre, password_hash)
     VALUES ('Smoke.Base@gmail.com', 'Otro', 'x')$q$, '23514');

SELECT pg_temp.debe_fallar('correo con formato inválido',
  $q$INSERT INTO usuarios (correo, nombre, password_hash)
     VALUES ('no-es-correo', 'Otro', 'x')$q$, '23514');

SELECT pg_temp.debe_fallar('estado inválido',
  $q$INSERT INTO usuarios (correo, nombre, password_hash, estado)
     VALUES ('smoke.estado@gmail.com', 'X', 'x', 'BANEADA')$q$, '23514');

SELECT pg_temp.debe_fallar('rol inválido',
  $q$INSERT INTO usuarios (correo, nombre, password_hash, rol)
     VALUES ('smoke.rol@gmail.com', 'X', 'x', 'SUPERUSUARIO')$q$, '23514');

SELECT pg_temp.debe_fallar('nombre vacío',
  $q$INSERT INTO usuarios (correo, nombre, password_hash)
     VALUES ('smoke.nombre@gmail.com', '   ', 'x')$q$, '23514');

SELECT pg_temp.afirmar('usuario nuevo nace PENDIENTE y ESTUDIANTE',
  (SELECT estado = 'PENDIENTE' AND rol = 'ESTUDIANTE'
     FROM usuarios WHERE correo = 'smoke.base@gmail.com'));

-- ---------------------------------------------------------------------
-- 3) Restricciones de TOKENS
-- ---------------------------------------------------------------------
SELECT pg_temp.debe_fallar('token con usuario inexistente (FK)',
  $q$INSERT INTO tokens (usuario_id, token_hash, fecha_vencimiento)
     VALUES (999999999, 'h', now() + interval '30 minutes')$q$, '23503');

SELECT pg_temp.debe_fallar('token que vence antes de crearse',
  $q$INSERT INTO tokens (usuario_id, token_hash, fecha_creacion, fecha_vencimiento)
     SELECT id, 'h', now(), now() - interval '1 minute'
       FROM usuarios WHERE correo = 'smoke.base@gmail.com'$q$, '23514');

SELECT pg_temp.debe_fallar('token usado sin fecha_uso',
  $q$INSERT INTO tokens (usuario_id, token_hash, fecha_vencimiento, usado)
     SELECT id, 'h', now() + interval '30 minutes', TRUE
       FROM usuarios WHERE correo = 'smoke.base@gmail.com'$q$, '23514');

SELECT pg_temp.debe_fallar('tipo de token inválido',
  $q$INSERT INTO tokens (usuario_id, token_hash, tipo, fecha_vencimiento)
     SELECT id, 'h', 'OTRO', now() + interval '30 minutes'
       FROM usuarios WHERE correo = 'smoke.base@gmail.com'$q$, '23514');

-- ---------------------------------------------------------------------
-- 4) Restricciones de SESIONES
-- ---------------------------------------------------------------------
SELECT pg_temp.debe_fallar('sesión con usuario inexistente (FK)',
  $q$INSERT INTO sesiones (id, usuario_id, fecha_vencimiento)
     VALUES (encode(sha256('x'::bytea), 'hex'), 999999999, now() + interval '7 days')$q$, '23503');

SELECT pg_temp.debe_fallar('sesión con id que no es SHA-256 (ej. UUID)',
  $q$INSERT INTO sesiones (id, usuario_id, fecha_vencimiento)
     SELECT gen_random_uuid()::text, id, now() + interval '7 days'
       FROM usuarios WHERE correo = 'smoke.base@gmail.com'$q$, '23514');

SELECT pg_temp.debe_fallar('sesión con id en mayúsculas (debe ser hex minúsculas)',
  $q$INSERT INTO sesiones (id, usuario_id, fecha_vencimiento)
     SELECT upper(encode(sha256('y'::bytea), 'hex')), id, now() + interval '7 days'
       FROM usuarios WHERE correo = 'smoke.base@gmail.com'$q$, '23514');

-- ---------------------------------------------------------------------
-- 5) Flujo básico a nivel de datos (HU-01 -> HU-02 -> HU-03)
-- ---------------------------------------------------------------------
-- HU-01: registro deja cuenta PENDIENTE + token vigente
INSERT INTO tokens (usuario_id, token_hash, fecha_vencimiento)
SELECT id, 'hash-smoke', now() + interval '30 minutes'
  FROM usuarios WHERE correo = 'smoke.base@gmail.com';

SELECT pg_temp.afirmar('registro: cuenta PENDIENTE con 1 token vigente',
  (SELECT u.estado = 'PENDIENTE' AND count(t.*) = 1
     FROM usuarios u JOIN tokens t ON t.usuario_id = u.id
    WHERE u.correo = 'smoke.base@gmail.com' AND NOT t.usado AND t.fecha_vencimiento > now()
    GROUP BY u.estado));

-- HU-02: verificar = activar cuenta + inutilizar token (CA-02-01)
UPDATE tokens SET usado = TRUE, fecha_uso = now()
 WHERE token_hash = 'hash-smoke' AND NOT usado AND fecha_vencimiento > now();
UPDATE usuarios SET estado = 'ACTIVA'
 WHERE correo = 'smoke.base@gmail.com';

SELECT pg_temp.afirmar('verificación: cuenta ACTIVA y token inutilizado',
  (SELECT u.estado = 'ACTIVA' AND t.usado
     FROM usuarios u JOIN tokens t ON t.usuario_id = u.id
    WHERE u.correo = 'smoke.base@gmail.com'));

-- HU-03: login crea sesión; logout la revoca (CA-03-03)
-- Inserta exactamente como lo hace la entidad JPA de E08 (id, usuario_id, vencimiento).
INSERT INTO sesiones (id, usuario_id, fecha_vencimiento)
SELECT encode(sha256('token-smoke'::bytea), 'hex'), id, now() + interval '7 days'
  FROM usuarios WHERE correo = 'smoke.base@gmail.com';

SELECT pg_temp.afirmar('login: sesión válida (no revocada, no vencida)',
  (SELECT count(*) = 1 FROM sesiones s JOIN usuarios u ON u.id = s.usuario_id
    WHERE u.correo = 'smoke.base@gmail.com'
      AND NOT s.revocada AND s.fecha_vencimiento > now()));

-- Logout como lo hace E08: solo revocada = true (sin fecha_revocacion).
UPDATE sesiones SET revocada = TRUE
 WHERE id = encode(sha256('token-smoke'::bytea), 'hex');

SELECT pg_temp.afirmar('logout: ya no quedan sesiones válidas',
  (SELECT count(*) = 0 FROM sesiones s JOIN usuarios u ON u.id = s.usuario_id
    WHERE u.correo = 'smoke.base@gmail.com'
      AND NOT s.revocada AND s.fecha_vencimiento > now()));

-- ---------------------------------------------------------------------
-- 6) Borrado en cascada: al borrar el usuario no quedan huérfanos
-- ---------------------------------------------------------------------
DELETE FROM usuarios WHERE correo = 'smoke.base@gmail.com';
SELECT pg_temp.afirmar('cascada: sin tokens ni sesiones huérfanos',
  (SELECT count(*) = 0 FROM tokens   WHERE usuario_id NOT IN (SELECT id FROM usuarios))
  AND
  (SELECT count(*) = 0 FROM sesiones WHERE usuario_id NOT IN (SELECT id FROM usuarios)));

ROLLBACK;
\o
\echo
\echo '==> SMOKE TEST SUPERADO: todas las comprobaciones pasaron.'
