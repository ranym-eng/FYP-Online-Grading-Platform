# Initialisation annuelle des donnees FYP

## 1. Objectif

La plateforme remplace les classeurs de notation EIC, CSN, CSP et PSE ainsi que la consolidation MATLAB. Les anciennes fiches de notes ne sont pas importees. Elles deviennent des formulaires en ligne avec brouillon, validation, verrouillage, moyenne et export final.

L'administrateur importe d'abord un seul classeur de donnees de reference :

`modele_initialisation_plateforme_fyp.xlsx`

Les etudiants restent des donnees academiques. Ils ne possedent aucun compte et ne sont pas un acteur de connexion.

## 2. Les sept feuilles obligatoires

| Feuille | Colonnes principales | Utilisation |
|---|---|---|
| `STUDENTS` | `studentId`, `studentName`, `email`, `cohort`, `trackCode`, `level` | Referentiel officiel des etudiants |
| `ADMINISTRATORS` | `actorId`, `actorName`, `email`, `phone`, `authenticationMode`, `status` | Identites administrateur SQU SSO |
| `COORDINATORS` | memes colonnes acteur | Identites coordinateur FYP SQU SSO |
| `SUPERVISORS` | colonnes acteur + `department`, `specialization` | Encadrants SQU SSO |
| `FACULTY_EVALUATORS` | colonnes acteur + profil academique | Evaluateurs rapport/oral SQU SSO |
| `INDUSTRY_GUESTS` | colonnes acteur + `organization`, `accessExpiresAt`, `status` | Invites externes Demo Day |
| `PROJECT_ASSIGNMENTS` | projet, etudiant, superviseur et e-mails evaluateurs | Toutes les relations necessaires |

Les codes de filiere autorises sont `EIC`, `CSN`, `CSP` et `PSE`. Ils sont crees automatiquement au premier demarrage.

## 3. Feuille PROJECT_ASSIGNMENTS

Colonnes attendues :

`cohort`, `trackCode`, `projectNumber`, `projectTitle`, `projectAbstract`, `section`, `studentId`, `studentName`, `supervisorId`, `supervisorName`, `reportPhaseIEvaluatorEmails`, `oralPhaseIEvaluatorEmails`, `reportPhaseIIEvaluatorEmails`, `oralPhaseIIEvaluatorEmails`, `industryGuestEmails`.

Toutes les lignes pre-remplies du modele sont fictives : les noms commencent par `Example`, les identifiants acteur par `DEMO`, les e-mails acteur utilisent le domaine reserve `example.com` et les identifiants etudiant utilisent la serie d'exemple `99000001...` avec le format d'e-mail SQU obligatoire. Elles doivent etre remplacees ou supprimees avant un import reel. Les exemples couvrent les quatre filieres, 1 a 5 etudiants, 1 ou 2 superviseurs, une ligne supplementaire sans etudiant, un ou plusieurs evaluateurs academiques et un ou deux Industry Guests.

Repeter le meme `projectNumber` sur chaque ligne du projet. Une ligne peut contenir un etudiant, un superviseur ou les deux. Pour un projet avec un seul etudiant et deux superviseurs, ajouter une deuxieme ligne avec `studentId` et `studentName` vides pour renseigner le deuxieme superviseur. Ne jamais dupliquer un identifiant etudiant.

Pour un meme projet, les regles sont :

- 1 a 5 etudiants distincts maximum ;
- 1 a 2 superviseurs distincts maximum ;
- un etudiant ne peut appartenir qu'a un projet pour la meme cohorte ;
- les e-mails rapport/oral doivent exister dans `FACULTY_EVALUATORS` ;
- les e-mails Industry Guest doivent exister dans `INDUSTRY_GUESTS` ;
- un Industry Guest recoit uniquement la fiche `DEMO_DAY_INDUSTRY` ;
- chaque superviseur recoit les fiches superviseur FYP I et FYP II du projet.

Les listes d'e-mails acceptent la virgule ou le point-virgule. `projectNumber` doit etre repete sur chaque ligne ; les autres metadonnees projet peuvent etre remplies sur la premiere ligne puis laissees vides sur les lignes suivantes.

## 4. Parcours exact de l'administrateur

