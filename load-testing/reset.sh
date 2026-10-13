#!/usr/bin/env bash
# Remet la base de test de charge dans son etat de depart : vide les taches/decisions/audit
# generees par le run precedent, et restaure le CPU/RAM des machines aux valeurs du seed.
set -euo pipefail

CONTAINER="taskmanager-loadtest-db"

docker exec "$CONTAINER" psql -U admin -d orchestration -v ON_ERROR_STOP=1 -c "
DELETE FROM placement_decisions;
DELETE FROM audit_events WHERE entity_type = 'task';
DELETE FROM tasks;

UPDATE machines SET cpu_available = 1,  memory_available_mb = 512   WHERE id = 'edge-01';
UPDATE machines SET cpu_available = 2,  memory_available_mb = 1024  WHERE id = 'edge-02';
UPDATE machines SET cpu_available = 4,  memory_available_mb = 2048  WHERE id = 'fog-01';
UPDATE machines SET cpu_available = 6,  memory_available_mb = 4096  WHERE id = 'fog-02';
UPDATE machines SET cpu_available = 12, memory_available_mb = 8192  WHERE id = 'cloud-01';
UPDATE machines SET cpu_available = 16, memory_available_mb = 16384 WHERE id = 'cloud-02';
"

echo "Base de test de charge reinitialisee."
