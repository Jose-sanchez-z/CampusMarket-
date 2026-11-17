-- =====================================================================
-- CampusMarket - Migración V1 (S1-T13)
-- Tablas base de identidad y acceso: usuarios, tokens, sesiones.
-- Motor: PostgreSQL 13+ (usa gen_random_uuid() nativo, sin extensiones).
-- Compatible con Flyway (V<versión>__<descripción>.sql).
-- HU relacionadas: HU-01 (registro), HU-02 (verificación), HU-03 (sesión).
-- =====================================================================

-- ---------------------------------------------------------------------
-- USUARIOS
-- estado: PENDIENTE (registrado, sin verificar) | ACTIVA (verificado)
-- rol:    ESTUDIANTE | ADMIN   (el rol se necesita desde el Sprint 2)
-- El correo se guarda SIEMPRE en minúsculas (lo exige el CHECK), así la
-- restricción UNIQUE es efectivamente insensible a mayúsculas y el
-- duplicado falla a nivel de base de datos (CA-01-02).
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id                   BIGSERIAL    PRIMARY KEY,
    correo               VARCHAR(255) NOT NULL,
    nombre               VARCHAR(120) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    estado               VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE',
    rol                  VARCHAR(12)  NOT NULL DEFAULT 'ESTUDIANTE',
    fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_usuarios_correo        UNIQUE (correo),
    CONSTRAINT ck_usuarios_correo_minus  CHECK (correo = lower(correo)),
    CONSTRAINT ck_usuarios_correo_forma  CHECK (correo ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_usuarios_nombre_vacio  CHECK (length(btrim(nombre)) > 0),
    CONSTRAINT ck_usuarios_estado        CHECK (estado IN ('PENDIENTE', 'ACTIVA')),
    CONSTRAINT ck_usuarios_rol           CHECK (rol IN ('ESTUDIANTE', 'ADMIN'))
);

COMMENT ON TABLE  usuarios               IS 'Cuentas de la plataforma. Una cuenta nace PENDIENTE y pasa a ACTIVA al verificar el token.';
COMMENT ON COLUMN usuarios.correo        IS 'Correo institucional, siempre en minúsculas (CHECK). Único.';
COMMENT ON COLUMN usuarios.password_hash IS 'Hash BCrypt/Argon2. Nunca texto plano.';

-- ---------------------------------------------------------------------
-- TOKENS
-- Token de un solo uso con vencimiento. Se guarda el HASH, no el token.
-- tipo: VERIFICACION (Sprint 1) | RECUPERACION (Sprint 2, HU-04).
--   Se deja el tipo desde ya para no crear otra tabla en el Sprint 2
--   (punto abierto del documento: "quién crea la tabla de recuperación").
-- intentos_fallidos: apoyo para limitar intentos (S1-T10).
-- fecha_creacion:    apoyo para limitar la frecuencia de reenvío (S1-T10).
-- ---------------------------------------------------------------------
CREATE TABLE tokens (
    id                  BIGSERIAL    PRIMARY KEY,
    usuario_id          BIGINT       NOT NULL,
    token_hash          VARCHAR(255) NOT NULL,
    tipo                VARCHAR(12)  NOT NULL DEFAULT 'VERIFICACION',
    fecha_vencimiento   TIMESTAMPTZ  NOT NULL,
    usado               BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_uso           TIMESTAMPTZ,
    intentos_fallidos   INTEGER      NOT NULL DEFAULT 0,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_tokens_usuario        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_tokens_tipo           CHECK (tipo IN ('VERIFICACION', 'RECUPERACION')),
    CONSTRAINT ck_tokens_intentos       CHECK (intentos_fallidos >= 0),
    CONSTRAINT ck_tokens_vencimiento    CHECK (fecha_vencimiento > fecha_creacion),
    -- coherencia: fecha_uso presente si y solo si usado = TRUE
    CONSTRAINT ck_tokens_uso_coherente  CHECK (usado = (fecha_uso IS NOT NULL))
);

COMMENT ON TABLE  tokens            IS 'Tokens de un solo uso (verificación de cuenta y, desde el Sprint 2, recuperación).';
COMMENT ON COLUMN tokens.token_hash IS 'Hash del token enviado por correo. El token en claro nunca se guarda.';

-- Búsqueda del token vigente de un usuario (verificar / reenviar).
CREATE INDEX idx_tokens_usuario_tipo_creacion
    ON tokens (usuario_id, tipo, fecha_creacion DESC);

-- Limpieza / consulta de tokens aún utilizables.
CREATE INDEX idx_tokens_vigentes
    ON tokens (fecha_vencimiento)
    WHERE usado = FALSE;

-- ---------------------------------------------------------------------
-- SESIONES
-- La cookie HttpOnly lleva este id (UUID aleatorio, no adivinable).
-- Una sesión es válida si NOT revocada Y fecha_vencimiento > now().
-- ---------------------------------------------------------------------
CREATE TABLE sesiones (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id          BIGINT       NOT NULL,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_vencimiento   TIMESTAMPTZ  NOT NULL,
    revocada            BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_revocacion    TIMESTAMPTZ,

    CONSTRAINT fk_sesiones_usuario       FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_sesiones_vencimiento   CHECK (fecha_vencimiento > fecha_creacion),
    CONSTRAINT ck_sesiones_rev_coherente CHECK (revocada = (fecha_revocacion IS NOT NULL))
);

COMMENT ON TABLE sesiones IS 'Sesiones de servidor. La cookie HttpOnly solo transporta el id.';

-- Revocar / listar sesiones de un usuario (logout, cambio de clave en HU-04).
CREATE INDEX idx_sesiones_usuario ON sesiones (usuario_id);

-- Validación del middleware: solo sesiones no revocadas.
CREATE INDEX idx_sesiones_activas
    ON sesiones (fecha_vencimiento)
    WHERE revocada = FALSE;
