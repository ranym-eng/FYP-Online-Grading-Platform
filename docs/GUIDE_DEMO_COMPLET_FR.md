# Guide de démonstration complet

Ce guide utilise uniquement les données fictives du fichier `FYP_FULL_DEMO_DATA.xlsx`.

## 1. Démarrage

Depuis PowerShell :

```powershell
Set-Location "D:\Desktop\sultan qaboos\FYP-Online-Grading-Platform"
docker compose up --build -d
docker compose ps
```

Ouvrir ensuite :

| Service | Adresse |
|---|---|
| Plateforme | http://localhost:3010 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Mailpit | http://localhost:8025 |

Le mode de démonstration locale doit avoir `LOCAL_INTERNAL_LOGIN_ENABLED=true` dans `.env`.

## 2. Initialiser les données

1. Se connecter avec le compte administrateur initial `admin@squ.edu.om` / `Admin@123`.
2. Ouvrir **Imports Excel**.
3. Télécharger ou sélectionner `FYP_FULL_DEMO_DATA.xlsx`.
4. Cliquer sur **Prévisualiser** et vérifier que les lignes sont valides.
5. Cliquer sur **Importer**.
6. Vérifier les utilisateurs, étudiants, projets, équipes, affectations et phases dans **Gestion des données**.

Le classeur importe 26 étudiants, 8 projets, 24 acteurs internes, 5 invités Industry et 4 phases. Les comptes internes importés utilisent `Demo@123` uniquement en mode de démonstration locale.

Dans la base Docker actuellement testée, les fiches Supervisor I, Report I et Oral I de `EIC-01` sont déjà validées, la note `8,19/10` est publiée et un rapport de phase est archivé. Pour montrer la saisie d’un nouveau brouillon sans réinitialiser la base, utiliser `EIC-02`. Pour rejouer tout le scénario depuis zéro, arrêter Docker avec suppression des volumes, redémarrer, puis réimporter ce classeur; cette opération efface les données locales existantes.

## 3. Comptes principaux

| Rôle | Compte | Mot de passe | Données attendues |
|---|---|---|---|
| Administrateur | `admin.demo@squ.edu.om` | `Demo@123` | Toutes les données |
| Coordinateur | `coordinator.demo@squ.edu.om` | `Demo@123` | Notes, rapports et audit |
| Superviseur | `supervisor.eic1@squ.edu.om` | `Demo@123` | Projet EIC-01 uniquement |
| Évaluateur de rapports | `report.eic@squ.edu.om` | `Demo@123` | EIC-01 et EIC-02, rapports uniquement |
| Évaluateur académique | `oral.eic@squ.edu.om` | `Demo@123` | Projets EIC attribués, oral uniquement |
| Industry Guest | `guest.automation@example.org` | Activation Mailpit | EIC-01 et EIC-02, Demo Day uniquement |
| Test d'isolation | `report.csn@squ.edu.om` | `Demo@123` | Aucun projet EIC |

## 4. Démonstration administrateur

### Tableau de bord

1. Se connecter avec `admin.demo@squ.edu.om`.
2. Montrer les indicateurs, la phase active, les échéances et les raccourcis.
3. Ouvrir la cloche et vérifier le résumé des notifications.
4. Utiliser la recherche globale pour rechercher `EIC-01` ou `Noura Atlas Demo`.

### Gestion académique

1. Ouvrir **Gestion des données**.
2. Filtrer les projets avec `EIC-01`.
3. Ouvrir le projet **Adaptive Greenhouse Control Platform**.
4. Vérifier ses 5 étudiants et ses 2 superviseurs.
5. Modifier son résumé, enregistrer, puis remettre le texte initial.
6. Créer un projet de test `TMP-01`, le modifier, puis le supprimer.
7. Rechercher l'étudiant `260001` et vérifier son rattachement.
8. Vérifier les affectations Report, Oral et Industry du projet EIC-01.

### Données CRUD exactes pour la démonstration

