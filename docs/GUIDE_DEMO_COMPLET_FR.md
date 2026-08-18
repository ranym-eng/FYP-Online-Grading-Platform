# Guide de démonstration réelle - FYP Online Grading Platform

Ce guide ne décrit que les fonctionnalités réellement présentes dans le projet. Il indique quoi ouvrir, quoi saisir, quoi modifier, quoi supprimer, quel résultat attendre et quoi expliquer à l'encadrant.

## 1. Le scénario métier en une phrase

L'administrateur prépare les données et les échéances, les évaluateurs enregistrent leurs notes en brouillon puis verrouillent leurs fiches, le système exclut les brouillons du calcul, l'administrateur consolide et publie les notes, et le coordinateur consulte les résultats et les rapports.

Les acteurs qui se connectent sont :

| Acteur | Rôle technique | Travail principal |
| --- | --- | --- |
| Administrateur | `ADMIN` | comptes, données, phases, import, prolongations, notes et rapports |
| Superviseur | `SUPERVISOR` | évaluation FYP I et FYP II des projets encadrés |
| Évaluateur académique | `FACULTY_EVALUATOR` | rapports et soutenances |
| Représentant industriel | `INDUSTRY_REPRESENTATIVE` | évaluation Demo Day |
| Coordinateur FYP | `COORDINATOR` | consultation des notes consolidées et rapports |

Un étudiant est une donnée académique rattachée à une équipe. Il ne possède pas de compte ni de dashboard.

## 2. Préparation obligatoire avant la démonstration

Ouvrir PowerShell puis exécuter :

```powershell
Set-Location "D:\Desktop\sultan qaboos\FYP-Online-Grading-Platform"
$env:FRONTEND_PORT = "3010"
docker compose up -d
docker compose ps
```

Les quatre services doivent afficher `healthy` ou `Up` :

| Service | Adresse |
| --- | --- |
| Frontend React | http://localhost:3010 |
| Backend Spring Boot | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Mailpit | http://localhost:8025 |
| Santé backend | http://localhost:8080/actuator/health |

Remettre les trois brouillons et les résultats à zéro juste avant la présentation :

```powershell
Get-Content -Raw .\backend\reset_demo_workflow.sql |
  docker compose exec -T postgres psql -U postgres -d fyp_grading_platform

Get-Content -Raw .\backend\seed_all_tables.sql |
  docker compose exec -T postgres psql -U postgres -d fyp_grading_platform
```

Ne pas relancer ce reset au milieu de la démonstration.

## 3. Comptes prêts à utiliser

| Acteur | E-mail | Mot de passe |
| --- | --- | --- |
| Administrateur | `admin@squ.edu.om` | `Admin@123` |
| Superviseur | `demo.supervisor@squ.edu.om` | `Test@123` |
| Évaluateur académique | `demo.faculty@squ.edu.om` | `Test@123` |
| Représentant industriel | `demo.industry@squ.edu.om` | `Test@123` |
| Coordinateur | `demo.coordinator@squ.edu.om` | `Test@123` |

Toujours cliquer sur Déconnexion avant de changer d'acteur. Cela prouve que chaque personne possède sa propre session et son propre espace.

## 4. Données préparées pour le cycle de notation

| Projet | Équipe | Étudiants | Fiche à terminer |
| --- | --- | --- | --- |
| `DEMO-PSE-01` Smart Grid Monitoring System | Team Smart Grid | Ali `20270001`, Maha `20270002` | superviseur FYP I, 21 notes sur 22 |
| `DEMO-CSP-02` AI Assisted FYP Grading Platform | Team Digital Assessment | Noor `20270003`, Salim `20270004` | rapport FYP II, 19 notes sur 20 |
| `DEMO-EIC-03` Intelligent Laboratory Energy Controller | Team Energy Lab | Aisha `20270005`, Omar `20270006` | Demo Day, 4 notes sur 5 |

| Phase | État prévu | Utilité |
| --- | --- | --- |
| FYP I - Demonstration Window | ouverte, environ 23 h restantes | évaluation superviseur et alerte 24 h |
| FYP II - Demo Day Window | ouverte, environ 11 h restantes | rapport, oral, industrie et alerte 12 h |
| FYP I - Expired Extension Example | ouverte mais expirée | blocage et demande de prolongation |

