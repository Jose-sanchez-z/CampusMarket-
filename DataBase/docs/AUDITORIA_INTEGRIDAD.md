# Auditoría de integridad SQL — S4-T13 (E09)

**Estado: parte 1 de 2.** La herramienta de auditoría está lista y probada. Los resultados de este informe cubren
**solo las tablas del Sprint 1** (`usuarios`, `tokens`, `sesiones`). Las tablas de los Sprints 2 y 3 (categorías,
productos, imágenes, contactos, descuentos, intereses, ventas, calificaciones) todavía no existen, así que la
auditoría completa y su cierre se hacen en el Sprint 4 (ver sección 5).

## 1. Cómo correrla

```bash
./DataBase/scripts/reset.sh --seed       # opcional: base limpia con datos de prueba
./DataBase/scripts/auditar.sh            # solo LEE; sale con código 1 si hay ERROR
```
Archivos: `DataBase/db/audit/auditoria_integridad.sql` y `DataBase/scripts/auditar.sh`.
Conexión por defecto: la del `docker-compose` (`DATABASE_URL` para cambiarla).

## 2. Qué revisa

| # | Chequeo | Severidad | Por qué |
|---|---|---|---|
| 01 | Toda tabla tiene PK | ERROR | identidad de cada fila |
| 02 | `estado` / `rol` / `tipo` con CHECK de valores válidos | ERROR | estados inválidos imposibles |
| 03 | Columnas de rango (precio, porcentaje, estrellas, intentos…) con CHECK | ERROR | reglas de negocio en la BD |
| 04 | Contraseñas solo como `*_hash` | ERROR | nunca texto plano |
| 05 | Fechas con zona horaria (`TIMESTAMPTZ`) | AVISO | convención del proyecto |
| 06 | FK con índice de apoyo | AVISO | rendimiento de JOIN y DELETE |
| 07 | Nombres `fk_` / `uq_` / `ck_` / `idx_` | AVISO | trazabilidad en errores |
| 08 | Regla `ON DELETE` de cada FK | INFO | revisar que sea la deseada |
| 09 | Inventario de PK / FK / UNIQUE / CHECK por tabla | INFO | para el informe |
| 10 | Restricciones de negocio que **deben** existir (lista por nombre) | ERROR | detecta si alguien borró una |
| 11 | Datos incoherentes (sesión vigente de cuenta PENDIENTE, token usado con cuenta pendiente, tokens vigentes duplicados) | ERROR / AVISO | estados que la app nunca debería producir |

## 3. Prueba de que la auditoría detecta fallos

Se creó una tabla mal hecha a propósito (sin PK, `estado` sin CHECK, `precio` sin rango, columna `password`,
`TIMESTAMP` sin zona, FK sin índice y sin nombre estándar), datos incoherentes y se borró la restricción
`uq_usuarios_correo`. Resultado: **7 ERROR y 5 AVISO detectados, código de salida 1**. Después se restauró la base.

## 4. Resultado sobre el esquema del Sprint 1 (con datos de prueba cargados)

**0 ERROR, 0 AVISO. Auditoría superada.**

| Tabla | PK | FK | UNIQUE | CHECK |
|---|---|---|---|---|
| `usuarios` | `usuarios_pkey (id)` | — | `uq_usuarios_correo (correo)` | 5: formato de correo, correo en minúsculas, estado, nombre no vacío, rol |
| `tokens` | `tokens_pkey (id)` | `fk_tokens_usuario → usuarios` | — | 4: intentos ≥ 0, tipo válido, `usado ⇔ fecha_uso`, vencimiento > creación |
| `sesiones` | `sesiones_pkey (id)` | `fk_sesiones_usuario → usuarios` | — | 2: `id` = SHA-256 hex, vencimiento > creación |

**Reglas `ON DELETE`:** `tokens.fk_tokens_usuario` y `sesiones.fk_sesiones_usuario` son `CASCADE`.
Es intencional: al borrar un usuario, sus tokens y sesiones no deben quedar huérfanos.
(Más adelante, para categorías con productos y productos con venta, se usará `RESTRICT`: S2-T14 lo exige.)

**Hallazgos y decisiones del Sprint 1**
1. `ck_sesiones_rev_coherente` (V1) rechazaba el logout del backend, que solo marca `revocada = true`. Se eliminó
   en la migración V2. La validez de una sesión depende de `revocada`; `fecha_revocacion` es informativa.
2. `sesiones.id` pasó de UUID a hash SHA-256 (V2): si alguien lee la tabla no obtiene cookies utilizables.
3. Las PK conservan el nombre por defecto de PostgreSQL (`<tabla>_pkey`); la auditoría las excluye de la convención.

## 5. Pendiente para cerrar S4-T13 (Sprint 4)

Cuando existan todas las tablas:
- [ ] En `auditoria_integridad.sql`, chequeo 10: descomentar/ajustar los nombres de las restricciones de Sprint 2 y 3
      (`ck_productos_precio`, `ck_descuentos_porcentaje`, `uq_ventas_producto`, `uq_calificaciones_venta`,
      `ck_calificaciones_estrellas`, etc.).
- [ ] Chequeo 11: descomentar los chequeos de datos (producto VENDIDO sin venta, venta con producto no VENDIDO).
- [ ] Correr `./scripts/auditar.sh` sobre la base completa; corregir cada ERROR con una migración nueva (`V<n>__...`).
- [ ] Revisar las reglas `ON DELETE` de todas las FK (categoría con productos, producto con venta/interés).
- [ ] Pendiente de definir con el Product Owner: descuento 100 %, 0 o negativos (HU-11) para el CHECK del porcentaje.
- [ ] Actualizar este documento con el resultado final y la captura de la ejecución (evidencia del entregable).
