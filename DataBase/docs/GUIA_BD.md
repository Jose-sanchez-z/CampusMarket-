# Guía de Base de Datos — CampusMarket (E09)

PostgreSQL 16 · pgAdmin 4 · migraciones Flyway-compatibles.
Estado: Sprint 1 (S1-T13 migración, S1-T14 semillas + smoke test).

---

## 1. Conectar pgAdmin 4

1. Levanta la base: `docker compose up -d` y luego `./scripts/reset.sh --seed`.
2. En pgAdmin 4: clic derecho en **Servers → Register → Server…**
   - **General → Name:** `CampusMarket local`
   - **Connection → Host:** `localhost` · **Port:** `5432`
   - **Maintenance database:** `campusmarket` · **Username:** `campus` · **Password:** `campus`
3. Ver las tablas: `Servers → CampusMarket local → Databases → campusmarket → Schemas → public → Tables`.
4. Consultar: clic derecho en `campusmarket` → **Query Tool** (o `Alt+Shift+Q`).
5. Ver el plan de una consulta: en el Query Tool, botón **Explain Analyze** (`Shift+F7`).

> ⚠️ **Regla de oro:** pgAdmin es para **mirar y consultar**. No crees ni modifiques tablas con el menú
> *Create / Properties* de pgAdmin. Todo cambio de estructura va en un archivo de migración, si no los demás
> no podrán reproducir tu base y se rompe el trabajo.

---

## 2. Reglas duras (no negociables)

1. **Nunca editar una migración ya mergeada.** Un cambio nuevo es `V2__...sql`, `V3__...sql`. Flyway guarda un
   checksum; editar una migración vieja rompe el arranque de todos.
2. **Todo cambio de estructura = migración.** Nada a mano en pgAdmin.
3. **La BD protege las reglas por sí sola:** PK, FK, UNIQUE, CHECK y NOT NULL. No depender solo del backend.
4. **Backend con `ddl-auto=validate`** (nunca `update` ni `create`): Hibernate no debe tocar el esquema.
5. **Sin datos reales** en semillas ni en el repo. Las semillas son solo para desarrollo.
6. **Contraseñas y tokens nunca en texto plano:** solo hash.
7. **Cada migración es repetible desde cero:** `./scripts/reset.sh` debe funcionar siempre en una base vacía.
8. **Probar antes del PR:** `./scripts/smoke_test.sh` debe terminar con `SMOKE TEST SUPERADO`.

---

## 3. Cómo hacer commit y PR de la base de datos (por separado)

La BD va en **sus propias ramas y PR**, separada del código de backend y frontend. Así E07/E08 pueden
revisarla rápido y, si algo falla, se revierte sin tocar su trabajo.

### 3.1 Una rama por tarea
```bash
git checkout develop
git pull origin develop
git checkout -b feat/S1-T13-migracion-inicial        # T13: migración
# más adelante, rama aparte:
# git checkout -b feat/S1-T14-semillas-smoke-test    # T14: semillas + smoke test
```

### 3.2 Qué archivos entran en cada rama
| Rama | Archivos |
|---|---|
| `feat/S1-T13-...` | `db/migration/V1__usuarios_tokens_sesiones.sql`, `docker-compose.yml`, `scripts/reset.sh`, `README.md` |
| `feat/S1-T14-...` | `db/seeds/S1_datos_prueba.sql`, `db/tests/smoke_test.sql`, `scripts/smoke_test.sh` |

> La ubicación exacta dentro del repo se ajusta a la estructura de carpetas que acordó el equipo
> (ver sección 6).

### 3.3 Commit
```bash
git status                                  # revisa que SOLO aparezcan archivos de BD
git add db/ scripts/ docker-compose.yml README.md      # agrega por carpeta, NO "git add ." a ciegas
git diff --staged                           # relee lo que vas a subir
git commit -m "feat(S1-T13): migración inicial de usuarios, tokens y sesiones"
git push -u origin feat/S1-T13-migracion-inicial
```

Prefijos de mensaje: `feat` (algo nuevo), `fix` (corrección), `test` (pruebas), `docs` (documentación),
`chore` (scripts/configuración). Siempre con el código de la tarea: `feat(S1-T14): ...`.