## 5. Démonstration complète, écran par écran

La durée complète est d'environ 40 à 50 minutes. Pour une présentation de 20 minutes, exécuter les étapes 5.1, 5.3, 5.6 à 5.12.

### 5.1 Connexion et séparation des rôles

1. Ouvrir http://localhost:3010.
2. Montrer le logo SQU, le choix français/anglais et le thème clair/sombre.
3. Saisir `admin@squ.edu.om` et `Admin@123`.
4. Cliquer sur Connexion.
5. Montrer le titre Espace administrateur et ses menus.
6. Ouvrir le profil puis le fermer.
7. Ouvrir la cloche de notifications.

Résultat attendu : l'utilisateur arrive directement sur le dashboard correspondant au rôle renvoyé par le backend.

Phrase à dire :

> Il n'existe aucun sélecteur de faux profil. Chaque acteur doit s'authentifier et le backend fournit son rôle, son identité et sa session.

### 5.2 Dashboard et navigation

Depuis le dashboard administrateur :

1. Montrer les cartes Utilisateurs, Projets, Évaluations et Rapports.
2. Montrer les raccourcis vers Gestion des données, Imports Excel, Évaluations, Prolongations, Notes, Rapports et Console API.
3. Utiliser la recherche globale avec `Smart Grid`.
4. Ouvrir Calendrier FYP et montrer les trois phases.
5. Réduire la fenêtre du navigateur pour montrer la navigation mobile.
6. Cliquer sur l'icône Actualiser.

Phrase à dire :

> Le dashboard synthétise les ressources réelles chargées par les API et adapte les actions au rôle connecté.

### 5.3 Import étudiant avec erreur puis import valide

Fichiers fournis :

- `docs/demo-data/etudiants_invalides.csv`
- `docs/demo-data/etudiants_valides.csv`

Test de validation sans insertion :

1. Ouvrir Imports Excel.
2. Rester sur l'onglet Étudiants.
3. Cliquer sur Choisir le fichier.
4. Sélectionner `etudiants_invalides.csv`.
5. Cliquer sur Analyser.
6. Montrer les quatre erreurs : identifiant non numérique, deux e-mails SQU incohérents et cohorte invalide.
7. Vérifier que le bouton d'import reste désactivé.

Phrase à dire :

> L'aperçu valide le fichier sans écrire dans PostgreSQL. Une seule erreur empêche l'import complet.

Import valide :

1. Choisir `etudiants_valides.csv`.
2. Cliquer sur Analyser.
3. Vérifier : 3 lignes, 3 valides, 0 à corriger.
4. Cliquer sur Créer ou mettre à jour les étudiants.
5. Ouvrir Gestion des données, puis Étudiants.
6. Rechercher `20999001`.

Résultat attendu : les trois fiches sont créées. Un second import identique doit les compter comme inchangées. Si le nom ou la cohorte est modifié dans le fichier, elles sont comptées comme mises à jour.

Colonnes officielles :

| Colonne | Exemple | Contrôle |
| --- | --- | --- |
| `stdID` | `20999001` | 5 à 12 chiffres |
| `cohort` | `2026` ou `26` | normalisée en quatre chiffres |
| `name` | `Sara Al Lawati` | obligatoire |
| `Email` | `s20999001@student.squ.edu.om` | doit correspondre au stdID |

### 5.4 CRUD administrateur avec des données temporaires

Ouvrir Gestion des données. Pour chaque ressource, la liste se trouve à gauche et le formulaire Create record à droite. Utiliser Edit pour modifier et Delete pour supprimer.

Ne jamais modifier ni supprimer les éléments dont le numéro commence par `DEMO-`.

#### A. Filière

Dans Tracks, créer :

| Champ | Valeur |
| --- | --- |
| Code | `LVD` |
| Name | `Live Demo Track` |
| Description | `Temporary track used during the presentation` |

Après création, cliquer Edit et remplacer Name par `Live Demonstration Track`, puis Update.

#### B. Étudiant manuel

L'import précédent suffit pour montrer la création en masse. Pour montrer la modification, rechercher `20999001`, cliquer Edit, remplacer le nom par `Sara Al Lawati Updated`, puis Update.

