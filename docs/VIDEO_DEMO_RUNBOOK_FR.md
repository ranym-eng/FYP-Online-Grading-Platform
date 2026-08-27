# Runbook complet pour la vidéo de démonstration

Ce scénario utilise `FYP_FULL_DEMO_DATA.xlsx`. Toutes les personnes, adresses et organisations du classeur sont fictives.

## 1. Ce que la démonstration doit prouver

La vidéo doit montrer la chaîne complète qui remplace les anciens fichiers Excel et le programme MATLAB :

1. import centralisé des données universitaires ;
2. création des projets, équipes et affectations ;
3. accès séparé selon le rôle ;
4. saisie Excel-like des évaluations ;
5. brouillon, reprise, validation et verrouillage ;
6. blocage par échéance et prolongation personnelle ;
7. consolidation automatique avec les pondérations officielles ;
8. publication, rapports Excel, e-mails, notifications et trace de l'import initial.

L'étudiant est une donnée académique. Il ne possède pas de session et ne se connecte pas à la plateforme.

## 2. Préparation avant l'enregistrement

### Adresses

| Service | Adresse |
|---|---|
| Plateforme | `http://127.0.0.1:3010` |
| Swagger | `http://127.0.0.1:8080/swagger-ui/index.html` |
| Mailpit | `http://127.0.0.1:8025` |

### Démarrage

```powershell
Set-Location "D:\Desktop\sultan qaboos\FYP-Online-Grading-Platform"
docker compose up --build -d
docker compose ps
```

Les quatre services `postgres`, `backend`, `frontend` et `mailpit` doivent être `healthy`.

Pour les comptes internes de démonstration, conserver `LOCAL_INTERNAL_LOGIN_ENABLED=true`. En production, ces comptes passent par le SSO de SQU.

### Base propre facultative

Ne faire ceci que pour une base locale jetable. La commande supprime toutes les données Docker locales :

```powershell
docker compose down -v
docker compose up --build -d
```

Si la base contient déjà la démonstration, ne pas la réinitialiser. La prévisualisation de l'import indiquera les enregistrements existants.

## 3. Import initial administrateur

1. Ouvrir la plateforme et se connecter avec `admin@squ.edu.om` / `Admin@123`.
2. Ouvrir **Excel imports** puis **Annual initialization**.
3. Choisir `FYP_FULL_DEMO_DATA.xlsx`.
4. Cliquer sur **Preview without saving**.
5. Résultat obligatoire : `9` feuilles contrôlées, `85` lignes détectées, `85` valides, `0` à corriger.
6. Les feuilles `README` et `DEMO_SCENARIOS` sont informatives et volontairement ignorées par l'importeur.
7. Cliquer sur **Initialize platform** et confirmer.
8. Se déconnecter puis utiliser `admin.demo@squ.edu.om` / `Demo@123` pour la suite.

Résultat attendu : 26 étudiants, 8 projets, 24 acteurs internes, 5 Industry Guests et 4 phases.

## 4. Comptes utilisés dans la vidéo

| Rôle | Compte | Mot de passe |
|---|---|---|
| Administrateur | `admin.demo@squ.edu.om` | `Demo@123` |
| Coordinateur | `coordinator.demo@squ.edu.om` | `Demo@123` |
| Superviseur 1 | `supervisor.eic1@squ.edu.om` | `Demo@123` |
| Superviseur 2 | `supervisor.eic2@squ.edu.om` | `Demo@123` |
| Report Evaluator 1 | `report.eic@squ.edu.om` | `Demo@123` |
| Report Evaluator 2, Phase I | `report.cross1@squ.edu.om` | `Demo@123` |
| Report Evaluator 2, Phase II | `report.cross2@squ.edu.om` | `Demo@123` |
| Faculty Evaluator 1 | `oral.eic@squ.edu.om` | `Demo@123` |
| Faculty Evaluator 2, Phase I | `oral.cross1@squ.edu.om` | `Demo@123` |
| Faculty Evaluator 2, Phase II | `oral.cross2@squ.edu.om` | `Demo@123` |
| Industry Guest 1 | `guest.automation@example.org` | activation Mailpit |
| Industry Guest 2 | `guest.innovation@example.org` | activation Mailpit |
| Test d'isolation | `report.csn@squ.edu.om` | `Demo@123` |

## 5. Chapitre administrateur : navigation et données

