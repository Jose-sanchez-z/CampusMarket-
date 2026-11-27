-- =====================================================================
-- CampusMarket - Pruebas de integridad, recuperación e índices (S2-T14)
-- Todo corre dentro de una transacción que se REVIERTE: no deja basura.
-- Ejecutar con:  ./scripts/smoke_test.sh
-- =====================================================================
\set ON_ERROR_STOP on
\set QUIET on
BEGIN;
\o /dev/null

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

CREATE FUNCTION pg_temp.afirmar(nombre text, condicion boolean)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF condicion IS TRUE THEN
    RAISE NOTICE 'OK    %', nombre;
  ELSE
    RAISE EXCEPTION 'FALLO %', nombre;
  END IF;
END $$;

CREATE FUNCTION pg_temp.usa_indice(nombre text, consulta text, indice_esperado text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  plan text := '';
  linea text;
BEGIN
  FOR linea IN EXECUTE 'EXPLAIN ' || consulta LOOP
    plan := plan || linea || E'\n';
  END LOOP;
  IF plan ~ indice_esperado THEN
    RAISE NOTICE 'OK    EXPLAIN % usa índice (%)', nombre, indice_esperado;
  ELSE
    RAISE EXCEPTION 'FALLO EXPLAIN % : no usó %.\nPlan:\n%', nombre, indice_esperado, plan;
  END IF;
END $$;

-- ---------------------------------------------------------------------
-- 1) Confirmar existencia de tablas Sprint 2 (S2-T13)
-- ---------------------------------------------------------------------
SELECT pg_temp.afirmar('tokens_recuperacion existe (creada en S2-T13)',
    to_regclass('public.tokens_recuperacion') IS NOT NULL);
SELECT pg_temp.afirmar('contactos existe',  to_regclass('public.contactos')  IS NOT NULL);
SELECT pg_temp.afirmar('categorias existe', to_regclass('public.categorias') IS NOT NULL);
SELECT pg_temp.afirmar('productos existe',  to_regclass('public.productos')  IS NOT NULL);
SELECT pg_temp.afirmar('imagenes existe',   to_regclass('public.imagenes')   IS NOT NULL);

-- ---------------------------------------------------------------------
-- 2) Datos base aislados de la prueba
-- ---------------------------------------------------------------------
INSERT INTO usuarios (correo, nombre, password_hash, estado)
VALUES ('s2.vendedor@gmail.com', 'Vendedor S2', '$2a$10$hashinicial', 'ACTIVA');

INSERT INTO categorias (nombre) VALUES ('CatPruebaS2');

INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio)
SELECT u.id, c.id, 'Producto Integridad S2', 'Descripción válida', 25000.00
  FROM usuarios u, categorias c
 WHERE u.correo = 's2.vendedor@gmail.com' AND c.nombre = 'CatPruebaS2';

INSERT INTO imagenes (producto_id, nombre_archivo, tipo_mime, tamano_bytes)
SELECT p.id, 's2-prueba-img.jpg', 'image/jpeg', 51200
  FROM productos p WHERE p.titulo = 'Producto Integridad S2';

-- ---------------------------------------------------------------------
-- 3) Pruebas de integridad (FK, precio, estado, huérfanos, cascada)
-- ---------------------------------------------------------------------
SELECT pg_temp.debe_fallar(
    'borrar categoría con productos (FK RESTRICT)',
    $q$DELETE FROM categorias WHERE nombre = 'CatPruebaS2'$q$,
    '23503');

SELECT pg_temp.debe_fallar(
    'producto con precio = 0',
    $q$INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio)
       SELECT u.id, c.id, 'P0', 'Desc', 0
         FROM usuarios u, categorias c
        WHERE u.correo = 's2.vendedor@gmail.com' AND c.nombre = 'CatPruebaS2'$q$,
    '23514');

SELECT pg_temp.debe_fallar(
    'producto con precio negativo',
    $q$INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio)
       SELECT u.id, c.id, 'Pneg', 'Desc', -1500
         FROM usuarios u, categorias c
        WHERE u.correo = 's2.vendedor@gmail.com' AND c.nombre = 'CatPruebaS2'$q$,
    '23514');

SELECT pg_temp.debe_fallar(
    'producto con estado inválido',
    $q$INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio, estado)
       SELECT u.id, c.id, 'Pest', 'Desc', 1000, 'ELIMINADO'
         FROM usuarios u, categorias c
        WHERE u.correo = 's2.vendedor@gmail.com' AND c.nombre = 'CatPruebaS2'$q$,
    '23514');

SELECT pg_temp.debe_fallar(
    'producto con categoría inexistente (FK)',
    $q$INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio)
       SELECT id, 9999999, 'Pcat', 'Desc', 1000
         FROM usuarios WHERE correo = 's2.vendedor@gmail.com'$q$,
    '23503');

SELECT pg_temp.debe_fallar(
    'producto con vendedor inexistente (FK)',
    $q$INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio)
       SELECT 9999999, id, 'Pvend', 'Desc', 1000
         FROM categorias WHERE nombre = 'CatPruebaS2'$q$,
    '23503');

SELECT pg_temp.debe_fallar(
    'imagen sin producto válido (FK)',
    $q$INSERT INTO imagenes (producto_id, nombre_archivo, tipo_mime, tamano_bytes)
       VALUES (9999999, 'huerfana.jpg', 'image/jpeg', 1024)$q$,
    '23503');