Le backend vérifie que l'e-mail reste exactement `s20999001@student.squ.edu.om`.

#### C. Projet

Dans Projets, créer :

| Champ | Valeur |
| --- | --- |
| Numéro de projet | `DEMO-LIVE-99` |
| Titre | `Smart Campus Live Demonstration` |
| Résumé | `Energy monitoring dashboard for university laboratories.` |
| Année académique | `2026-2027` |
| Filière | `Live Demonstration Track` |
| État | `ACTIVE` |

Cliquer Edit et remplacer le titre par `Smart Campus Energy Dashboard`.

#### D. Équipe

Dans Teams, créer :

| Champ | Valeur |
| --- | --- |
| Team name | `Live Demo Team` |
| Section | `S99` |
| Academic year | `2026-2027` |
| Project | `DEMO-LIVE-99` |
| Student IDs | Sara `20999001` et Yousuf `20999002` |

Cliquer Edit et remplacer la section par `S100`.

#### E. Compte évaluateur

Dans Users, créer :

| Champ | Valeur |
| --- | --- |
| University ID | `LIVE-EVAL-99` |
| Full name | `Dr Nasser Al Demo` |
| Email | `demo.live@squ.edu.om` |
| Phone | `+968 9999 0099` |
| Password | `Demo@123` |
| Role | `FACULTY_EVALUATOR` |

Cliquer Edit, remplacer le téléphone par `+968 9999 0199`, laisser le mot de passe vide, puis Update.

#### F. Profil évaluateur

Dans Evaluators, créer :

| Champ | Valeur |
| --- | --- |
| Evaluator user | `Dr Nasser Al Demo` |
| Department | `Electrical and Computer Engineering` |
| Specialization | `Artificial Intelligence` |
| External organization | vide |
| External | décoché |

Modifier ensuite Specialization en `AI and Data Science`.

#### G. Phase

Dans Phases, créer :

| Champ | Valeur |
| --- | --- |
| Phase type | `PHASE_I` |
| Name | `Live CRUD Phase` |
| Academic year | `2026-2027` |
| Start date | date et heure actuelles moins une heure |
| Deadline | date actuelle plus sept jours à 17:00 |
| Status | `NOT_STARTED` |

Modifier ensuite Status en `OPEN`. Le backend refuse une deadline antérieure ou égale à la date de début.

#### H. Modèle de formulaire

Dans Evaluation forms, créer :

| Champ | Valeur |
| --- | --- |
| Form name | `Live Demo Form` |
| Evaluation type | `ORAL_PHASE_I` |
| Phase type | `PHASE_I` |
| Description | `Temporary oral evaluation template` |
| Total weight | `100` |

Modifier le nom en `Live Demo Form Updated`.

#### I. Ressources en lecture seule

Dans Reports et Audit logs, montrer que la liste est consultable mais qu'aucun formulaire de création manuelle n'est proposé.

### 5.5 Affectations et critères dans Swagger

Ces opérations existent dans le backend mais n'ont pas encore d'écran React spécialisé. Les montrer dans Swagger uniquement si l'encadrant demande la partie avancée.

Authentification Swagger :

1. Exécuter `POST /api/auth/login` avec :

```json
{
  "email": "admin@squ.edu.om",
  "password": "Admin@123"
}
```

2. Copier `data.token`.
3. Cliquer Authorize.
4. Saisir `Bearer VOTRE_TOKEN`.

Affecter l'évaluateur au projet :

1. Exécuter `GET /api/projects/search?keyword=DEMO-LIVE-99` et copier l'UUID du projet.
2. Exécuter `GET /api/evaluators` et trouver le profil dont l'utilisateur est `demo.live@squ.edu.om`.
3. Exécuter `POST /api/projects/{projectId}/evaluators` :

```json
{
  "evaluatorId": "UUID_DU_PROFIL_EVALUATEUR",
  "evaluationType": "ORAL_PHASE_I"
}
```

4. Vérifier avec `GET /api/projects/{projectId}/evaluators`.
5. Conserver l'UUID de l'affectation pour la supprimer à la fin.

