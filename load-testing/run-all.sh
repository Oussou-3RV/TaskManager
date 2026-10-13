#!/usr/bin/env bash
# Lance le protocole de charge complet : 10, 50 puis 100 clients concurrents,
# avec un reset de la base entre chaque palier pour une comparaison equitable.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/results"
mkdir -p "$RESULTS_DIR"

BASE_URL="${BASE_URL:-http://localhost:8081}"
ITERATIONS="${ITERATIONS:-100}"

for VUS in 10 50 100; do
    echo "=== Palier a $VUS clients concurrents ($ITERATIONS iterations) ==="
    "$SCRIPT_DIR/reset.sh"

    k6 run \
        --env BASE_URL="$BASE_URL" \
        --env VUS="$VUS" \
        --env ITERATIONS="$ITERATIONS" \
        --out "csv=$RESULTS_DIR/raw-$VUS.csv" \
        --summary-export "$RESULTS_DIR/summary-$VUS.json" \
        "$SCRIPT_DIR/submit-task.js"

    echo "=== Palier a $VUS clients termine ==="
    echo
done

python3 "$SCRIPT_DIR/generate_report.py"

echo "Tous les paliers sont termines. Resultats dans $RESULTS_DIR/"
