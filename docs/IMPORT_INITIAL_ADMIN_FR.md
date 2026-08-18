# Initialisation annuelle des donnees FYP

## 1. Constat sur les anciens fichiers Excel

Les fichiers `EIC-FYP-SP26-Forms.xlsx`, `PSE-FYP-SP26-Forms.xlsx`,
`CSN-FYP-SP26-Forms.xlsx` et `CSP-FYP-SP26-Forms.xlsx` utilisent la meme
structure de notation. Les deux fichiers CSP fournis sont des copies identiques.

Chaque ancien classeur remplissait deux fonctions en meme temps :

1. formulaire de notation pour les presentations, rapports, superviseurs et Demo Day ;
2. mini-base de donnees cachee dans les feuilles `D1`, `D2`, `D3` et `temp`.

Les feuilles cachees contenaient notamment :

- les codes et titres des projets ;
- un ou plusieurs superviseurs par projet ;
- les identifiants et noms des etudiants ;
- la filiere EIC, CSN, CSP ou PSE ;
- des listes utilisees par les menus deroulants des fiches manuelles.

Cette duplication produisait plusieurs risques : incoherence entre filieres,
erreurs de copier-coller, formules modifiees, versions concurrentes et absence de
controle central des droits.

## 2. Principe retenu dans la plateforme

La plateforme remplace les anciennes feuilles de notation. Les criteres, calculs,
brouillons, validations et consolidations sont geres par le backend et ne doivent
plus etre recopies dans un classeur annuel.

L'administrateur importe uniquement les donnees de reference et leurs relations au
moyen du fichier :

`modele_initialisation_plateforme_fyp.xlsx`

Les etudiants restent des donnees academiques. Ils ne recoivent pas de compte et
ne sont pas un acteur de la plateforme.

## 3. Feuilles du modele maitre

| Feuille | Contenu | Cle de mise a jour |
|---|---|---|
| `TRACKS` | Filieres EIC, CSN, CSP et PSE | `code` |
| `STUDENTS` | Etudiants provenant de la base universitaire | `studentNumber` |
| `ACTORS` | Admin, coordinateur, superviseur, evaluateur et industrie | `email` |
| `PROJECTS` | Projets et equipe associee | `projectNumber` |
| `TEAM_MEMBERS` | Appartenance des etudiants aux equipes | projet + etudiant |
| `SUPERVISORS` | Superviseurs affectes aux projets | projet + e-mail |
| `EVALUATOR_ASSIGNMENTS` | Autorisations de notation par type de fiche | projet + e-mail + type |
| `PHASES` | FYP I, FYP II, dates et delais | annee academique + type |

Les feuilles `INSTRUCTIONS`, `REFERENCE_LISTS` et `EXAMPLES` ne sont pas
importees. Elles expliquent le modele et fournissent les valeurs autorisees.

## 4. Parcours exact de l'administrateur

1. Ouvrir `Imports Excel` dans l'espace administrateur.
2. Choisir `Initialisation annuelle`.
3. Telecharger le modele officiel.
4. Exporter les etudiants et le personnel depuis la base de l'universite.
5. Completer les huit feuilles de donnees, dans leur ordre.
6. Cliquer sur `Analyser sans enregistrer`.
7. Corriger toutes les erreurs affichees avec feuille, ligne et colonne.
8. Relancer l'analyse jusqu'a obtenir zero erreur.
9. Cliquer sur `Initialiser la plateforme`.
10. Verifier les projets, equipes, phases et affectations dans les modules de gestion.

L'analyse est sans ecriture. L'import final utilise une seule transaction : si une
erreur inattendue survient, l'ensemble est annule.

## 5. Regles de validation importantes

- Les en-tetes des feuilles ne doivent pas etre renommes.
- Un nouvel acteur doit avoir un mot de passe temporaire.
- Un acteur deja existant peut laisser `temporaryPassword` vide ; son mot de passe
  actuel n'est pas remplace.
- Le role `STUDENT` n'existe pas.
- Le courriel etudiant doit correspondre a
  `s<studentNumber>@student.squ.edu.om`.
- Toute filiere, tout etudiant, tout projet et tout acteur reference doit exister
  dans le fichier ou deja dans la base.
- Un acteur `SUPERVISOR` est affecte dans `SUPERVISORS`.
- Une ligne `SUPERVISORS` cree automatiquement les acces
  `SUPERVISOR_PHASE_I` et `SUPERVISOR_PHASE_II` pour le projet.
- Un `FACULTY_EVALUATOR` utilise seulement les fiches `REPORT_*` et `ORAL_*`.
- Un `INDUSTRY_REPRESENTATIVE` utilise seulement `DEMO_DAY_INDUSTRY`.
- La date limite d'une phase doit etre posterieure a sa date de debut.
- Plusieurs superviseurs peuvent etre affectes au meme projet.

## 6. Reimport et corrections

L'import est idempotent : il effectue une creation ou une mise a jour selon la cle
metier. Le meme fichier peut etre relance apres une correction sans creer de
doublons.

Par securite, l'absence d'une ligne dans un nouveau fichier ne supprime pas la
donnee correspondante. Une suppression ou une desactivation reste une action
explicite dans l'interface d'administration.

Pour une mise a jour ponctuelle pendant l'annee, l'administrateur peut utiliser
l'onglet `Mise a jour etudiants`, qui n'affecte ni les projets ni les jurys.

## 7. API Swagger

Avec un jeton administrateur :

- `POST /api/import/initialization/preview` : analyse sans sauvegarde ;
- `POST /api/import/initialization` : import transactionnel complet ;
- `POST /api/import/students/preview` : analyse du fichier etudiants ;
- `POST /api/import/students` : mise a jour des etudiants seulement.

Les quatre operations utilisent `multipart/form-data` avec une partie `file`.

## 8. Evolution recommandee

Le modele Excel constitue une passerelle fiable pour la premiere version. En
production, la meilleure evolution est une integration en lecture seule avec la
base ou l'API officielle SQU : synchronisation planifiee des etudiants et du
personnel, puis confirmation des projets et affectations par le coordinateur.

Les validations, restrictions de role, journaux d'audit et transactions du present
import resteront utiles dans ce futur connecteur.
