-- =====================================================================
-- CampusMarket - Migración V4 (S2-T14)
-- Índices adicionales de rendimiento para el Sprint 2.
-- Nota: `tokens_recuperacion` ya fue creada en V3 (S2-T13), junto con
-- los índices básicos de FKs (idx_tokens_rec_usuario, idx_contactos_usuario,
-- idx_productos_vendedor, idx_productos_categoria, idx_imagenes_producto).
-- Aquí se agregan los índices compuestos/parciales para filtrado de catálogo
-- por estado, categoría y precio.
-- =====================================================================

-- 1) Catálogo filtrado por estado + categoría + rango/orden de precio.
--    Cubre consultas del tipo:
--      WHERE estado = 'DISPONIBLE' AND categoria_id = ? AND precio BETWEEN ? AND ?
--    También acelera filtros solo por `estado` (prefijo izquierdo del índice).
CREATE INDEX idx_productos_estado_categoria_precio
    ON productos (estado, categoria_id, precio);

-- 2) Índice parcial por precio para productos DISPONIBLE.
--    Cubre consultas del catálogo cuando el usuario filtra u ordena por precio
--    sin seleccionar una categoría específica, manteniendo el índice pequeño
--    al excluir publicaciones INACTIVO o VENDIDO.
CREATE INDEX idx_productos_precio
    ON productos (precio)
    WHERE estado = 'DISPONIBLE';
