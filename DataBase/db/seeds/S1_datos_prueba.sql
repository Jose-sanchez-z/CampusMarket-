-- =====================================================================
-- CampusMarket - Datos de prueba Sprint 1 (S1-T14)
-- SOLO para desarrollo/pruebas. Es re-ejecutable: limpia y vuelve a cargar.
--
-- CONTRASEÑAS (hash BCrypt $2a$10, compatible con BCryptPasswordEncoder):
--   estudiantes:  Campus123*
--   admin:        Admin123*
--
-- TOKENS: token_hash = SHA-256 (hex) del código de 6 dígitos.
--   Si el backend (E07) usa otro algoritmo de hash, avisar para ajustar.
--     111111 -> vigente      (pendiente1)
--     222222 -> vigente      (pendiente2, con 4 intentos fallidos)
--     333333 -> vencido      (vencido)
--     444444 -> ya usado     (activa1, activó su cuenta con este)
--     555555 -> ya usado     (activa2)
--     666666 -> vigente RECUPERACION (activa1)
-- =====================================================================

TRUNCATE TABLE sesiones, tokens, usuarios RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- USUARIOS
-- ---------------------------------------------------------------------
INSERT INTO usuarios (correo, nombre, password_hash, estado, rol) VALUES
 -- id 1: administrador activo
 ('admin@gmail.com',      'Admin Campus',        '$2a$10$0j3yYCva271yy4mSr6oBeeVbWT9Rdcr.bvkd.TMdMyTp9fBTmXGsm', 'ACTIVA',    'ADMIN'),
 -- id 2,3: estudiantes activos
 ('activa1@gmail.com',    'Ana Activa Uno',      '$2a$10$Ben9BJv9fnYLxeZx6QnxqeES4f42MOoXZOJfX8jYTYgkK8GLR3G7e', 'ACTIVA',    'ESTUDIANTE'),
 ('activa2@gmail.com',    'Luis Activo Dos',     '$2a$10$Ben9BJv9fnYLxeZx6QnxqeES4f42MOoXZOJfX8jYTYgkK8GLR3G7e', 'ACTIVA',    'ESTUDIANTE'),
 -- id 4,5,6: pendientes (token vigente / vigente con intentos / vencido)
 ('pendiente1@gmail.com', 'Paula Pendiente Uno', '$2a$10$Ben9BJv9fnYLxeZx6QnxqeES4f42MOoXZOJfX8jYTYgkK8GLR3G7e', 'PENDIENTE', 'ESTUDIANTE'),
 ('pendiente2@gmail.com', 'Pedro Pendiente Dos', '$2a$10$Ben9BJv9fnYLxeZx6QnxqeES4f42MOoXZOJfX8jYTYgkK8GLR3G7e', 'PENDIENTE', 'ESTUDIANTE'),
 ('vencido@gmail.com',    'Vera Vencida',        '$2a$10$Ben9BJv9fnYLxeZx6QnxqeES4f42MOoXZOJfX8jYTYgkK8GLR3G7e', 'PENDIENTE', 'ESTUDIANTE');

-- ---------------------------------------------------------------------
-- TOKENS  (vigentes, vencidos y usados)
-- ---------------------------------------------------------------------
INSERT INTO tokens
 (usuario_id, token_hash, tipo, fecha_creacion, fecha_vencimiento, usado, fecha_uso, intentos_fallidos)
VALUES
 -- pendiente1: vigente (creado hace 5 min, vence en 25 min)
 (4, 'bcb15f821479b4d5772bd0ca866c00ad5f926e3580720659cc80d39c9d09802a', 'VERIFICACION',
     now() - interval '5 minutes',  now() + interval '25 minutes', FALSE, NULL, 0),
 -- pendiente2: vigente con intentos fallidos (para probar el límite)
 (5, '4cc8f4d609b717356701c57a03e737e5ac8fe885da8c7163d3de47e01849c635', 'VERIFICACION',
     now() - interval '10 minutes', now() + interval '20 minutes', FALSE, NULL, 4),
 -- vencido: expiró hace 1 hora (CA-02-02)
 (6, '68487dc295052aa79c530e283ce698b8c6bb1b42ff0944252e1910dbecdc5425', 'VERIFICACION',
     now() - interval '90 minutes', now() - interval '1 hour',     FALSE, NULL, 0),
 -- activa1: token ya usado (CA-02-01/CA-02-02)
 (2, '69f7f7a7f8bca9970fa6f9c0b8dad06901d3ef23fd599d3213aa5eee5621c3e3', 'VERIFICACION',
     now() - interval '2 days',     now() - interval '2 days' + interval '30 minutes', TRUE, now() - interval '2 days' + interval '10 minutes', 0),
 -- activa2: token ya usado
 (3, 'af41e68e1309fa29a5044cbdc36b90a3821d8807e68c7675a6c495112bc8a55f', 'VERIFICACION',
     now() - interval '3 days',     now() - interval '3 days' + interval '30 minutes', TRUE, now() - interval '3 days' + interval '5 minutes', 0),
 -- activa1: token de recuperación vigente (Sprint 2, HU-04)
 (2, '94edf28c6d6da38fd35d7ad53e485307f89fbeaf120485c8d17a43f323deee71', 'RECUPERACION',
     now() - interval '1 minute',   now() + interval '29 minutes', FALSE, NULL, 0);

-- ---------------------------------------------------------------------
-- SESIONES  (válida, vencida, revocada)
-- sesiones.id = SHA-256 (hex) del token de la cookie. Para probar con curl,
-- usar como valor de la cookie el TOKEN EN CLARO (no el hash):
--   activa1 (válida)    -> sesion-activa1-prueba
--   admin   (válida)    -> sesion-admin-prueba
--   activa2 (vencida)   -> sesion-activa2-vencida
--   activa2 (revocada)  -> sesion-activa2-revocada
-- (el nombre de la cookie lo define E08 en S1-T11)
-- ---------------------------------------------------------------------
INSERT INTO sesiones (id, usuario_id, fecha_creacion, fecha_vencimiento, revocada, fecha_revocacion) VALUES
 -- activa1: sesión válida (vence en 7 días)
 ('0e2f0fe0c6430619ab750c62f68184aa912428e10a95f7fd73d079e01b50222a', 2, now() - interval '1 hour', now() + interval '7 days', FALSE, NULL),
 -- admin: sesión válida
 ('d0a792c45402b679d3ec7d93d1461d5860f444a29b257eee14fde8563369e1d1', 1, now() - interval '1 hour', now() + interval '7 days', FALSE, NULL),
 -- activa2: sesión vencida
 ('2ee9dec3936fd39cd514d53f76f367aaf62ccefc685810aaf7a1399055f626c1', 3, now() - interval '9 days', now() - interval '2 days', FALSE, NULL),
 -- activa2: sesión revocada (logout hecho, CA-03-03)
 ('382a901cb3cf4bf979773278fdb395ce74f3df68aa1fc6ded79fac9927e4e9ae', 3, now() - interval '3 hours', now() + interval '7 days', TRUE, now() - interval '1 hour');
