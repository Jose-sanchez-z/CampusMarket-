# Índices, Datos de Prueba y Verificación — Sprint 2 (S2-T14)

Documento breve de soporte para **HU-04, HU-05, HU-06, HU-07 y HU-08**.

---

## 1. Índices creados y justificación

### Agregados en `V3__sprint2_recuperacion_contactos_categorias_productos_imagenes.sql` (S2-T13)
| Índice | Tabla (columnas) | Justificación |
|---|---|---|
| `idx_tokens_rec_usuario` | `tokens_recuperacion (usuario_id)` | Acelera la búsqueda e invalidación de tokens de recuperación de un usuario y apoya la FK `ON DELETE CASCADE`. |
| `idx_contactos_usuario` | `contactos (usuario_id)` | Acelera la consulta de contactos públicos/privados de un usuario en perfil y detalle de producto. |
| `idx_productos_vendedor` | `productos (vendedor_id)` | Acelera el listado "Mis publicaciones" del vendedor (`HU-08`) y apoya la FK. |
| `idx_productos_categoria` | `productos (categoria_id)` | Acelera el filtrado por categoría y la verificación de integridad `ON DELETE RESTRICT` al intentar borrar una categoría. |
| `idx_productos_estado_fecha` | `productos (estado, fecha_creacion DESC, id DESC)` | Acelera el catálogo ordenado por fecha más reciente con orden estable (`DISPONIBLE`). |
| `idx_imagenes_producto` | `imagenes (producto_id)` | Acelera la carga de la galería de fotos de un producto y apoya la FK `ON DELETE CASCADE`. |

### Agregados en `V4__indices_sprint2.sql` (S2-T14)
| Índice | Tabla (columnas) | Justificación |
|---|---|---|
| `idx_productos_estado_categoria_precio` | `productos (estado, categoria_id, precio)` | Índice compuesto para el catálogo cuando se combinan filtros de estado (`DISPONIBLE`), categoría y rango de precio. |
| `idx_productos_precio` | `productos (precio) WHERE estado = 'DISPONIBLE'` | Índice parcial que acelera el filtro/ordenamiento por precio sin categoría, manteniendo el índice pequeño al excluir `INACTIVO` y `VENDIDO`. |

> **Verificación `EXPLAIN`:** El script [`integridad_recuperacion_s2.sql`](../db/tests/integridad_recuperacion_s2.sql) valida automáticamente con `EXPLAIN` (forzando `enable_seqscan = off`) que PostgreSQL utiliza `idx_productos_categoria`, `idx_productos_estado_categoria_precio`, `idx_productos_precio`, `idx_productos_vendedor` e `idx_imagenes_producto`.

---

## 2. Cómo cargar los datos de prueba (Sprint 1 + Sprint 2)

Desde la carpeta `DataBase/`:

```bash
# Levantar esquema limpio + migraciones (V1..V4) + semillas (S1 + S2)
./scripts/reset.sh --seed
```

Las semillas de [`S2_datos_prueba.sql`](../db/seeds/S2_datos_prueba.sql) incluyen:
- **Contactos:** públicos (`es_publico = TRUE`) y privados (`es_publico = FALSE`) para `activa1@gmail.com` y `activa2@gmail.com`.
- **Categorías:** 6 activas (desde `V3`) + 2 inactivas (`Tutorías`, `Servicios`).
- **Productos e imágenes:** 4 productos `DISPONIBLE` y 2 `INACTIVO`, con imágenes asociadas.
- **Tokens de recuperación (`tokens_recuperacion`):**
  - `777777` → vigente (`activa1@gmail.com`)
  - `888888` → vencido (`activa2@gmail.com`)
  - `999999` → ya usado (`activa1@gmail.com`)

---

## 3. Cómo ejecutar las pruebas

```bash
# 1. Reset + semillas + smoke test Sprint 1 + pruebas de integridad, recuperación e índices Sprint 2
./scripts/smoke_test.sh

# 2. Auditoría de integridad (0 ERROR, 0 AVISO)
./scripts/auditar.sh
```
