#!/usr/bin/env bash
# Smoke test completo de BD: base desde cero + semillas + pruebas de restricciones.
#   ./scripts/smoke_test.sh
# Termina con código distinto de 0 si algo falla (útil para CI).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATABASE_URL="${DATABASE_URL:-postgresql://campus:campus@localhost:5432/campusmarket}"
export DATABASE_URL

echo "=== 1/3 Base desde cero + migraciones + semillas ==="
"$ROOT/scripts/reset.sh" --seed

echo
echo "=== 2/3 Verificación de datos sembrados ==="
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -q -At <<'SQL'
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM usuarios WHERE estado='PENDIENTE';
  IF n <> 3 THEN RAISE EXCEPTION 'se esperaban 3 usuarios PENDIENTE y hay %', n; END IF;
  SELECT count(*) INTO n FROM usuarios WHERE estado='ACTIVA';
  IF n <> 3 THEN RAISE EXCEPTION 'se esperaban 3 usuarios ACTIVA y hay %', n; END IF;
  SELECT count(*) INTO n FROM tokens WHERE NOT usado AND fecha_vencimiento > now();
  IF n < 1 THEN RAISE EXCEPTION 'no hay tokens vigentes'; END IF;
  SELECT count(*) INTO n FROM tokens WHERE NOT usado AND fecha_vencimiento <= now();
  IF n < 1 THEN RAISE EXCEPTION 'no hay tokens vencidos'; END IF;
  SELECT count(*) INTO n FROM tokens WHERE usado;
  IF n < 1 THEN RAISE EXCEPTION 'no hay tokens usados'; END IF;
  SELECT count(*) INTO n FROM sesiones WHERE revocada;
  IF n < 1 THEN RAISE EXCEPTION 'no hay sesiones revocadas'; END IF;
  SELECT count(*) INTO n FROM contactos WHERE es_publico;
  IF n < 1 THEN RAISE EXCEPTION 'no hay contactos públicos'; END IF;
  SELECT count(*) INTO n FROM contactos WHERE NOT es_publico;
  IF n < 1 THEN RAISE EXCEPTION 'no hay contactos privados'; END IF;
  SELECT count(*) INTO n FROM categorias WHERE activa;
  IF n < 6 THEN RAISE EXCEPTION 'se esperaban al menos 6 categorías activas y hay %', n; END IF;
  SELECT count(*) INTO n FROM categorias WHERE NOT activa;
  IF n < 1 THEN RAISE EXCEPTION 'no hay categorías inactivas'; END IF;
  SELECT count(*) INTO n FROM productos WHERE estado='DISPONIBLE';
  IF n < 1 THEN RAISE EXCEPTION 'no hay productos DISPONIBLE'; END IF;
  SELECT count(*) INTO n FROM productos WHERE estado='INACTIVO';
  IF n < 1 THEN RAISE EXCEPTION 'no hay productos INACTIVO'; END IF;
  SELECT count(*) INTO n FROM tokens_recuperacion WHERE NOT usado AND vencimiento > now();
  IF n < 1 THEN RAISE EXCEPTION 'no hay tokens de recuperación vigentes'; END IF;
  SELECT count(*) INTO n FROM tokens_recuperacion WHERE NOT usado AND vencimiento <= now();
  IF n < 1 THEN RAISE EXCEPTION 'no hay tokens de recuperación vencidos'; END IF;
  SELECT count(*) INTO n FROM tokens_recuperacion WHERE usado;
  IF n < 1 THEN RAISE EXCEPTION 'no hay tokens de recuperación usados'; END IF;
  RAISE NOTICE 'OK    semillas cubren Sprint 1 y Sprint 2 (contactos, categorías, productos, recuperación)';
END $$;
SQL

echo
echo "=== 3/3 Restricciones, flujo básico e integridad Sprint 2 ==="
correr_sql() {
  if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* ]]; then
    sed 's|\\o /dev/null|\\o NUL|g' "$1" | psql "$DATABASE_URL" -v ON_ERROR_STOP=1
  else
    psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$1"
  fi
}
correr_sql "$ROOT/db/tests/smoke_test.sql"
correr_sql "$ROOT/db/tests/integridad_recuperacion_s2.sql"