SELECT pg_temp.afirmar(
    'no existen productos huérfanos',
    NOT EXISTS (
        SELECT 1 FROM productos p
         WHERE NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.id = p.vendedor_id)
            OR NOT EXISTS (SELECT 1 FROM categorias c WHERE c.id = p.categoria_id)
    ));

SELECT pg_temp.afirmar(
    'no existen imágenes sin producto',
    NOT EXISTS (
        SELECT 1 FROM imagenes i
         WHERE NOT EXISTS (SELECT 1 FROM productos p WHERE p.id = i.producto_id)
    ));

DELETE FROM productos WHERE titulo = 'Producto Integridad S2';
SELECT pg_temp.afirmar(
    'borrar producto elimina sus imágenes en cascada',
    NOT EXISTS (SELECT 1 FROM imagenes WHERE nombre_archivo = 's2-prueba-img.jpg'));

-- ---------------------------------------------------------------------
-- 4) Pruebas del flujo de recuperación a nivel de datos (HU-04)
-- ---------------------------------------------------------------------
-- Sesión activa previa al cambio de contraseña
INSERT INTO sesiones (id, usuario_id, fecha_vencimiento)
SELECT 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', id, now() + interval '7 days'
  FROM usuarios WHERE correo = 's2.vendedor@gmail.com';

-- Token de verificación en tabla `tokens` (Sprint 1)
INSERT INTO tokens (usuario_id, token_hash, tipo, fecha_vencimiento)
SELECT id, 'hash_verif_solo_sprint1', 'VERIFICACION', now() + interval '30 minutes'
  FROM usuarios WHERE correo = 's2.vendedor@gmail.com';

-- Token de recuperación vigente y token de recuperación vencido
INSERT INTO tokens_recuperacion (usuario_id, token_hash, vencimiento)
SELECT id, 'hash_rec_vigente_s2', now() + interval '15 minutes'
  FROM usuarios WHERE correo = 's2.vendedor@gmail.com';

INSERT INTO tokens_recuperacion (usuario_id, token_hash, vencimiento, fecha_creacion)
SELECT id, 'hash_rec_vencido_s2', now() - interval '5 minutes', now() - interval '20 minutes'
  FROM usuarios WHERE correo = 's2.vendedor@gmail.com';

SELECT pg_temp.afirmar(
    'token de verificación no se encuentra en tokens_recuperacion',
    NOT EXISTS (SELECT 1 FROM tokens_recuperacion WHERE token_hash = 'hash_verif_solo_sprint1'));

SELECT pg_temp.afirmar(
    'token de recuperación vencido se identifica y excluye de vigentes',
    EXISTS (SELECT 1 FROM tokens_recuperacion
             WHERE token_hash = 'hash_rec_vencido_s2' AND vencimiento <= now())
    AND NOT EXISTS (SELECT 1 FROM tokens_recuperacion
                     WHERE token_hash = 'hash_rec_vencido_s2'
                       AND NOT usado AND vencimiento > now()));

-- Confirmar recuperación: consumir token, cambiar clave y revocar sesiones
UPDATE tokens_recuperacion
   SET usado = TRUE
 WHERE token_hash = 'hash_rec_vigente_s2' AND NOT usado AND vencimiento > now();

UPDATE usuarios
   SET password_hash = '$2a$10$nuevohashrecuperado'
 WHERE correo = 's2.vendedor@gmail.com';

UPDATE sesiones
   SET revocada = TRUE, fecha_revocacion = now()
 WHERE usuario_id = (SELECT id FROM usuarios WHERE correo = 's2.vendedor@gmail.com')
   AND NOT revocada;

SELECT pg_temp.afirmar(
    'token de recuperación usado no se puede reutilizar',
    NOT EXISTS (SELECT 1 FROM tokens_recuperacion
                 WHERE token_hash = 'hash_rec_vigente_s2'
                   AND NOT usado AND vencimiento > now()));

SELECT pg_temp.afirmar(
    'sesiones quedan revocadas tras confirmar recuperación',
    NOT EXISTS (SELECT 1 FROM sesiones s
                  JOIN usuarios u ON u.id = s.usuario_id
                 WHERE u.correo = 's2.vendedor@gmail.com'
                   AND NOT s.revocada AND s.fecha_vencimiento > now()));

-- ---------------------------------------------------------------------
-- 5) Verificación EXPLAIN de uso de índices
-- ---------------------------------------------------------------------
SET LOCAL enable_seqscan = off;

SELECT pg_temp.usa_indice(
    'productos por categoria_id',
    'SELECT id FROM productos WHERE categoria_id = 1',
    'idx_productos_categoria');

SELECT pg_temp.usa_indice(
    'productos por estado, categoria_id y precio',
    'SELECT id FROM productos WHERE estado = ''INACTIVO'' AND categoria_id = 1 AND precio BETWEEN 10000 AND 90000',
    'idx_productos_estado_categoria_precio');

SELECT pg_temp.usa_indice(
    'productos DISPONIBLE por precio',
    'SELECT id FROM productos WHERE estado = ''DISPONIBLE'' AND precio <= 60000 ORDER BY precio',
    'idx_productos_precio');

SELECT pg_temp.usa_indice(
    'productos por vendedor_id',
    'SELECT id FROM productos WHERE vendedor_id = 2',
    'idx_productos_vendedor');

SELECT pg_temp.usa_indice(
    'imagenes por producto_id',
    'SELECT id FROM imagenes WHERE producto_id = 1',
    'idx_imagenes_producto');

ROLLBACK;

\echo ''
\echo '==> PRUEBAS SPRINT 2 SUPERADAS: integridad, recuperación e índices OK.'