1. Montrer le **Dashboard**, les métriques, la phase active, les échéances et les raccourcis.
2. Ouvrir la cloche : montrer le résumé, **View all notifications** et **Mark all as read**.
3. Rechercher `EIC-01`, `Alya Nadir Demo` et `Noura Atlas Demo` dans **Search everywhere**.
4. Réduire puis rouvrir la sidebar. Réduire la largeur de la fenêtre pour montrer le responsive.
5. Basculer une fois entre light et dark mode, puis revenir au light mode.
6. Ouvrir le profil utilisateur et montrer les informations de session.

### Relations à montrer pour EIC-01

Dans **Data management**, utiliser la recherche des tableaux :

- projet : `EIC-01 - Adaptive Greenhouse Control Platform` ;
- équipe : `EIC-A` ;
- étudiants : `260001` à `260005` ;
- superviseurs : Noura Atlas Demo et Kareem Cedar Demo ;
- Report I : Leena Harbor Demo et Hala Meridian Demo ;
- Oral I : Tariq Meadow Demo et Samir North Demo ;
- Report II : Leena Harbor Demo et Nader Summit Demo ;
- Oral II : Tariq Meadow Demo et Farah Loom Demo ;
- Demo Day : Sami Relay Demo et Hana Venture Demo.

Les affectations d'évaluation se vérifient également dans **Reports > Evaluation completeness**.

## 6. CRUD complet à filmer

### Étudiant temporaire

Dans **Data management > Students > Add** :

| Champ | Valeur |
|---|---|
| stdID | `269999` |
| Cohort | `2026` |
| Official full name | `Test Student Demo` |
| SQU email | `s269999@student.squ.edu.om` |
| FYP academic year | `2026` |
| FYP track | `EIC` |
| Level | `Final Year` |

Modifier ensuite le nom en `Test Student Updated`, enregistrer et rechercher `269999`.

### Projet temporaire

Dans **Projects > Add** :

| Champ | Valeur |
|---|---|
| Project number | `TMP-01` |
| Project title | `Temporary Sensor Demonstrator` |
| Abstract | `Temporary project used during the jury demonstration.` |
| Academic year | `2026` |
| Track | `EIC` |
| Status | `ACTIVE` |

Modifier le titre en `Temporary Sensor Demonstrator - Updated`.

### Équipe temporaire

Dans **Teams > Add** : `TMP-01 Team`, section `EIC-T`, année `2026`, projet `TMP-01`, étudiant `269999`.

Montrer les validations en essayant une fois d'enregistrer un champ obligatoire vide. Nettoyer ensuite dans cet ordre : équipe, projet, étudiant.

## 7. Phase I : scénario complet d'évaluation

Utiliser le projet `EIC-01` et la phase `FYP I - Demo Cycle`.

### Superviseur 1 : brouillon et verrouillage

1. Se connecter avec `supervisor.eic1@squ.edu.om`.
2. Vérifier que seul `EIC-01` est proposé.
3. Ouvrir **Evaluations**, sélectionner `EIC-01`, `Supervisor evaluation · FYP I` et `FYP I - Demo Cycle`.
4. Saisir quelques cellules puis cliquer sur **Save draft**.
5. Recharger la page : les valeurs doivent réapparaître et le statut rester `Draft`.
6. Pour chacun des 11 critères, saisir la même note par étudiant : Alya `8.0`, Omar `8.2`, Noor `8.4`, Layan `8.6`, Zayd `8.8`.
7. Commentaire : `Good progress, clear planning and consistent individual contribution.`
8. Cliquer sur **Submit form**, confirmer et montrer que la fiche devient verrouillée.

### Superviseur 2

Avec `supervisor.eic2@squ.edu.om`, soumettre la même fiche EIC-01 avec `8.4` dans toutes les cellules des cinq étudiants.

### Report Evaluators

1. `report.eic@squ.edu.om` : EIC-01, Report Phase I, tous les critères `8.0`, puis soumettre.
2. `report.cross1@squ.edu.om` : EIC-01, Report Phase I, tous les critères `8.6`, puis soumettre.
3. Commentaire conseillé : `The report is well structured, technically sound and properly referenced.`

### Faculty Evaluators

1. `oral.eic@squ.edu.om` : pour les deux critères individuels, utiliser Alya `8.0`, Omar `8.2`, Noor `8.4`, Layan `8.6`, Zayd `8.8`; pour tous les critères de groupe, utiliser `8.2`.
2. `oral.cross1@squ.edu.om` : tous les critères individuels `8.4` et tous les critères de groupe `8.6`.
3. Soumettre les deux fiches.

## 8. Résultat Phase I attendu

Se reconnecter comme administrateur, ouvrir **Consolidated grades**, choisir EIC-01 et FYP I, puis **Calculate**.