1. Se connecter avec le compte administrateur.
2. Ouvrir `Imports Excel` puis `Initialisation annuelle`.
3. Telecharger le modele officiel.
4. Completer les sept feuilles sans renommer les en-tetes.
5. Cliquer sur `Analyser sans enregistrer`.
6. Corriger toutes les erreurs affichees avec feuille, ligne et champ.
7. Relancer l'analyse jusqu'a zero erreur.
8. Cliquer sur `Initialiser la plateforme`.
9. Verifier dans Mailpit que les invitations Industry Guest ont ete generees, puis verifier comptes, etudiants, projets, equipes et affectations dans `Gestion des donnees`.
10. Creer ensuite les phases FYP I et FYP II. Leur `academicYear` doit etre exactement egal a la valeur `cohort` importee.
11. Definir debut, echeance et statut `OPEN`.

L'analyse ne modifie pas la base. L'import final utilise une transaction unique : une erreur annule toute l'operation. Le reimport met a jour les lignes connues sans creer de doublons.

## 5. Identites, SSO et invitations

Il n'existe aucune inscription publique et les etudiants ne possedent pas de compte.

Pour `ADMINISTRATORS`, `COORDINATORS`, `SUPERVISORS` et `FACULTY_EVALUATORS`, ne fournir aucun mot de passe dans le classeur. `actorId` et l'e-mail institutionnel proviennent de la source officielle SQU, `authenticationMode` vaut `SQU_SSO` et `status` vaut normalement `ACTIVE`. L'acteur clique sur `Se connecter avec le compte SQU`. Apres validation OIDC, la plateforme recherche exactement son e-mail importe, applique son role et ouvre son dashboard. Un compte SQU absent du referentiel importe est refuse.

Pour `INDUSTRY_GUESTS`, renseigner l'organisation, une date future `accessExpiresAt` au format `YYYY-MM-DD` ou `YYYY-MM-DDTHH:mm`, puis `PENDING_INVITATION`. L'import genere un lien d'activation unique envoye par e-mail. Le lien d'invitation expire par defaut apres 48 heures. L'invite choisit alors son mot de passe externe. Son compte ne donne acces qu'au Demo Day, seulement aux projets attribues, et devient inutilisable apres `accessExpiresAt`. L'administrateur peut renvoyer une invitation depuis `Comptes et acces`.

Le lien `Mot de passe oublie` concerne les Industry Guests. En developpement, les invitations et jetons sont visibles dans Mailpit. Le compte administrateur local `admin@squ.edu.om` / `Admin@123` reste uniquement un mode de demonstration controle par `LOCAL_INTERNAL_LOGIN_ENABLED=true` ; il doit etre desactive en production.

## 6. Evaluation et calcul

- Toute saisie est enregistree comme brouillon PostgreSQL.
- Seul le bouton `Valider la fiche` verrouille la fiche et la rend calculable.
- Apres l'echeance, un brouillon est conserve pour la trace mais exclu des notes.
- L'acteur peut demander une prolongation a l'administrateur.
- Le rapport est une note commune au projet.
- La presentation combine partie individuelle et partie groupe.
- Le superviseur attribue une note individuelle a chaque etudiant.
- Demo Day est commun au projet et utilise les poids `2, 1, 4, 2, 1`.
- Plusieurs fiches verrouillees du meme type sont moyennees, y compris les notes egales a zero.

## 7. Resultats et export

Apres validation de toutes les fiches requises, l'administrateur calcule les notes par projet et phase, controle chaque etudiant puis publie les resultats.

L'administrateur ou le coordinateur telecharge un classeur comportant :

- `LEGACY_SUMMARY` : 11 colonnes compatibles avec l'ancien fichier final ;
- `FINAL_SUMMARY` : synthese enrichie par etudiant et phase ;
- `EVALUATOR_DETAILS` : detail de chaque fiche verrouillee ;
- `MISSING_FORMS` : affectations non commencees ou brouillons non valides ;
- `AUDIT_TRAIL` : actions liees au perimetre exporte.

Les e-mails signalent qu'un rapport est disponible. Le fichier de notes reste telechargeable uniquement apres authentification.

## 8. API Swagger

Avec un jeton administrateur :

- `POST /api/import/initialization/preview` ;
- `POST /api/import/initialization` ;
- `POST /api/import/students/preview` ;
- `POST /api/import/students`.

Avec un compte administrateur ou coordinateur :

- `GET /api/reports/completeness/phase/{phaseId}` ;
- `GET /api/reports/export/phase/{phaseId}` ;
- `GET /api/reports/export/project/{projectId}`.