Ajouter un critère au modèle :

1. Copier l'UUID de `Live Demo Form Updated` avec `GET /api/evaluation-forms`.
2. Exécuter `POST /api/evaluation-forms/{formId}/criteria` :

```json
{
  "title": "Technical clarity",
  "description": "Clarity of the technical explanation",
  "maxScore": 10,
  "weight": 2,
  "displayOrder": 1,
  "required": true
}
```

3. Vérifier avec `GET /api/evaluation-forms/{formId}/criteria`.

Phrase à dire :

> Le backend possède les affectations et la configuration des critères. L'interface React spécialisée pour ces deux opérations reste un écran à ajouter.

### 5.6 Superviseur : brouillon, autosauvegarde et verrouillage

1. Se déconnecter de l'administrateur.
2. Se connecter avec `demo.supervisor@squ.edu.om` et `Test@123`.
3. Ouvrir Évaluations.
4. Sélectionner le projet `DEMO-PSE-01`.
5. Sélectionner la phase FYP I - Demonstration Window.
6. Sélectionner le type `SUPERVISOR_PHASE_I`.
7. Sélectionner le profil de Dr Ahmed Al Balushi si le champ est affiché.
8. Vérifier que 21 notes sur 22 sont déjà remplies.
9. Pour Maha, saisir `8` dans le dernier critère Proposal submitted by deadline.
10. Remplacer le commentaire général par `Good progress. Objectives are clear and the proposal was submitted on time.`
11. Attendre deux secondes.
12. Montrer le message de sauvegarde du brouillon.
13. Recharger la page et rouvrir la même fiche : la valeur doit rester présente.
14. Cliquer Valider la fiche.
15. Confirmer la validation.
16. Montrer le statut `LOCKED`, la date de soumission et les champs désactivés.

Résultat attendu : note de fiche proche de `7.865 / 10`.

Phrase à dire :

> Chaque saisie est autosauvegardée comme brouillon dans PostgreSQL. Seul le bouton Valider la fiche transforme la soumission en fiche verrouillée et utilisable pour le calcul.

### 5.7 Évaluateur académique : rapport FYP II

1. Se déconnecter.
2. Se connecter avec `demo.faculty@squ.edu.om` et `Test@123`.
3. Ouvrir Évaluations.
4. Sélectionner `DEMO-CSP-02`.
5. Sélectionner FYP II - Demo Day Window.
6. Sélectionner `REPORT_PHASE_II`.
7. Vérifier que 19 notes sur 20 sont déjà remplies.
8. Pour Salim, saisir `8` dans Complete the proposed work.
9. Saisir le commentaire `The report is complete, technically sound and clearly structured.`
10. Attendre l'autosauvegarde.
11. Cliquer Valider la fiche.
12. Montrer la fiche verrouillée et l'historique des évaluations du projet.

Résultat attendu : note de fiche `8.09 / 10`.

Les autres types académiques réellement gérés sont `REPORT_PHASE_I`, `ORAL_PHASE_I` et `ORAL_PHASE_II`.

### 5.8 Représentant industriel : Demo Day

1. Se déconnecter.
2. Se connecter avec `demo.industry@squ.edu.om` et `Test@123`.
3. Ouvrir Évaluations.
4. Sélectionner `DEMO-EIC-03`.
5. Sélectionner FYP II - Demo Day Window.
6. Sélectionner `DEMO_DAY_INDUSTRY`.
7. Vérifier que quatre critères sur cinq sont remplis.
8. Saisir `9` pour Poster.
9. Saisir `Strong prototype, clear answers and a professional poster.` comme commentaire.
10. Attendre l'autosauvegarde.
11. Cliquer Valider la fiche.
12. Montrer le verrouillage.

Calcul attendu :

```text
(2 × prototype + présentation + 4 × questions + 2 × travail + poster) / 10
= (2×9 + 8 + 4×9 + 2×8 + 9) / 10
= 8.70 / 10
```

### 5.9 Échéance dépassée et demande de prolongation

Rester connecté avec un évaluateur, par exemple le superviseur.

1. Ouvrir Calendrier FYP et montrer FYP I - Expired Extension Example.
2. Ouvrir Prolongations.
3. Choisir cette phase expirée.
4. Choisir comme nouvelle échéance demain à 17:00.
5. Saisir :

