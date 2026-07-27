# Import des étudiants SQU et évaluation Industry Guest

## 1. Référentiel étudiant officiel

Le modèle de données reprend directement la structure du fichier fourni par l'université.

| Colonne Excel | Champ API | Règle |
| --- | --- | --- |
| `stdID` | `studentNumber` | 5 à 12 chiffres, unique |
| `cohort` | `cohort` | `YY` ou `YYYY`; `22` devient `2022` |
| `name` | `fullName` | Nom officiel complet, obligatoire |
| `Email` | `email` | Doit être exactement `s<stdID>@student.squ.edu.om`, unique |

Les champs `academicYear`, `trackCode` et `level` sont des enrichissements FYP facultatifs. Un étudiant est une donnée académique gérée par l'administrateur; l'import ne crée aucun compte utilisateur.

## 2. Import collectif par l'administrateur

1. Se connecter avec un compte administrateur.
2. Ouvrir **Imports Excel**, puis l'onglet **Étudiants**.
3. Télécharger au besoin `modele_import_etudiants_squ.xlsx`.
4. Choisir un fichier `.xlsx` ou `.csv` de 10 Mo maximum.
5. Cliquer sur **Analyser le fichier**.
6. Corriger toutes les erreurs affichées.
7. Cliquer sur **Importer dans PostgreSQL**.

L'analyse ne modifie pas la base. L'import est atomique: si une ligne est invalide, aucune ligne n'est enregistrée. Pour chaque ligne valide:

- `CREATE`: le `stdID` n'existe pas et un étudiant est créé;
- `UPDATE`: le `stdID` existe et les données officielles sont actualisées;
- `UNCHANGED`: les données correspondent déjà à PostgreSQL.

## 3. Ajout manuel et CRUD

Dans **Administration > Étudiants**, l'administrateur peut:

- créer un étudiant;
- consulter et rechercher par identifiant, nom ou e-mail;
- filtrer par cohorte ou filière;
- modifier les données officielles et les champs FYP facultatifs;
- supprimer un étudiant.

Exemple de création:

```json
{
  "studentNumber": "142430",
  "cohort": "2022",
  "fullName": "Mohammed Qasim Al Saadi",
  "email": "s142430@student.squ.edu.om",
  "academicYear": "2026-2027",
  "trackCode": "PSE",
  "level": "Final year"
}
```

## 4. Endpoints Swagger

Tous les endpoints d'écriture exigent un administrateur authentifié.

| Méthode | Endpoint | Fonction |
| --- | --- | --- |
| `POST` | `/api/import/students/preview` | Valider le fichier multipart sans enregistrer |
| `POST` | `/api/import/students` | Importer ou actualiser toutes les lignes valides |
| `POST` | `/api/students` | Ajouter un étudiant |
| `GET` | `/api/students` | Lister les étudiants |
| `GET` | `/api/students/{id}` | Consulter un étudiant |
| `PUT` | `/api/students/{id}` | Modifier un étudiant |
| `DELETE` | `/api/students/{id}` | Supprimer un étudiant |
| `GET` | `/api/students/search?keyword=...` | Rechercher |
| `GET` | `/api/students/by-cohort/{cohort}` | Filtrer par cohorte |
| `GET` | `/api/students/by-track/{trackCode}` | Filtrer par filière |

Dans Swagger, choisir un endpoint multipart, cliquer sur **Try it out**, sélectionner le fichier dans le champ `file`, puis exécuter.

## 5. Fiche Industry Guest

La fiche Demo Day est une évaluation du projet entier, pas une évaluation individuelle des étudiants. Le projet doit avoir un `projectNumber`, affiché sur la fiche avec la filière, le membre du jury et la date.

Chaque critère est noté sur 10:

| Critère | Poids |
| --- | ---: |
| Sélectionner les composants, construire et tester le prototype | 2 |
| Présenter le prototype clairement et logiquement | 1 |
| Répondre aux questions et commentaires | 4 |
| Achever le travail proposé | 2 |
| Qualité de l'affiche, contenu technique et anglais | 1 |

La note finale est:

```text
(2 × C1 + C2 + 4 × C3 + 2 × C4 + C5) / 10
```

Le frontend et le backend utilisent la même pondération. Le backend refuse les critères absents, supplémentaires ou incomplets.

## 6. Brouillon, échéance et validation

- Chaque modification est enregistrée comme brouillon.
- Un brouillon n'entre jamais dans le calcul officiel.
- **Valider la fiche** vérifie toutes les notes, soumet et verrouille la fiche.
- Après l'échéance de la phase, une fiche non validée reste traçable mais n'est pas prise en compte.
- Des notifications sont envoyées environ 24 h et 12 h avant l'échéance.
- Un évaluateur peut demander une prolongation; seul un administrateur peut l'approuver ou la refuser.

## 7. Vérifications réalisées

- Le fichier réel `FYP_students_2026_2027.xlsx` contient 130 lignes reconnues comme valides.
- Les cohortes à deux chiffres sont normalisées.
- Les identifiants et e-mails sont contrôlés et dédupliqués.
- La formule Industry Guest est couverte côté Java et côté JavaScript.
- Le modèle téléchargeable ne contient aucune donnée personnelle réelle.