Pondérations : Supervisor 40 %, Report 35 %, Oral 25 %. Seules les fiches soumises et verrouillées sont utilisées.

| Étudiant | Supervisor | Report | Oral | Final attendu |
|---|---:|---:|---:|---:|
| Alya Nadir Demo | 8.20 | 8.30 | 8.32 | 8.27 |
| Omar Rayyan Demo | 8.30 | 8.30 | 8.36 | 8.32 |
| Noor Hadi Demo | 8.40 | 8.30 | 8.40 | 8.37 |
| Layan Sami Demo | 8.50 | 8.30 | 8.44 | 8.42 |
| Zayd Kareem Demo | 8.60 | 8.30 | 8.48 | 8.47 |

Moyenne projet attendue : `8.37/10`. Cliquer ensuite sur **Publish** et confirmer.

## 9. Brouillon expiré et demande de prolongation

Faire ce test après la consolidation Phase I pour ne pas interrompre les fiches EIC-01.

1. Avec `supervisor.eic2@squ.edu.om`, ouvrir EIC-02 / Supervisor Phase I, saisir quelques notes et enregistrer le brouillon sans le soumettre.
2. Comme administrateur, modifier FYP I : deadline = une heure dans le passé, statut `CLOSED`.
3. Revenir au superviseur : la modification et la soumission sont bloquées; le brouillon n'est pas pris en compte dans les notes.
4. Ouvrir **Extensions**, cliquer sur **New request**, choisir la phase et saisir uniquement `Additional time is required to complete the verified assessment.`
5. Aucun champ de nouvelle date ne doit être proposé au superviseur.
6. Comme administrateur, ouvrir **Extensions**, filtrer `Pending`, cliquer **Review**.
7. Choisir une échéance future, ajouter `Approved for the demonstration after verification.` et approuver.
8. Revenir au superviseur : la notification redirige vers le bon écran et l'extension personnelle permet de reprendre la fiche, même si la phase globale reste fermée.
9. Réouvrir ensuite FYP I avec une échéance future si d'autres tests Phase I sont nécessaires.

## 10. Industry Guest et Demo Day

### Préparer FYP II

Comme administrateur, modifier `FYP II - Demo Cycle` : date de début antérieure à maintenant, deadline future, statut `OPEN`.

### Activer les invités

1. Ouvrir Mailpit et rechercher `guest.automation@example.org`.
2. Ouvrir **Invitation to the SQU FYP grading platform**, suivre le lien et définir `Industry@123`.
3. Répéter pour `guest.innovation@example.org` avec `Innovation@123`.
4. Si le lien a plus de 48 heures, utiliser **Data management > Accounts and access > Invite** pour le renvoyer.

### Tester les restrictions

Avec `guest.automation@example.org` :

1. vérifier le dashboard Industry ;
2. vérifier que seuls EIC-01 et EIC-02 sont disponibles ;
3. vérifier que seule la fiche Demo Day est modifiable ;
4. EIC-01 : mettre `8.8` dans les cinq critères, sauvegarder le brouillon puis soumettre ;
5. ouvrir **Consolidated grades** en lecture seule ;
6. vérifier l'absence des fiches Supervisor, Report et Oral.

Avec `guest.innovation@example.org`, soumettre EIC-01 Demo Day avec `9.2` dans tous les critères.

## 11. Phase II complète facultative

Pour calculer également FYP II sur EIC-01, soumettre :

| Compte | Formulaire | Valeur dans toutes les cellules |
|---|---|---:|
| `supervisor.eic1@squ.edu.om` | Supervisor II | 8.5 |
| `supervisor.eic2@squ.edu.om` | Supervisor II | 8.7 |
| `report.eic@squ.edu.om` | Report II | 8.2 |
| `report.cross2@squ.edu.om` | Report II | 8.6 |
| `oral.eic@squ.edu.om` | Oral II individuel et groupe | 8.4 |
| `oral.cross2@squ.edu.om` | Oral II individuel et groupe | 8.6 |
| `guest.automation@example.org` | Demo Day | 8.8 |
| `guest.innovation@example.org` | Demo Day | 9.2 |

Pondérations Phase II : Supervisor 30 %, Report 25 %, Oral 25 %, Demo Day 20 %. Résultat attendu pour chaque étudiant : `8.61/10`.

## 12. Rapports et remplacement de MATLAB

