#!/usr/bin/env bash
# Tests de contrat : verifie que les reponses reelles de l'API respectent
# le fichier openapi.yaml a la racine du depot, avec l'outil Schemathesis.
#
# Prerequis : la stack doit deja tourner (docker compose up -d db placement backend)
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -d contract-tests/.venv ]; then
  python3 -m venv contract-tests/.venv
fi
contract-tests/.venv/bin/pip install --quiet --upgrade pip schemathesis

echo "Connexion en tant qu'operateur pour obtenir un jeton..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"operateur","password":"password123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

echo "Lancement de Schemathesis contre openapi.yaml..."
contract-tests/.venv/bin/schemathesis run openapi.yaml \
  --url http://localhost:8080 \
  -H "Authorization: Bearer $TOKEN" \
  --exclude-checks unsupported_method,negative_data_rejection