### 3.4 Pull Request
- Destino: `develop`. Nunca a `main`, nunca push directo a `develop`.
- Descripción obligatoria: `Closes #<número-del-issue>`.
- Agrega a **E07 y E08** como revisores (son quienes usan las tablas).
- Cuando se haga merge:
```bash
git checkout develop
git pull origin develop
git branch -d feat/S1-T13-migracion-inicial
```

### 3.5 Si te equivocas
- **Antes del merge:** corriges el archivo, `git add` + `git commit` (o `git commit --amend` si aún no hiciste push).
- **Después del merge:** **no** edites la migración. Creas `V2__correccion_xxx.sql` en una rama nueva.

---

## 4. Lo que deben hacer los demás (E07 y E08)

**Configuración de Spring Boot** (`application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/campusmarket
spring.datasource.username=campus
spring.datasource.password=campus

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```
Dependencias (Flyway 10 o superior): `flyway-core` **y** `flyway-database-postgresql`, más el driver `postgresql`.

**Mapeo de columnas (para evitar errores de tipos):**
| Columna | Tipo en BD | Tipo Java sugerido |
|---|---|---|
| `usuarios.id`, `tokens.id`, `*_id` | `BIGINT` | `Long` |
| `sesiones.id` | `VARCHAR(64)` (SHA-256 hex minúsculas del token de la cookie) | `String` |
| `fecha_*` | `TIMESTAMPTZ` | `Instant` o `OffsetDateTime` |
| `estado`, `rol`, `tipo` | `VARCHAR` + CHECK | `enum` con `@Enumerated(EnumType.STRING)` |

**Reglas que el backend debe respetar:**
- Guardar el **correo en minúsculas** (`correo.trim().toLowerCase()`); si no, la BD rechaza el insert.
- Duplicado de correo: además de verificar antes, capturar `DataIntegrityViolationException` y responder **409**.
- Marcar un token como usado: `usado = true` **y** `fecha_uso = now()` juntos (la BD exige que vayan a la par).
- Revocar sesión: basta `revocada = true`; `fecha_revocacion` es opcional.
- `sesiones.id` = SHA-256 en **hex minúsculas** (64 caracteres) del token de la cookie. El token en claro solo vive en la cookie.
- Nombres de columna exactos (`CLAUDE.md`): la fecha de expiración es `fecha_vencimiento` en `sesiones` y `tokens`; mapear con `@Column(name = "fecha_vencimiento")`.
- Sesión válida = `NOT revocada AND fecha_vencimiento > now()`.

**Mensaje listo para avisar al equipo cuando se haga merge:**
> La migración V1 (usuarios, tokens, sesiones) ya está en `develop`. Para levantar la BD: `docker compose up -d`
> y `./scripts/reset.sh --seed`. Config en la guía `docs/GUIA_BD.md` sección 4. Ojo: `ddl-auto=validate`, correo
> en minúsculas, y el token se guarda como hash (las semillas asumen SHA-256 hex).

---

## 5. Optimización (nivel Sprint 1)

Índices creados en V1 y para qué sirven:
| Índice | Consulta que acelera |
|---|---|
| `uq_usuarios_correo` (UNIQUE) | login, registro, buscar por correo |
| `idx_tokens_usuario_tipo_creacion` | último token de un usuario (verificar / reenviar) |
| `idx_tokens_vigentes` (parcial) | tokens aún utilizables / limpieza |
| `idx_sesiones_usuario` | revocar o listar sesiones de un usuario |
| `idx_sesiones_activas` (parcial) | validación del middleware |

Cómo comprobar un índice en pgAdmin: Query Tool → escribe la consulta con `EXPLAIN ANALYZE` delante.
Con pocas filas (como las semillas) Postgres usa *Seq Scan* aunque exista el índice; **es normal**. Para ver el
índice en acción hay que probar con miles de filas.

---

## 6. Pendiente

- **Ubicación definitiva de los archivos en el repo:** falta la estructura de carpetas del equipo (monorepo,
  repos separados, carpeta del backend). Con esa estructura se ajustan las rutas de la sección 3.2.
- **Pruebas manuales en pgAdmin:** se agregan a esta guía una vez confirmadas.
- **Decisiones por confirmar:** hash del token (SHA-256), tabla única de tokens con `tipo`, nombre de la cookie (E08).
