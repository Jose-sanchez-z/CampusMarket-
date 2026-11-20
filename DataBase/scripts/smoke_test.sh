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
  RAISE NOTICE 'OK    semillas cubren pendientes/activos y tokens vigentes/vencidos/usados';
END $$;
SQL

echo
echo "=== 3/3 Restricciones y flujo básico ==="
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$ROOT/db/tests/smoke_test.sql"