```text
The university network was unavailable during the final submission period.
Please extend my personal evaluation deadline until tomorrow at 17:00.
```

6. Cliquer Envoyer la demande.
7. Montrer le statut `PENDING`.
8. Ouvrir Mailpit à http://localhost:8025 et montrer l'e-mail envoyé à l'administrateur.
9. Se déconnecter et revenir comme administrateur.
10. Ouvrir Prolongations.
11. Choisir une nouvelle échéance demain à 17:00.
12. Saisir le commentaire `Approved for this evaluator only.`.
13. Cliquer Approve.
14. Montrer le statut `APPROVED`.
15. Revenir avec le superviseur et montrer la notification de décision.

Phrase à dire :

> Une prolongation approuvée crée une échéance personnelle. Elle ne prolonge pas automatiquement la phase pour tous les acteurs.

Pour montrer le rejet, créer une deuxième demande et utiliser :

```text
Rejected because the reason does not justify an exceptional extension.
```

### 5.10 Notifications 24 h et 12 h

Se connecter comme administrateur puis ouvrir Console API :

| Champ | Valeur |
| --- | --- |
| Method | `POST` |
| Path | `/api/notifications/reminders/evaluation-deadline` |
| Body | vide |

Cliquer Exécuter.

Ensuite :

1. Ouvrir la cloche.
2. Montrer l'alerte de phase proche de 24 h et l'alerte urgente proche de 12 h.
3. Ouvrir une notification.
4. Cliquer Marquer comme lue.
5. Cliquer Tout marquer comme lu.
6. Se connecter avec un autre acteur et montrer sa notification personnelle.

Le planificateur backend exécute aussi ce contrôle automatiquement toutes les 15 minutes et utilise une clé de déduplication.

### 5.11 Consolidation et publication des notes

Cette étape doit être faite après les trois validations précédentes.

Se connecter comme administrateur, ouvrir Notes consolidées, puis calculer :

| Projet | Phase | Résultat attendu |
| --- | --- | ---: |
| `DEMO-PSE-01` | FYP I - Demonstration Window | `8.141` |
| `DEMO-CSP-02` | FYP II - Demo Day Window | `8.5825` |
| `DEMO-EIC-03` | FYP II - Demo Day Window | `8.405` |

Pour chaque ligne :

1. Choisir le projet.
2. Choisir la phase.
3. Cliquer Calculate grade.
4. Vérifier `published = false`.
5. Cliquer Publish.
6. Vérifier `published = true`.

Règles réellement utilisées :

| Phase | Type | Poids |
| --- | --- | ---: |
| FYP I | superviseur | 40 % |
| FYP I | rapport | 35 % |
| FYP I | oral | 25 % |
| FYP II | superviseur | 30 % |
| FYP II | rapport | 25 % |
| FYP II | oral | 25 % |
| FYP II | Demo Day | 20 % |

Phrase à dire :

> Le calcul sélectionne uniquement les soumissions LOCKED. Un brouillon, même complet, n'est jamais inclus.

### 5.12 Coordinateur FYP

1. Se déconnecter.
2. Se connecter avec `demo.coordinator@squ.edu.om` et `Test@123`.
3. Montrer son dashboard distinct.
4. Ouvrir Notes consolidées.
5. Montrer les notes publiées.
6. Ouvrir Rapports.
7. Montrer l'archive et les statuts.
8. Montrer que le coordinateur n'a pas le menu Évaluations.

Phrase à dire :

> Le coordinateur consulte les résultats institutionnels mais ne remplit pas les fiches d'évaluation.

### 5.13 Rapports et e-mail

Se connecter comme administrateur puis ouvrir Rapports.

1. Sélectionner `DEMO-PSE-01` et la phase FYP I.
2. Cliquer Generate phase report.
3. Cliquer Generate final report.
4. Montrer les nouvelles lignes dans Report archive.
5. Cliquer Send sur une ligne.
6. Montrer le passage du statut à `SENT`.

Ce que cette fonction fait réellement aujourd'hui :

