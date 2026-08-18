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
| `ADMINISTRATORS` | `actorId`, `actorName`, `email`, `phone`, `temporaryPassword`, `status` | Comptes administrateur |
| `COORDINATORS` | memes colonnes acteur | Comptes coordinateur FYP |
| `SUPERVISORS` | colonnes acteur + `department`, `specialization` | Encadrants |
| `FACULTY_EVALUATORS` | colonnes acteur + profil academique | Evaluateurs rapport/oral |
| `INDUSTRY_GUESTS` | colonnes acteur + `organization` | Jury industriel Demo Day |
| `PROJECT_ASSIGNMENTS` | projet, etudiant, superviseur et e-mails evaluateurs | Toutes les relations necessaires |

Les codes de filiere autorises sont `EIC`, `CSN`, `CSP` et `PSE`. Ils sont crees automatiquement au premier demarrage.

## 3. Feuille PROJECT_ASSIGNMENTS

Colonnes attendues :

`cohort`, `trackCode`, `projectNumber`, `projectTitle`, `projectAbstract`, `section`, `studentId`, `studentName`, `supervisorId`, `supervisorName`, `reportPhaseIEvaluatorEmails`, `oralPhaseIEvaluatorEmails`, `reportPhaseIIEvaluatorEmails`, `oralPhaseIIEvaluatorEmails`, `industryGuestEmails`.

Une ligne contient un seul etudiant et un seul superviseur. Pour un meme projet, ajouter plusieurs lignes :

- 1 a 5 etudiants distincts maximum ;
- 1 a 2 superviseurs distincts maximum ;
- un etudiant ne peut appartenir qu'a un projet pour la meme cohorte ;
- les e-mails rapport/oral doivent exister dans `FACULTY_EVALUATORS` ;
- les e-mails Industry Guest doivent exister dans `INDUSTRY_GUESTS` ;
- un Industry Guest recoit uniquement la fiche `DEMO_DAY_INDUSTRY` ;
- chaque superviseur recoit les fiches superviseur FYP I et FYP II du projet.

Les listes d'e-mails acceptent la virgule ou le point-virgule. Les metadonnees projet peuvent etre remplies sur la premiere ligne puis laissees vides sur les lignes suivantes du meme projet.

## 4. Parcours exact de l'administrateur

1. Se connecter avec le compte administrateur.
2. Ouvrir `Imports Excel` puis `Initialisation annuelle`.
3. Telecharger le modele officiel.
4. Completer les sept feuilles sans renommer les en-tetes.
5. Cliquer sur `Analyser sans enregistrer`.
6. Corriger toutes les erreurs affichees avec feuille, ligne et champ.
7. Relancer l'analyse jusqu'a zero erreur.
8. Cliquer sur `Initialiser la plateforme`.
9. Verifier comptes, etudiants, projets, equipes et affectations dans `Gestion des donnees`.
10. Creer ensuite les phases FYP I et FYP II. Leur `academicYear` doit etre exactement egal a la valeur `cohort` importee.
11. Definir debut, echeance et statut `OPEN`.

L'analyse ne modifie pas la base. L'import final utilise une transaction unique : une erreur annule toute l'operation. Le reimport met a jour les lignes connues sans creer de doublons.

## 5. Mots de passe et sessions

Un nouvel acteur doit avoir `temporaryPassword`. Un acteur deja existant peut laisser ce champ vide afin de conserver son mot de passe. Chaque acteur se connecte personnellement, ne voit que son espace et change son mot de passe depuis son profil.

Le lien `Mot de passe oublie` envoie un jeton valable 30 minutes. En developpement, le message est visible dans Mailpit.

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