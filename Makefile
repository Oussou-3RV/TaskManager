.PHONY: up down reset-db

up:
	docker compose up -d

down:
	docker compose down

# Remet la base de developpement a zero (comptes de demo, machines, regles —
# exactement l'etat des migrations Flyway). A lancer avant les tests de contrat
# ou les tests Playwright, qui tournent contre cette base et la polluent sinon
# avec des taches de test (contrairement aux tests unitaires/integration
# backend, qui utilisent une base jetable via Testcontainers et ne la touchent
# jamais).
reset-db:
	docker compose down -v
	docker compose up -d db placement backend