- elle crée un enregistrement de rapport avec un snapshot textuel ;
- elle trace le statut, le destinataire et les dates ;
- le bouton Send marque et trace l'envoi dans la base.

Elle ne génère pas encore un fichier PDF physique et le bouton Send du rapport n'envoie pas encore une pièce jointe SMTP réelle.

Pour démontrer un véritable e-mail SMTP :

1. Ouvrir Gestion des données, puis Notifications.
2. Remplir :

| Champ | Valeur |
| --- | --- |
| Recipient | `coordinator.demo@squ.edu.om` |
| Subject | `FYP grading demonstration completed` |
| Body | `The three demo projects have been evaluated and consolidated successfully.` |
| Attachment path | vide |

3. Cliquer Create.
4. Ouvrir http://localhost:8025.
5. Montrer l'e-mail reçu dans Mailpit.

### 5.14 Audit et Console API

Dans Gestion des données, ouvrir Audit logs et montrer les actions d'import et les événements tracés.

Dans Console API, exécuter :

| Method | Path | Résultat |
| --- | --- | --- |
| GET | `/api/projects` | liste des projets |
| GET | `/api/phases/current` | phases ouvertes |
| GET | `/api/students/search?keyword=Sara` | recherche étudiant |
| GET | `/api/projects/search?keyword=Smart` | recherche projet |

Phrase à dire :

> Swagger et la console intégrée permettent de tester le contrat REST indépendamment des écrans React.

## 6. Scénarios d'erreur à montrer

| Action | Test | Résultat attendu |
| --- | --- | --- |
| Mauvais login | mot de passe `wrong` | erreur 400, aucune session |
| Import invalide | fichier `etudiants_invalides.csv` | aucune insertion |
| Étudiant dupliqué | recréer `20999001` | identifiant déjà existant |
| E-mail étudiant incorrect | `s999@student.squ.edu.om` pour `20999001` | e-mail SQU attendu refusé |
| Projet dupliqué | recréer `DEMO-LIVE-99` | numéro de projet déjà existant |
| Phase invalide | deadline avant start date | dates refusées |
| Fiche incomplète | cliquer Valider avant la dernière note | validation refusée |
| Phase expirée | ouvrir l'accès d'évaluation expiré | accès interdit |
| Type non affecté | évaluateur sur un autre type | affectation refusée |
| Fiche verrouillée | essayer de modifier après validation | champs bloqués et backend refuse |
| Calcul trop tôt | laisser une fiche en brouillon | brouillon exclu ou données insuffisantes |
| Accès de rôle | coordinateur vers Évaluations | menu absent |

## 7. Suppression propre des données temporaires

Effectuer le nettoyage dans cet ordre pour respecter les relations :

1. Dans Swagger, supprimer l'affectation temporaire avec `DELETE /api/projects/{projectId}/evaluators/{assignmentId}`.
2. Dans Swagger, supprimer le critère avec `DELETE /api/criteria/{criterionId}`.
3. Dans Gestion des données, supprimer `Live Demo Form Updated`.
4. Supprimer `Live CRUD Phase`.
5. Supprimer `Live Demo Team`.
6. Supprimer `DEMO-LIVE-99`.
7. Supprimer les étudiants `20999001`, `20999002` et `20999003`.
8. Supprimer le profil Evaluator de `demo.live@squ.edu.om`.
9. Supprimer le compte `demo.live@squ.edu.om`.
10. Supprimer la filière `LVD`.

Ne jamais supprimer un parent avant ses enfants : projet avant équipe, utilisateur avant profil évaluateur, ou formulaire avant critère.

Pour remettre uniquement le workflow des trois projets préparés à son état initial, relancer les deux scripts de la section 2.

## 8. Fonctionnalités réelles par acteur

