-- =====================================================================
-- CampusMarket - Datos de prueba Sprint 2 (S2-T14)
-- SOLO para desarrollo/pruebas. Se ejecuta después de S1_datos_prueba.sql.
-- Es re-ejecutable: limpia las tablas del Sprint 2 y vuelve a cargar.
--
-- Cubre:
--   - Contactos públicos y privados (HU-05)
--   - Categorías activas (6 de V3) e inactivas (HU-06)
--   - Productos DISPONIBLE e INACTIVO con sus imágenes (HU-07, HU-08)
--   - Tokens de recuperación vigentes, vencidos y usados (HU-04)
--     (token_hash = SHA-256 hex del código de 6 dígitos)
--       777777 -> vigente  (activa1)
--       888888 -> vencido  (activa2)
--       999999 -> ya usado (activa1)
-- =====================================================================

TRUNCATE TABLE imagenes, productos, contactos, tokens_recuperacion RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- CONTACTOS (públicos y privados)
-- Depende de usuarios creados en S1_datos_prueba.sql:
--   id 2 = activa1@gmail.com | id 3 = activa2@gmail.com
-- ---------------------------------------------------------------------
INSERT INTO contactos (usuario_id, tipo, valor, es_publico) VALUES
    (2, 'WHATSAPP',  '+573001112233', TRUE),
    (2, 'CORREO',    'ana.personal@gmail.com', FALSE),
    (3, 'INSTAGRAM', '@luis_campus', TRUE),
    (3, 'TELEFONO',  '3109998877', FALSE);

-- ---------------------------------------------------------------------
-- CATEGORÍAS INACTIVAS ADICIONALES (las 6 activas vienen de V3)
-- ---------------------------------------------------------------------
INSERT INTO categorias (nombre, activa) VALUES
    ('Tutorías',  FALSE),
    ('Servicios', FALSE)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- PRODUCTOS (DISPONIBLE e INACTIVO)
-- ---------------------------------------------------------------------
INSERT INTO productos (vendedor_id, categoria_id, titulo, descripcion, precio, estado) VALUES
    (2, (SELECT id FROM categorias WHERE nombre = 'Libros'),
        'Cálculo de Stewart 8va Edición',
        'Libro en excelente estado, sin rayones ni hojas sueltas.',
        85000.00, 'DISPONIBLE'),
    (2, (SELECT id FROM categorias WHERE nombre = 'Electrónica'),
        'Calculadora Científica Casio fx-991LAX',
        'Incluye tapa protectora y batería nueva.',
        65000.00, 'DISPONIBLE'),
    (3, (SELECT id FROM categorias WHERE nombre = 'Deportes'),
        'Balón de Baloncesto Molten #7',
        'Uso moderado en cancha cubierta.',
        55000.00, 'DISPONIBLE'),
    (3, (SELECT id FROM categorias WHERE nombre = 'Muebles'),
        'Lámpara LED de Escritorio',
        '3 niveles de intensidad, conexión USB.',
        32000.00, 'DISPONIBLE'),
    (2, (SELECT id FROM categorias WHERE nombre = 'Ropa'),
        'Sudadera Universitaria Talla M',
        'Publicación pausada temporalmente por el vendedor.',
        45000.00, 'INACTIVO'),
    (3, (SELECT id FROM categorias WHERE nombre = 'Otros'),
        'Audífonos Inalámbricos',
        'Pausado mientras se confirma entrega.',
        70000.00, 'INACTIVO');

-- ---------------------------------------------------------------------
-- IMÁGENES DE PRODUCTOS
-- ---------------------------------------------------------------------
INSERT INTO imagenes (producto_id, nombre_archivo, tipo_mime, tamano_bytes) VALUES
    ((SELECT id FROM productos WHERE titulo = 'Cálculo de Stewart 8va Edición'),
        'seed-prod-1-calculo.jpg', 'image/jpeg', 142800),
    ((SELECT id FROM productos WHERE titulo = 'Calculadora Científica Casio fx-991LAX'),
        'seed-prod-2-casio.png', 'image/png', 98400),
    ((SELECT id FROM productos WHERE titulo = 'Balón de Baloncesto Molten #7'),
        'seed-prod-3-balon.jpg', 'image/jpeg', 185200),
    ((SELECT id FROM productos WHERE titulo = 'Lámpara LED de Escritorio'),
        'seed-prod-4-lampara.webp', 'image/webp', 76300),
    ((SELECT id FROM productos WHERE titulo = 'Sudadera Universitaria Talla M'),
        'seed-prod-5-sudadera.jpg', 'image/jpeg', 120500);

-- ---------------------------------------------------------------------
-- TOKENS DE RECUPERACIÓN (vigente, vencido, usado)
-- ---------------------------------------------------------------------
INSERT INTO tokens_recuperacion (usuario_id, token_hash, vencimiento, usado, fecha_creacion) VALUES
    -- activa1: vigente (777777)
    (2, '4ba499fd929c2ef6f648e954f620aa1542bd8c95817a907d7bc7eb43bbafbe07',
        now() + interval '15 minutes', FALSE, now() - interval '1 minute'),
    -- activa2: vencido (888888)
    (3, '270d2e83c2661b4f1b4c2d5c4e2444d936c3e268c8b67b1d452c5f93c5425148',
        now() - interval '1 hour', FALSE, now() - interval '75 minutes'),
    -- activa1: ya usado (999999)
    (2, '888df25ae35772424a560c7152a1de794440e0ea5cfee62828333a456a506e05',
        now() - interval '2 days' + interval '15 minutes', TRUE, now() - interval '2 days');
