-- =====================================================================
-- CampusMarket - Auditoría de integridad SQL (S4-T13, parte 1)
-- SOLO LEE el catálogo y los datos: no modifica nada.
-- Ejecutar con:  ./scripts/auditar.sh
--
-- Severidades:
--   ERROR  -> la base NO protege una regla; hay que corregirlo con una migración.
--   AVISO  -> conviene revisarlo (convención o rendimiento).
--   INFO   -> dato para el informe (p. ej. regla ON DELETE de cada FK).
--
-- CÓMO AMPLIARLO EN EL SPRINT 4: cada sprint agrega tablas. Los chequeos
-- genéricos (1 a 9) ya las cubren solos. Falta solo:
--   a) descomentar/agregar nombres en la lista "esperado" (chequeo 10);
--   b) descomentar los chequeos de datos del Sprint 2/3 (chequeo 11).
-- =====================================================================
\pset format unaligned
\pset tuples_only on
\pset fieldsep ' | '

WITH
tablas AS (            -- tablas propias del proyecto (sin la historia de Flyway)
  SELECT c.oid, c.relname
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
   WHERE n.nspname = 'public' AND c.relkind = 'r'
     AND c.relname <> 'flyway_schema_history'
),
columnas AS (
  SELECT t.oid AS tabla_oid, t.relname AS tabla, a.attnum, a.attname AS columna,
         a.atttypid, format_type(a.atttypid, a.atttypmod) AS tipo
    FROM tablas t
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum > 0 AND NOT a.attisdropped
),
hallazgos(severidad, chequeo, objeto, detalle) AS (

  -- 1) Toda tabla debe tener clave primaria
  SELECT 'ERROR', '01_PK', 'tabla ' || t.relname,
         'La tabla no tiene clave primaria'
    FROM tablas t
   WHERE NOT EXISTS (SELECT 1 FROM pg_constraint k
                      WHERE k.conrelid = t.oid AND k.contype = 'p')

  UNION ALL
  -- 2) Columnas de estado/rol/tipo deben tener un CHECK con los valores válidos
  SELECT 'ERROR', '02_CHECK_ESTADO', c.tabla || '.' || c.columna,
         'Columna de estado/rol/tipo sin CHECK de valores válidos'
    FROM columnas c
   WHERE c.columna ~ '^(estado|rol|tipo)$'
     AND NOT EXISTS (SELECT 1 FROM pg_constraint k
                      WHERE k.conrelid = c.tabla_oid AND k.contype = 'c'
                        AND k.conkey @> ARRAY[c.attnum])

  UNION ALL
  -- 3) Columnas con rango de negocio (precio, descuento, estrellas, intentos...) deben tener CHECK
  SELECT 'ERROR', '03_CHECK_RANGO', c.tabla || '.' || c.columna,
         'Columna numérica de negocio (' || c.tipo || ') sin CHECK de rango'
    FROM columnas c
   WHERE c.columna ~ '(precio|porcentaje|descuento|estrellas|calificacion|puntuacion|intentos|cantidad)'
     AND c.atttypid IN ('integer'::regtype, 'smallint'::regtype, 'bigint'::regtype, 'numeric'::regtype)
     AND NOT EXISTS (SELECT 1 FROM pg_constraint k
                      WHERE k.conrelid = c.tabla_oid AND k.contype = 'c'
                        AND k.conkey @> ARRAY[c.attnum])

  UNION ALL
  -- 4) Contraseñas: nunca en texto plano (la columna debe llamarse *_hash)
  SELECT 'ERROR', '04_PASSWORD_PLANO', c.tabla || '.' || c.columna,
         'Columna de contraseña/clave sin sufijo hash: ¿se guarda en texto plano?'
    FROM columnas c
   WHERE c.columna ~ '(password|contrasena|clave)' AND c.columna !~ 'hash'

  UNION ALL
  -- 5) Fechas siempre con zona horaria (TIMESTAMPTZ)
  SELECT 'AVISO', '05_TIMESTAMP_SIN_ZONA', c.tabla || '.' || c.columna,
         'Usa timestamp sin zona horaria; el proyecto usa TIMESTAMPTZ'
    FROM columnas c
   WHERE c.atttypid = 'timestamp'::regtype

  UNION ALL
  -- 6) Clave foránea sin índice que empiece por sus columnas (JOIN y DELETE lentos)
  SELECT 'AVISO', '06_FK_SIN_INDICE', k.conrelid::regclass || '.' || k.conname,
         'FK sin índice de apoyo'
    FROM pg_constraint k
   WHERE k.contype = 'f' AND k.connamespace = 'public'::regnamespace
     AND NOT EXISTS (
           SELECT 1 FROM pg_index i
            WHERE i.indrelid = k.conrelid
              AND (i.indkey::int2[])[0:cardinality(k.conkey) - 1] = k.conkey)

  UNION ALL
  -- 7) Convención de nombres: fk_ / uq_ / ck_ para restricciones y idx_ para índices
  SELECT 'AVISO', '07_NOMBRE_RESTRICCION', k.conrelid::regclass || '.' || k.conname,
         'La restricción no sigue la convención fk_ / uq_ / ck_'
    FROM pg_constraint k
   WHERE k.connamespace = 'public'::regnamespace AND k.contype IN ('f', 'u', 'c')
     AND k.conname !~ '^(fk|uq|ck)_'
  UNION ALL
  SELECT 'AVISO', '07_NOMBRE_INDICE', i.indrelid::regclass || '.' || ic.relname,
         'El índice no sigue la convención idx_ (o uq_)'
    FROM pg_index i
    JOIN pg_class ic ON ic.oid = i.indexrelid
    JOIN tablas t ON t.oid = i.indrelid
   WHERE NOT i.indisprimary AND ic.relname !~ '^(idx|uq)_'

  UNION ALL
  -- 8) Regla ON DELETE de cada FK (para el informe; revisar que sea la deseada)
  SELECT 'INFO', '08_FK_ON_DELETE', k.conrelid::regclass || '.' || k.conname,
         'ON DELETE ' || CASE k.confdeltype
                           WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT'
                           WHEN 'c' THEN 'CASCADE'   WHEN 'n' THEN 'SET NULL'
                           WHEN 'd' THEN 'SET DEFAULT' END
           || ' -> ' || k.confrelid::regclass
    FROM pg_constraint k
   WHERE k.contype = 'f' AND k.connamespace = 'public'::regnamespace

  UNION ALL
  -- 9) Inventario de tablas (para el informe)
  SELECT 'INFO', '09_TABLA', t.relname,
         (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = t.oid AND k.contype = 'p') || ' PK, '
      || (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = t.oid AND k.contype = 'f') || ' FK, '
      || (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = t.oid AND k.contype = 'u') || ' UNIQUE, '
      || (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = t.oid AND k.contype = 'c') || ' CHECK'
    FROM tablas t

  UNION ALL
  -- 10) Restricciones de negocio que DEBEN existir (por nombre)
  SELECT 'ERROR', '10_RESTRICCION_ESPERADA', e.nombre,
         'No existe la restricción esperada (' || e.regla || ')'
    FROM (VALUES
            -- Sprint 1
            ('uq_usuarios_correo',          'correo único, CA-01-02'),
            ('ck_usuarios_correo_minus',    'correo siempre en minúsculas'),
            ('ck_usuarios_estado',          'PENDIENTE / ACTIVA'),
            ('ck_usuarios_rol',             'ESTUDIANTE / ADMIN'),
            ('fk_tokens_usuario',           'token pertenece a un usuario'),
            ('ck_tokens_tipo',              'VERIFICACION / RECUPERACION'),
            ('ck_tokens_uso_coherente',     'usado <=> fecha_uso'),
            ('fk_sesiones_usuario',         'sesión pertenece a un usuario'),
            ('ck_sesiones_id_hash',         'id de sesión = SHA-256 hex')
            -- Sprint 2 (descomentar cuando existan las tablas; ajustar nombres reales):
            -- ,('uq_categorias_nombre',     'categoría única')
            -- ,('ck_productos_precio',      'precio > 0')
            -- ,('ck_productos_estado',      'estados válidos del producto')
            -- ,('fk_productos_usuario',     'producto tiene dueño')
            -- ,('fk_productos_categoria',   'producto tiene categoría')
            -- Sprint 3:
            -- ,('ck_descuentos_porcentaje', 'rango del descuento')
            -- ,('uq_intereses_producto_comprador', 'un interés por comprador y producto')
            -- ,('uq_ventas_producto',       'una sola venta por producto')
            -- ,('uq_calificaciones_venta',  'una calificación por venta')
            -- ,('ck_calificaciones_estrellas', 'estrellas entre 1 y 5')
         ) AS e(nombre, regla)
   WHERE NOT EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conname = e.nombre)

  UNION ALL
  -- 11) Datos incoherentes (Sprint 1)
  SELECT 'ERROR', '11_DATOS_SESION_PENDIENTE', 'usuario ' || u.id,
         'Hay una sesión vigente de una cuenta PENDIENTE (viola CA-03-02)'
    FROM usuarios u
   WHERE u.estado = 'PENDIENTE'
     AND EXISTS (SELECT 1 FROM sesiones s
                  WHERE s.usuario_id = u.id AND NOT s.revocada AND s.fecha_vencimiento > now())
  UNION ALL
  SELECT 'ERROR', '11_DATOS_TOKEN_USADO_PENDIENTE', 'usuario ' || u.id,
         'Token de verificación usado pero la cuenta sigue PENDIENTE'
    FROM usuarios u
   WHERE u.estado = 'PENDIENTE'
     AND EXISTS (SELECT 1 FROM tokens t
                  WHERE t.usuario_id = u.id AND t.tipo = 'VERIFICACION' AND t.usado)
  UNION ALL
  SELECT 'AVISO', '11_DATOS_TOKENS_VIGENTES_MULTIPLES', 'usuario ' || t.usuario_id,
         'Más de un token ' || t.tipo || ' vigente (el reenvío debe invalidar el anterior)'
    FROM tokens t
   WHERE NOT t.usado AND t.fecha_vencimiento > now()
   GROUP BY t.usuario_id, t.tipo
  HAVING count(*) > 1
  -- Sprint 2/3 (descomentar y ajustar cuando existan las tablas):
  -- UNION ALL
  -- SELECT 'ERROR','11_DATOS_VENDIDO_SIN_VENTA','producto '||p.id,'Producto VENDIDO sin venta'
  --   FROM productos p WHERE p.estado='VENDIDO'
  --    AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.producto_id=p.id)
  -- UNION ALL
  -- SELECT 'ERROR','11_DATOS_VENTA_SIN_VENDIDO','venta '||v.id,'Venta cuyo producto no está VENDIDO'
  --   FROM ventas v JOIN productos p ON p.id=v.producto_id WHERE p.estado<>'VENDIDO'
)
SELECT severidad, chequeo, objeto, detalle
  FROM hallazgos
 ORDER BY CASE severidad WHEN 'ERROR' THEN 1 WHEN 'AVISO' THEN 2 ELSE 3 END,
          chequeo, objeto;
