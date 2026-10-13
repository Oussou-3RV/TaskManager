# TaskManager

Application web qui place automatiquement des tâches sur la meilleure machine disponible.

L'opérateur soumet une tâche, l'API vérifie son rôle, puis un moteur de décision choisit la machine parmi celles qui sont en service. Projet de synthèse du baccalauréat en informatique à l'UQTR.

## Fonctionnalités

- Soumission de tâches depuis un tableau de bord
- Placement automatique sur une machine en service
- Historique des décisions de placement
- Contrôle d'accès par rôle (OPÉRATEUR, ADMIN, AUDITEUR), vérifié côté serveur

## Architecture

Quatre services indépendants qui communiquent par requêtes réseau, déployés avec Docker Compose.

| Service | Technologie |
|---|---|
| Interface | Vue.js 3 |
| API et sécurité | Spring Boot (Java) |
| Moteur de décision | Python (Flask) |
| Base de données | PostgreSQL (port 5432) |

## Démarrage

Prérequis : Docker et Docker Compose.

| Service | Adresse |
|---|---|
| Interface | `http://localhost:<port>` |
| API | `http://localhost:<port>` |
| PostgreSQL | `localhost:5432` |

Si le port 5432 est déjà pris par un PostgreSQL local, l'arrêter ou changer le port exposé dans `docker-compose.yml`.

## Comptes de test

| Rôle | Utilisateur | Mot de passe |
|---|---|---|
| OPÉRATEUR | `operateur` | `password123` |
| ADMIN | `admin` | `password123` |
| AUDITEUR | `auditeur` | `password123` |



## Tests de charge

Les tests ont été faits avec k6 (10, 50 puis 100 utilisateurs simultanés) et les résultats visualisés dans Grafana.

Résultat principal : sous forte charge, c'est l'API (limite de threads) qui ralentit le plus, et non la base de données comme prévu au départ.


## Auteur

Ousmane Balde — UQTR