| Action | Valeurs à saisir |
|---|---|
| Ajouter un étudiant temporaire | `studentNumber=269999`, `cohort=2026`, `fullName=Test Student Demo`, `email=s269999@student.squ.edu.om`, `academicYear=2026`, `trackCode=EIC`, `level=Final Year` |
| Ajouter un projet temporaire | `projectNumber=TMP-01`, `title=Temporary Sensor Demonstrator`, `abstractText=Temporary project used during the jury demonstration.`, `academicYear=2026`, `track=EIC`, `status=ACTIVE` |
| Modifier le projet | Remplacer le titre par `Temporary Sensor Demonstrator - Updated`, puis enregistrer |
| Ajouter une équipe temporaire | `name=TMP-01 Team`, `section=EIC-T`, `academicYear=2026`, projet `TMP-01`, étudiant `269999` |
| Tester la recherche | Rechercher successivement `TMP-01`, `269999`, puis `EIC-01` |
| Nettoyer | Supprimer l’équipe, puis le projet, puis l’étudiant temporaire en confirmant chaque action |

### Phases et échéances

1. Vérifier que **FYP I - Demo Cycle** est ouverte jusqu'au 30/09/2026.
2. Vérifier que **FYP II - Demo Cycle** est future.
3. Pour tester Demo Day, modifier FYP II avec une date de début antérieure à la date du jour, conserver une échéance future, puis l'ouvrir.
4. Montrer que seul l'administrateur peut changer les dates.

### Prolongations

1. Fermer temporairement FYP I après avoir terminé le test de brouillon.
2. Se connecter comme superviseur et envoyer une demande avec un motif, sans proposer de date.
3. Revenir comme administrateur dans **Prolongations**.
4. Ouvrir la demande, choisir la nouvelle échéance et l'approuver.
5. Vérifier la notification reçue par le demandeur.

## 5. Démonstration superviseur

1. Se connecter avec `supervisor.eic1@squ.edu.om` / `Demo@123`.
2. Vérifier que seul EIC-01 est disponible.
3. Ouvrir **Évaluations** et choisir la fiche Supervisor Phase I.
4. Saisir des notes partielles et un commentaire.
5. Cliquer sur **Enregistrer le brouillon**.
6. Recharger la page et vérifier que le brouillon réapparaît.
7. Compléter tous les critères.
8. Cliquer sur **Valider la fiche** et confirmer.
9. Vérifier le statut soumis/verrouillé et l'impossibilité de modifier les notes.
10. Vérifier que le brouillon seul n'apparaît pas dans les notes consolidées.

Test négatif : essayer d'accéder à CSN-01. Le projet ne doit pas apparaître dans la liste et un accès direct doit être refusé par l'API.

## 6. Démonstration évaluateur de rapports

1. Se connecter avec `report.eic@squ.edu.om` / `Demo@123`.
2. Vérifier que seuls EIC-01 et EIC-02 sont visibles.
3. Vérifier que seules les fiches **Report I** et **Report II** sont proposées.
4. Remplir la fiche Report I d'EIC-01 avec les critères inspirés des anciens fichiers Excel.
5. Enregistrer un brouillon, le reprendre, le compléter et le valider.
6. Vérifier que les évaluations Supervisor, Oral et Demo Day ne sont pas modifiables.

Test d'isolation : se connecter avec `report.csn@squ.edu.om`. Les projets EIC-01/EIC-02 doivent être absents.

## 7. Démonstration évaluateur académique

1. Se connecter avec `oral.eic@squ.edu.om` / `Demo@123`.
2. Vérifier les projets EIC attribués.
3. Ouvrir la fiche Oral Phase I d'EIC-01.
4. Saisir les critères de présentation et les commentaires.
5. Enregistrer le brouillon, puis valider la fiche complète.
6. Vérifier que les fiches Report, Supervisor et Industry ne sont pas accessibles en écriture.

## 8. Démonstration Industry Guest

### Première activation

1. Ouvrir Mailpit après l'import.
2. Rechercher le message envoyé à `guest.automation@example.org`.
3. Ouvrir le lien d'activation.
4. Définir un mot de passe de démonstration, par exemple `Industry@123`.
5. Revenir au login et se connecter.

### Accès limité

