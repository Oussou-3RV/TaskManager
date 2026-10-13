# Analyse — décomposition de la latence par couche

## Ce qu'on a testé

L'action : soumettre une tâche (`POST /api/v1/tasks`). C'est l'action qui traverse
les 4 couches de l'application : le réseau, l'API (Spring Boot), le moteur de
placement (Python), et la base de données (Postgres).

On a mesuré cette action sous 3 niveaux de charge : **10, 50 et 100 clients qui
envoient des requêtes en même temps**. À chaque niveau, 100 requêtes au total ont
été envoyées, et la base a été remise à zéro entre chaque niveau pour comparer
équitablement.

## Notre hypothèse, avant de lancer le test

On s'attendait à ce que la **base de données** devienne le goulot d'étranglement
sous forte charge. La raison : chaque soumission de tâche déclenche environ 8
opérations en base (vérifier si la tâche existe déjà, chercher l'utilisateur,
créer la tâche, écrire un événement d'audit, lire la règle active, lire les
machines, sauvegarder la décision, mettre à jour le statut de la tâche et les
ressources de la machine). C'est beaucoup d'opérations pour une seule action, donc
on pensait que Postgres serait le premier à ralentir quand plusieurs clients
arrivent en même temps.

## Ce qu'on a mesuré

Chaque requête a été chronométrée séparément sur 4 couches, grâce à un identifiant
de trace qui suit la requête du navigateur jusqu'au moteur Python. Le tableau
montre la part de chaque couche dans le temps total (p99 = le pire cas sur 100
requêtes, en excluant le 1% le plus extrême).

| Couche | Part à 10 clients | Part à 50 clients | Part à 100 clients |
|---|---|---|---|
| API + sérialisation | 27,4 % | 50,3 % | 42,0 % |
| Base de données | 43,6 % | 5,8 % | 14,4 % |
| Réseau + frontend | 7,6 % | 35,4 % | 29,5 % |
| Logique de placement | 21,5 % | 8,4 % | 14,1 % |

(Les chiffres exacts bougent un peu à chaque exécution du test — c'est normal,
l'ordinateur fait autre chose en même temps. Le graphique en aires,
`results/latence-par-couche.svg`, montre la même chose visuellement.)

## Ce qu'on a trouvé (et pourquoi c'est surprenant)

**Notre hypothèse était fausse.** Ce n'est pas la base de données qui devient le
goulot sous charge — c'est l'inverse : sa part diminue quand la charge augmente
(43,6 % → 5,8 % → 14,4 %). La couche qui prend le plus de place, et qui reste
importante ou grossit avec la charge, c'est **l'API** (Spring Boot lui-même :
recevoir la requête, la valider, la transformer en objets Java, puis transformer
la réponse en JSON).

**Pourquoi ça a du sens en y réfléchissant** : Postgres est très bon pour gérer
beaucoup de petites requêtes rapides en parallèle, même sous charge — ce n'est
donc pas surprenant qu'il tienne bien le coup. Le vrai goulot semble être ailleurs :
le nombre de threads que Tomcat (le serveur web intégré à Spring Boot) peut traiter
en même temps est limité, donc quand 50 ou 100 clients arrivent en même temps,
les requêtes commencent à attendre leur tour avant même d'être traitées — ce
temps d'attente est compté dans la couche "API" parce qu'il fait partie du temps
total moins ce qu'on a mesuré précisément (base de données et moteur de
placement).

## Conclusion

Si on devait rendre cette application plus rapide sous forte charge, l'énoncé
demande d'identifier la couche prioritaire à optimiser en premier — et d'après ce
test, ce ne serait **pas** la base de données malgré le grand nombre d'opérations
qu'elle effectue par tâche. Ce serait plutôt du côté de l'API : augmenter le
nombre de threads disponibles dans Tomcat, ou rendre le traitement de la requête
plus léger, avant de penser à optimiser la base de données.

## Comment reproduire ce test

```bash
cd load-testing
docker compose -f docker-compose.loadtest.yml up -d   # une seule fois
./run-all.sh                                            # relance tout le protocole
```

Ça régénère les CSV bruts dans `results/*.csv`, le graphique
`results/latence-par-couche.svg`, et réaffiche le tableau ci-dessus dans le
terminal.
