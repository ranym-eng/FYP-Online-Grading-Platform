# Rapport d'implementation frontend

## Projet

- Application: SQU Online FYP Grading Platform
- Frontend: React + Vite
- Backend cible: Spring Boot microservices-ready expose sous `http://localhost:8080`
- Port frontend de developpement: `http://127.0.0.1:5173`

## Objectif realise

Le frontend basique a ete transforme en interface complete synchronisee avec le backend Spring Boot. L'application couvre les acteurs du cahier de specification: administrateur, etudiant, superviseur, evaluateur faculty, representant industrie et coordinateur.

## Fichiers principaux modifies

- `src/App.jsx`: application SPA complete, navigation, authentification, dashboards, CRUD, evaluations, grades, rapports et console API.
- `src/App.css`: design moderne responsive avec ecran login/signup, sidebar, cards, tables, formulaires, badges et panels dynamiques.
- `src/index.css`: styles globaux propres pour Vite/React.
- `src/api.js`: client API centralise vers le backend.
- `src/config.js`: configuration des roles, vues, templates acteurs et ressources CRUD.

## Fonctionnalites implementees

### Authentification

- Ecran login interactif avec logo Sultan Qaboos University.
- Ecran sign up avec selection du role.
- Mode demo quand l'API auth n'est pas disponible.
- Conservation de la session dans `localStorage`.
- Changement rapide d'acteur pour tester les differents templates.

### Templates par acteur

Chaque acteur possede son espace fonctionnel:

- Admin: gestion utilisateurs, tracks, projets, equipes, templates, rapports, audit.
- Student: projet, equipe, phases, evaluations, rapports.
- Supervisor: evaluations superviseur, phases, submissions, grades.
- Faculty Evaluator: rapport, oral, criteres, validations.
- Industry Representative: demo day, notation industrie, feedback.
- Coordinator: affectations, validation, rapports, audit et supervision globale.

### Dashboard

- Statistiques rapides synchronisees avec l'API.
- Suivi des phases I et II.
- Progression des workflows.
- Actions rapides selon le role.
- Vue des projets recents.

### CRUD

CRUD generique configure pour:

- Utilisateurs
- Profils etudiants
- Profils evaluateurs
- Tracks
- Projets
- Equipes
- Phases
- Templates de formulaires
- Rapports
- Notifications email
- Audit logs

Chaque ressource contient:

- Liste des donnees via GET
- Creation via POST
- Modification via PUT
- Suppression via DELETE
- Formulaire dynamique
- Recherche
- Import JSON
- Export JSON
- Refresh API

### Evaluations

- Studio d'evaluation par phase et type.
- Chargement des projets, phases, templates et submissions.
- Creation de submissions avec feedback, score et statut.
- Sauvegarde vers `/api/evaluation-submissions`.

### Grades

- Centre de calcul des notes.
- Selection projet + phase.
- Declenchement du calcul via `/api/grades/calculate`.
- Consultation des grades depuis `/api/grades`.

### Rapports

- Generation de rapport par projet et type.
- Types: summary, grade, audit, evaluation.
- Consultation des rapports existants.

### Console API

- Outil de test integre pour appeler manuellement les endpoints REST.
- Support GET, POST, PUT, DELETE.
- Corps JSON libre pour les tests avances.

## Synchronisation backend

Le frontend utilise `VITE_API_BASE_URL` si la variable existe, sinon il pointe automatiquement vers:

```bash
http://localhost:8080
```

Exemple pour changer l'URL:

```bash
$env:VITE_API_BASE_URL="http://localhost:8080"
npm run dev
```

## Verification effectuee

- `npm run build`: succes.
- Frontend local: `http://127.0.0.1:5173` repond avec HTTP 200.
- Backend API: `http://localhost:8080/api/tracks` repond avec HTTP 200.
- Backend detecte sur le port 8080.
- Frontend lance sur le port 5173.

## Commandes utiles

Installer les dependances:

```bash
npm install
```

Demarrer le frontend:

```bash
npm run dev -- --host 127.0.0.1
```

Compiler:

```bash
npm run build
```

Tester l'API backend:

```bash
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/tracks
```

## Donnees de test backend

Les comptes seed crees cote backend utilisent:

- Mot de passe: `Test@123`
- Exemples:
  - `seed.student1@squ.edu.om`
  - `seed.supervisor1@squ.edu.om`
  - `seed.faculty.report@squ.edu.om`
  - `seed.industry@squ.edu.om`
  - `seed.coordinator@squ.edu.om`

## Resultat

Le projet React n'est plus un starter basique. Il contient maintenant une interface fonctionnelle, moderne et orientee metier pour piloter toute la plateforme FYP: roles, CRUD, evaluation, notation, rapports, audit et integration API Spring Boot.
