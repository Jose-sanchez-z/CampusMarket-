#!/usr/bin/env bash
# Levanta la base DESDE CERO con un solo comando (criterio de S1-T13).
#   ./scripts/reset.sh            -> esquema limpio + migraciones
#   ./scripts/reset.sh --seed     -> además carga datos de prueba
#
# Conexión (por defecto la del docker-compose). Se puede sobreescribir con:
#   DATABASE_URL="postgresql://usuario:clave@host:5432/base" ./scripts/reset.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATABASE_URL="${DATABASE_URL:-postgresql://campus:campus@localhost:5432/campusmarket}"
PSQL=(psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -q)

cd "$ROOT"

echo ">> Borrando esquema public..."
"${PSQL[@]}" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

echo ">> Aplicando migraciones..."
for f in $(ls db/migration/V*.sql | sort -V); do
  echo "   - $(basename "$f")"
  "${PSQL[@]}" -f "$f"
done

if [[ "${1:-}" == "--seed" ]]; then
  echo ">> Cargando datos de prueba..."
  for f in $(ls db/seeds/*.sql | sort -V); do
    echo "   - $(basename "$f")"
    "${PSQL[@]}" -f "$f"
  done
fi

echo ">> Listo."