1. Comme administrateur, ouvrir **Reports**.
2. Choisir EIC-01 et FYP I, puis vérifier **Evaluation completeness** : les six affectations Phase I doivent être `SUBMITTED`.
3. Cliquer **Phase summary** : téléchargement du résumé de toute la phase.
4. Cliquer **Project export** : téléchargement des résultats EIC-01.
5. Cliquer **Archive phase**, confirmer, puis générer **Final report**.
6. Ouvrir **Report archive**, tester **Regenerate** puis **Send**.
7. Dans Mailpit, vérifier l'e-mail destiné à `fyp-coordinator@squ.edu.om` et la pièce jointe Excel.
8. Expliquer que cette chaîne remplace MATLAB : données centralisées, moyennes par évaluateur, pondérations par phase, exclusion des brouillons, résultats individuels et export final.

## 13. Coordinateur

1. Se connecter avec `coordinator.demo@squ.edu.om`.
2. Vérifier le dashboard de suivi.
3. Ouvrir **Consolidated grades** et consulter les résultats publiés.
4. Ouvrir **Reports**, télécharger les exports et consulter l'archive.
5. Vérifier que **Data management** et **Excel imports** sont absents.

## 14. Notifications et rappels 24 h / 12 h

1. Comme administrateur, mettre une phase ouverte avec une deadline dans moins de 24 heures.
2. Attendre le planificateur, exécuté toutes les 15 minutes, ou appeler dans Swagger `POST /api/notifications/reminders/evaluation-deadline` avec un token Admin.
3. Vérifier la cloche chez un évaluateur : alerte, compteur, résumé, redirection et marquage comme lu.
4. Refaire le test avec une deadline dans moins de 12 heures pour l'alerte urgente.

## 15. Mot de passe oublié

Faire ce test à la fin pour ne pas changer un compte utilisé dans les autres chapitres.

1. Depuis le login, cliquer **Forgot password?**.
2. Entrer `coordinator.quality@squ.edu.om`.
3. Vérifier le message générique, ouvrir Mailpit et suivre le lien.
4. Définir `ResetDemo@123`, puis se connecter avec ce nouveau mot de passe.
5. Refaire la demande avec `unknown@example.org` : le message affiché doit rester identique pour ne pas révéler l'existence du compte.

## 16. Sécurité et cas négatifs

| Test | Résultat attendu |
|---|---|
| Connexion avec `s260001@student.squ.edu.om` | refus : aucun acteur Student |
| `report.csn@squ.edu.om` dans Evaluations | uniquement les projets CSN, aucun EIC |
| Superviseur 1 | EIC-01 seulement |
| Report Evaluator | Report I/II seulement |
| Faculty Evaluator | Oral I/II seulement |
| Industry Guest | Demo Day seulement |
| Score vide ou fiche incomplète | soumission refusée |
| Score inférieur à 0 ou supérieur à 10 | l'interface borne la valeur; l'API refuse une valeur hors intervalle |
| Fiche soumise | verrouillée et non modifiable |
| Brouillon | absent du calcul consolidé |
| Phase future, fermée, archivée ou expirée | évaluation bloquée |
| Extension demandée avant expiration | refusée |
| Deuxième demande déjà Pending | refusée |
| Publication avant calcul | refusée |
| Calcul avec un type d'évaluation manquant | `GRADE_NOT_READY` |
| Suppression d'une ressource liée | refus métier ou confirmation explicite |

## 17. Audit et nettoyage final

1. Revenir comme administrateur dans **Data management > Audit logs**.
2. Montrer l'événement d'initialisation créé par l'import du classeur.
3. Ne pas annoncer que toutes les actions métier sont auditées : dans la version actuelle, les CRUD, validations et publications ne produisent pas encore tous une entrée d'audit.
4. Supprimer uniquement les objets `TMP-01` et `269999` s'ils existent encore.
5. Ne pas supprimer les comptes ou projets du classeur de démonstration.

## 18. Ordre conseillé pour une vidéo fluide

1. Introduction de 20 secondes : ancien Excel/MATLAB contre plateforme centralisée.
2. Import 85/85 et contrôle des données EIC-01.
3. Dashboard, recherche, notifications, responsive et CRUD temporaire.
4. Brouillon puis validation Supervisor I.
5. Report I et Oral I; accélérer les secondes fiches si nécessaire.
6. Consolidation Phase I, résultat 8.27 à 8.47, moyenne 8.37, publication.
7. Blocage d'échéance, demande sans date et approbation Admin.
8. Invitation Industry, activation Mailpit et Demo Day uniquement.
9. Rapports Excel, envoi Mailpit et explication du remplacement de MATLAB.
10. Coordinateur, audit, mot de passe oublié et test final d'accès interdit.

Pour une vidéo courte, préparer les secondes fiches avant l'enregistrement et filmer en direct une fiche complète, un brouillon, une validation, une consolidation et un export.
