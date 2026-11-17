#!/usr/bin/env bash
# Auditoría de integridad de la BD (S4-T13). Solo LEE; no modifica nada.
#   ./scripts/auditar.sh
# Sale con código 1 si hay hallazgos de severidad ERROR (útil para CI).
# Para auditar con datos de prueba:  ./scripts/reset.sh --seed && ./scripts/auditar.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATABASE_URL="${DATABASE_URL:-postgresql://campus:campus@localhost:5432/campusmarket}"

SALIDA="$(psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -q -f "$ROOT/db/audit/auditoria_integridad.sql")"

echo "SEVERIDAD | CHEQUEO | OBJETO | DETALLE"
echo "----------------------------------------"
echo "$SALIDA"
echo "----------------------------------------"

ERRORES=$(grep -c '^ERROR' <<<"$SALIDA" || true)
AVISOS=$(grep -c '^AVISO' <<<"$SALIDA" || true)
echo "Resumen: ${ERRORES} ERROR, ${AVISOS} AVISO"

if [[ "$ERRORES" -gt 0 ]]; then
  echo "AUDITORÍA NO SUPERADA: corregir los ERROR con una migración nueva."
  exit 1
fi
echo "AUDITORÍA SUPERADA (sin ERROR)."