| Fonctionnalité | Admin | Superviseur | Faculty | Industrie | Coordinateur |
| --- | :---: | :---: | :---: | :---: | :---: |
| Connexion, déconnexion, profil | oui | oui | oui | oui | oui |
| Dashboard propre au rôle | oui | oui | oui | oui | oui |
| Calendrier des phases | oui | oui | oui | oui | oui |
| Notifications personnelles | oui | oui | oui | oui | oui |
| CRUD données académiques | oui | non | non | non | non |
| Import officiel étudiants | oui | non | non | non | non |
| Évaluation superviseur | contrôle | oui | non | non | non |
| Rapport et oral | contrôle | non | oui | non | non |
| Demo Day | contrôle | non | non | oui | non |
| Brouillon et autosauvegarde | contrôle | oui | oui | oui | non |
| Validation et verrouillage | contrôle | oui | oui | oui | non |
| Demande de prolongation | décision | oui | oui | oui | non |
| Calcul et publication | oui | non | non | non | consultation |
| Rapports | gestion | non | non | non | consultation |
| Swagger et Console API | oui | non | non | non | non |

## 9. Ce qui est implémenté et ce qui reste à faire

### Implémenté et démontrable

- authentification et redirection selon cinq rôles ;
- sessions séparées, déconnexion, dashboards et menus par rôle ;
- CRUD utilisateurs, étudiants, évaluateurs, filières, projets, équipes, phases et modèles ;
- import CSV/XLSX officiel des étudiants avec aperçu et validation ;
- affectation superviseur et évaluateurs par API ;
- sept types de grilles d'évaluation ;
- autosauvegarde des brouillons dans PostgreSQL ;
- complétude, validation, verrouillage et historique ;
- contrôle de phase, deadline et affectation ;
- demandes de prolongation, approbation, rejet et échéance personnelle ;
- notifications personnelles, lecture, lecture globale et alertes 24 h/12 h ;
- envoi SMTP testable dans Mailpit ;
- calcul pondéré à partir des fiches verrouillées ;
- publication et consultation coordinateur ;
- enregistrements de rapports et journal d'audit partiel ;
- Swagger, Actuator, Docker Compose et PostgreSQL.

### À ne pas présenter comme terminé

- l'import Professeurs est visible dans React mais l'endpoint backend correspondant n'existe pas ;
- les écrans React dédiés aux affectations et aux critères restent à créer ;
- les grilles visibles dans l'écran de notation sont encore définies dans le frontend et ne sont pas entièrement construites depuis les critères dynamiques du backend ;
- le rapport est un enregistrement et un snapshot textuel, pas encore un PDF téléchargeable ;
- l'envoi d'un rapport avec pièce jointe réelle n'est pas terminé ;
- la sécurité HTTP globale doit être renforcée ; plusieurs contrôleurs reposent encore sur des contrôles manuels ;
- le jeton actuel n'est pas un JWT signé de production ;
- changement de mot de passe, mot de passe oublié, refresh token et `/me` restent incomplets ;
- le projet est actuellement un monolithe modulaire déployé dans un conteneur backend, pas plusieurs microservices indépendants ;
- l'audit n'enregistre pas encore toutes les actions ;
- la configuration Flyway et les migrations versionnées restent à finaliser.

## 10. Script oral très court

> La plateforme gère tout le cycle d'évaluation FYP. L'administrateur importe les étudiants, crée les projets, équipes, acteurs et phases, puis affecte les évaluateurs. Chaque acteur se connecte avec sa propre session. Les notes sont autosauvegardées comme brouillon, mais elles ne comptent qu'après validation et verrouillage. Les deadlines sont contrôlées côté backend ; après expiration, l'évaluateur peut demander une prolongation personnelle. Les notifications préviennent à 24 heures et 12 heures. Enfin, l'administrateur consolide uniquement les fiches verrouillées, publie les notes, et le coordinateur consulte les résultats et les rapports.

## 11. Vérification technique effectuée pour ce guide

Un cycle temporaire complet a été exécuté sur l'application locale :

- création, modification, activation et suppression d'un utilisateur ;
- création, modification et suppression d'un profil évaluateur ;
- création, modification, activation et suppression d'une filière ;
- création, modification et suppression d'un étudiant ;
- création, modification, changement de statut et suppression d'un projet ;
- création, modification, relation étudiant-équipe et suppression d'une équipe ;
- création, ouverture, fermeture, archivage, modification et suppression d'une phase ;
- création, modification, activation et suppression d'un modèle ;
- création, modification, réordonnancement et suppression d'un critère.

Toutes les données temporaires de ce contrôle ont été supprimées. Les données `DEMO-*` prévues pour la présentation sont restées intactes.