1. Vérifier que l'espace affiche le rôle Industry et non un profil générique d'évaluateur.
2. Vérifier que seuls EIC-01 et EIC-02 sont proposés.
3. Vérifier que seule la fiche **Demo Day** peut être remplie.
4. Remplir les critères, enregistrer le brouillon, puis valider.
5. Ouvrir **Notes consolidées** pour consulter les autres résultats en lecture seule.
6. Vérifier qu'Industry ne peut ni modifier une phase ni évaluer Report, Oral ou Supervisor.

## 9. Consolidation et remplacement de MATLAB

Après validation des fiches requises :

1. Se connecter comme administrateur.
2. Ouvrir **Notes consolidées**.
3. Sélectionner EIC-01 et la phase concernée.
4. Lancer le calcul/recalcul.
5. Vérifier que seules les fiches verrouillées sont utilisées.
6. Vérifier les moyennes par type d'évaluation et les pondérations.
7. Publier les notes lorsque les résultats sont complets et confirmer la fenêtre de publication.
8. Ouvrir **Rapports**, générer le rapport de phase puis le rapport final.
9. Exporter le fichier Excel et comparer sa structure au résumé final de l'ancien processus MATLAB.

Résultat réel vérifié pour `EIC-01 / FYP I - Demo Cycle`: cinq étudiants, scores finaux compris entre `8,09` et `8,29`, moyenne de phase `8,19`, rapport archivé avec statut `GENERATED`, export Excel HTTP `200` de `13 118` octets.

Le remplacement de MATLAB est démontré par la chaîne suivante : affectations centralisées, saisie directe des critères, brouillons exclus, validation/verrouillage, calcul pondéré côté serveur, contrôle de complétude, génération et export du résultat final.

## 10. Démonstration coordinateur

1. Se connecter avec `coordinator.demo@squ.edu.om` / `Demo@123`.
2. Consulter l'avancement des évaluations.
3. Vérifier les notes consolidées sans pouvoir modifier les données administratives.
4. Ouvrir les rapports de phase et finaux.
5. Exporter les résultats disponibles.
6. Revenir avec l’administrateur pour vérifier la trace des actions dans **Journaux d’audit**; le coordinateur n’a pas accès à la gestion administrative.

## 11. Notifications et récupération du mot de passe

### Notifications

1. Ouvrir la cloche depuis plusieurs rôles.
2. Cliquer sur une notification et vérifier la redirection vers l'écran concerné.
3. Marquer une notification comme lue, puis toutes comme lues.
4. Pour les rappels de délai, vérifier les alertes à 24 h et 12 h.

### Mot de passe oublié

1. Depuis le login, cliquer sur **Mot de passe oublié ?**.
2. Entrer `supervisor.eic2@squ.edu.om`.
3. Vérifier le message générique de sécurité.
4. Ouvrir Mailpit et suivre le lien de réinitialisation.
5. Définir le nouveau mot de passe, puis se reconnecter.
6. Répéter avec une adresse inconnue et vérifier que le message reste identique.

## 12. Tests de règles métier à montrer

| Cas | Résultat attendu |
|---|---|
| Brouillon enregistré avant l'échéance | Conservé et modifiable |
| Brouillon non validé à l'échéance | Exclu des calculs |
| Fiche complète validée | Soumise, verrouillée et prise en compte |
| Phase fermée ou date dépassée | Saisie et validation refusées |
| Demande de prolongation par un acteur | Motif seulement, aucune date imposée |
| Approbation par l'administrateur | Nouvelle échéance choisie par l'administrateur |
| Projet non attribué | Invisible et refusé côté serveur |
| Industry hors Demo Day | Lecture éventuelle des résultats, aucune saisie |
| Calcul avec fiches manquantes | Complétude signalée, publication à contrôler |
| Suppression liée à des données utilisées | Refus ou confirmation explicite selon la ressource |

## 13. Ordre conseillé pour la vidéo

1. Login et import administrateur.
2. Projet EIC-01 avec équipe, 5 étudiants, 2 superviseurs et affectations.
3. Brouillon puis validation du superviseur.
4. Évaluation Report I.
5. Évaluation Oral I.
6. Activation et évaluation Demo Day de l'invité Industry.
7. Demande et approbation d'une prolongation.
8. Consolidation, publication, rapport et export.
9. Consultation coordinateur et audit.
10. Test final d'accès interdit avec `report.csn@squ.edu.om`.
