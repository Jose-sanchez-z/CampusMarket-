# CampusMarket — Base de datos (E09)

PostgreSQL 13+ · migraciones compatibles con Flyway · Sprint 1 (S1-T13 y S1-T14).

## Levantar la base desde cero (un solo comando)

```bash
docker compose up -d                 # Postgres 16 en localhost:5432 (campus / campus / campusmarket)
./scripts/reset.sh                   # esquema limpio + migraciones
./scripts/reset.sh --seed            # lo mismo + datos de prueba
./scripts/smoke_test.sh              # reset + semillas + todas las pruebas (exit != 0 si algo falla)
```

Otra base: `DATABASE_URL="postgresql://user:pass@host:5432/db" ./scripts/reset.sh`

> `reset.sh` **borra el esquema `public`**. Solo para desarrollo.

## Estructura

```
db/migration/V1__usuarios_tokens_sesiones.sql   S1-T13  tablas base
db/migration/V2__sesiones_id_hash_token.sql     S1-T13  sesiones.id = hash SHA-256 (pedido de E08)
db/seeds/S1_datos_prueba.sql                    S1-T14  datos de prueba
db/tests/smoke_test.sql                         S1-T14  restricciones + flujo HU-01..03
scripts/reset.sh | smoke_test.sh
docker-compose.yml
```

Para Spring Boot + Flyway: copiar `db/migration/` a `src/main/resources/db/migration/`
(o `spring.flyway.locations=filesystem:db/migration`). **Nunca** editar una migración ya
mergeada: los cambios van en `V2__...`, `V3__...`.

## Modelo (Sprint 1)

```
usuarios 1 ───< tokens
    1 ───────< sesiones
```

| Tabla | Columnas clave | Reglas en la BD |
|---|---|---|
| `usuarios` | `id`, `correo`, `nombre`, `password_hash`, `estado`, `rol`, `fecha_creacion`, `fecha_actualizacion` | `correo` UNIQUE y siempre en minúsculas (CHECK) · formato de correo · `estado` ∈ {PENDIENTE, ACTIVA} (default PENDIENTE) · `rol` ∈ {ESTUDIANTE, ADMIN} (default ESTUDIANTE) |
| `tokens` | `id`, `usuario_id`, `token_hash`, `tipo`, `fecha_vencimiento`, `usado`, `fecha_uso`, `intentos_fallidos`, `fecha_creacion` | FK → usuarios (CASCADE) · `tipo` ∈ {VERIFICACION, RECUPERACION} · vencimiento > creación · `usado` ⇔ `fecha_uso` no nula |
| `sesiones` | `id` (VARCHAR(64) = SHA-256 hex del token de la cookie), `usuario_id`, `fecha_creacion`, `fecha_vencimiento`, `revocada`, `fecha_revocacion` (opcional) | FK → usuarios (CASCADE) · `id` debe ser hex minúsculas de 64 chars (CHECK) · vencimiento > creación |

Sesión válida = `NOT revocada AND fecha_vencimiento > now()`.
Token utilizable = `NOT usado AND fecha_vencimiento > now()`.

## Datos de prueba

Contraseñas: estudiantes `Campus123*`, admin `Admin123*`.

| Correo | Estado | Para probar |
|---|---|---|
| admin@gmail.com | ACTIVA / ADMIN | rol admin, sesión válida |
| activa1@gmail.com | ACTIVA | login OK, token ya usado, token de recuperación vigente |
| activa2@gmail.com | ACTIVA | sesión vencida y sesión revocada |
| pendiente1@gmail.com | PENDIENTE | verificar con token `111111` (vigente) |
| pendiente2@gmail.com | PENDIENTE | token `222222` con 4 intentos fallidos (límite) |
| vencido@gmail.com | PENDIENTE | token `333333` vencido (CA-02-02) |

`token_hash` en las semillas es SHA-256 hex del código de 6 dígitos.

## Decisiones que conviene confirmar con el equipo

1. **Tabla de tokens única con `tipo`.** Sirve para verificación (Sprint 1) y recuperación (HU-04, Sprint 2), lo que
   resuelve el punto abierto "¿quién crea la tabla de recuperación?". Si prefieren tablas separadas, se cambia en V2.
2. **Hash del token** (E07): las semillas asumen SHA-256 hex. Si usan BCrypt u otro, avisar y ajusto las semillas.
3. **Nombre de la cookie** lo define E08 (S1-T11); la BD solo guarda el hash SHA-256 del token (`sesiones.id`), nunca el token en claro.
4. **Rol:** `ESTUDIANTE` / `ADMIN` (el documento usa `ADMIN` en el Sprint 2).
5. **Correo en minúsculas:** el backend debe normalizar (`toLowerCase()`) antes de insertar; si no, la BD rechaza
   el insert (CHECK). Así `Ana@x.co` y `ana@x.co` no pueden coexistir.
6. **Duplicado concurrente:** capturar `DataIntegrityViolationException` (SQLSTATE 23505) y responder 409, además
   de la verificación previa.
