# TaskManager — Console d'orchestration de bout en bout

Console permettant à un opérateur de soumettre des tâches, de choisir une politique
de placement, de suivre les décisions en direct et de consulter l'historique des
opérations. Projet réalisé dans le cadre du cours "Console d'orchestration de bout
en bout".

## Architecture

| Service | Techno | Rôle |
|---|---|---|
| `frontend` | Vue.js 3 + Vite | Interface opérateur |
| `backend` | Spring Boot (Java 21) | API REST, authentification JWT, persistance |
| `placement` | Python (Flask) | Moteur de décision de placement (fourni) |
| `db` | PostgreSQL 16 | Persistance (migrations Flyway) |

Le backend appelle le service `placement` pour chaque tâche soumise, puis persiste
la décision. Toutes les requêtes significatives portent un identifiant de trace
propagé entre les 4 couches (voir [Traçage et test de charge](#traçage-et-test-de-charge)).

## Prérequis

- Docker et Docker Compose
- Pour le développement local sans Docker : Java 21, Node.js ≥ 20, Python 3.12

## Installation et exécution

```bash
git clone <url-du-depot>
cd TaskManager
cp .env.example .env
docker compose up
```

Une seule commande démarre toute la stack (base de données, migrations,
backend, moteur de placement, frontend). Une fois les services prêts :

- Frontend : http://localhost:5173
- API backend : http://localhost:8080
- Santé du backend : http://localhost:8080/actuator/health
- Moteur de placement : http://localhost:8000/health

### Comptes de démonstration

Créés automatiquement par les données d'amorçage (migration `V2__seed.sql`).
Même mot de passe pour les trois :

| Utilisateur | Mot de passe | Rôle |
|---|---|---|
| `admin` | `password123` | ADMIN |
| `operateur` | `password123` | OPERATOR |
| `auditeur` | `password123` | AUDITOR |

## Configuration

Toute la configuration passe par des variables d'environnement (fichier `.env`,
voir `.env.example`) — aucun secret n'est codé en dur dans le code applicatif.

| Variable | Rôle |
|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Base de données |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Connexion du backend à la base |
| `JWT_SECRET` | Clé de signature des jetons JWT |
| `PLACEMENT_SERVICE_URL` | URL du moteur de placement, appelée par le backend |

## Développement local (sans Docker)

```bash
# Base de donnees seule
docker compose up -d db

# Backend (port 8080)
cd backend && mvn spring-boot:run

# Frontend (port 5173)
cd frontend && npm install && npm run dev

# Moteur de placement (port 8000)
cd placement-service && pip install -r requirements.txt && python app.py
```

## Traçage et test de charge

Le dossier [`load-testing/`](load-testing/) contient le protocole expérimental de
décomposition de la latence par couche (réseau, API, logique de placement, base
de données), sur une stack Docker isolée dédiée (jamais la base de développement).

```bash
cd load-testing
docker compose -f docker-compose.loadtest.yml up -d
./run-all.sh
```

Résultats et analyse : voir [`load-testing/ANALYSE.md`](load-testing/ANALYSE.md).

## Structure du dépôt

```
backend/            API Spring Boot, migrations Flyway, JWT
frontend/           Interface Vue.js
placement-service/  Moteur de placement Python (fourni par l'énoncé)
load-testing/       Protocole de charge k6 + traçage par couche + analyse
```
