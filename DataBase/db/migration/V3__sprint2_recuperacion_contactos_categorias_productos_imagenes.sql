-- =====================================================================
-- CampusMarket - Migración V3 (S2-T13)
-- Tablas del Sprint 2: tokens_recuperacion, contactos, categorias,
-- productos, imagenes y datos iniciales de categorías.
-- Motor: PostgreSQL 13+ · Compatible con Flyway.
-- HU relacionadas: HU-04 (recuperación), HU-05 (contactos),
--                  HU-06 (categorías), HU-07 (productos e imágenes).
-- Nota: La tabla `tokens` del Sprint 1 (verificación) NO se modifica.
-- =====================================================================

-- ---------------------------------------------------------------------
-- TOKENS DE RECUPERACIÓN (HU-04)
-- Tabla aparte de `tokens` (verificación del Sprint 1).
-- ---------------------------------------------------------------------
CREATE TABLE tokens_recuperacion (
    id              BIGSERIAL    PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL,
    token_hash      VARCHAR(255) NOT NULL,
    vencimiento     TIMESTAMPTZ  NOT NULL,
    usado           BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_tokens_rec_usuario     FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uq_tokens_rec_token_hash  UNIQUE (token_hash),
    CONSTRAINT ck_tokens_rec_vencimiento CHECK (vencimiento > fecha_creacion)
);

COMMENT ON TABLE  tokens_recuperacion            IS 'Tokens de recuperación de contraseña (HU-04). Tabla independiente de la de verificación.';
COMMENT ON COLUMN tokens_recuperacion.token_hash IS 'Hash único del token de recuperación. Nunca en texto plano.';

CREATE INDEX idx_tokens_rec_usuario
    ON tokens_recuperacion (usuario_id);

-- ---------------------------------------------------------------------
-- CONTACTOS (HU-05)
-- Privados por defecto (es_publico = FALSE) hasta que el usuario decida.
-- ---------------------------------------------------------------------
CREATE TABLE contactos (
    id              BIGSERIAL    PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    valor           VARCHAR(150) NOT NULL,
    es_publico      BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_contactos_usuario     FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_contactos_tipo        CHECK (tipo IN ('WHATSAPP', 'TELEFONO', 'CORREO', 'INSTAGRAM', 'TELEGRAM', 'OTRO')),
    CONSTRAINT ck_contactos_valor_vacio CHECK (length(btrim(valor)) > 0)
);

COMMENT ON TABLE  contactos            IS 'Medios de contacto del usuario. Privados por defecto hasta marcar es_publico = TRUE.';
COMMENT ON COLUMN contactos.es_publico IS 'FALSE = privado (solo dueño); TRUE = visible en detalle de productos.';

CREATE INDEX idx_contactos_usuario
    ON contactos (usuario_id);

-- ---------------------------------------------------------------------
-- CATEGORÍAS (HU-06)
-- Nombre único sin distinguir mayúsculas/minúsculas.
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id              BIGSERIAL   PRIMARY KEY,
    nombre          VARCHAR(80) NOT NULL,
    activa          BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_categorias_nombre       UNIQUE (nombre),
    CONSTRAINT ck_categorias_nombre_vacio CHECK (length(btrim(nombre)) > 0)
);

COMMENT ON TABLE categorias IS 'Categorías del catálogo de productos. Nombre único sin distinguir mayúsculas.';

CREATE UNIQUE INDEX uq_categorias_nombre_ci
    ON categorias (lower(btrim(nombre)));

-- ---------------------------------------------------------------------
-- PRODUCTOS (HU-07 / HU-08)
-- Se incluye estado VENDIDO desde ahora para evitar migración en Sprint 3.
-- ---------------------------------------------------------------------
CREATE TABLE productos (
    id              BIGSERIAL      PRIMARY KEY,
    vendedor_id     BIGINT         NOT NULL,
    categoria_id    BIGINT         NOT NULL,
    titulo          VARCHAR(150)   NOT NULL,
    descripcion     TEXT           NOT NULL,
    precio          NUMERIC(12, 2) NOT NULL,
    estado          VARCHAR(15)    NOT NULL DEFAULT 'DISPONIBLE',
    fecha_creacion  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_productos_usuario           FOREIGN KEY (vendedor_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_productos_categoria         FOREIGN KEY (categoria_id)
        REFERENCES categorias (id) ON DELETE RESTRICT,
    CONSTRAINT ck_productos_titulo_vacio      CHECK (length(btrim(titulo)) > 0),
    CONSTRAINT ck_productos_descripcion_vacio CHECK (length(btrim(descripcion)) > 0),
    CONSTRAINT ck_productos_precio            CHECK (precio > 0),
    CONSTRAINT ck_productos_estado            CHECK (estado IN ('DISPONIBLE', 'INACTIVO', 'VENDIDO'))
);

COMMENT ON TABLE  productos        IS 'Publicaciones de productos de los vendedores.';
COMMENT ON COLUMN productos.precio IS 'Precio en moneda local con 2 decimales. Siempre > 0.';
COMMENT ON COLUMN productos.estado IS 'DISPONIBLE | INACTIVO | VENDIDO.';

CREATE INDEX idx_productos_vendedor
    ON productos (vendedor_id);

CREATE INDEX idx_productos_categoria
    ON productos (categoria_id);

CREATE INDEX idx_productos_estado_fecha
    ON productos (estado, fecha_creacion DESC, id DESC);

-- ---------------------------------------------------------------------
-- IMÁGENES DE PRODUCTOS (HU-07)
-- ---------------------------------------------------------------------
CREATE TABLE imagenes (
    id              BIGSERIAL    PRIMARY KEY,
    producto_id     BIGINT       NOT NULL,
    nombre_archivo  VARCHAR(255) NOT NULL,
    tipo_mime       VARCHAR(100) NOT NULL,
    tamano_bytes    BIGINT       NOT NULL,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_imagenes_producto         FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON DELETE CASCADE,
    CONSTRAINT uq_imagenes_nombre_archivo   UNIQUE (nombre_archivo),
    CONSTRAINT ck_imagenes_nombre_vacio     CHECK (length(btrim(nombre_archivo)) > 0),
    CONSTRAINT ck_imagenes_tipo_mime_vacio  CHECK (length(btrim(tipo_mime)) > 0),
    CONSTRAINT ck_imagenes_tamano           CHECK (tamano_bytes > 0)
);

COMMENT ON TABLE imagenes IS 'Metadatos de las imágenes asociadas a cada producto.';

CREATE INDEX idx_imagenes_producto
    ON imagenes (producto_id);

-- ---------------------------------------------------------------------
-- DATOS INICIALES: 6 categorías de ejemplo
-- ---------------------------------------------------------------------
INSERT INTO categorias (nombre) VALUES
    ('Libros'),
    ('Electrónica'),
    ('Ropa'),
    ('Muebles'),
    ('Deportes'),
    ('Otros')
ON CONFLICT DO NOTHING;
