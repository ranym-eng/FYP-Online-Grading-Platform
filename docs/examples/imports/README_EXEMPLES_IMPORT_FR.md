# Exemples de fichiers Excel a importer

Ces fichiers contiennent uniquement des donnees fictives et sont compatibles avec les imports de la plateforme.

## Fichiers

- `exemple_initialisation_complete_fyp_2027_2028.xlsx` : initialisation annuelle complete avec 4 tracks, 8 etudiants, 10 acteurs, 4 projets, les equipes, les encadrants, les evaluateurs et les phases.
- `exemple_initialisation_minimale_fyp_2028_2029.xlsx` : exemple simple avec un projet PSE, 2 etudiants, 1 encadrant, 1 evaluateur academique et 1 representant industriel.
- `exemple_mise_a_jour_etudiants_2029.xlsx` : mise a jour du referentiel etudiant seulement, au format `stdID`, `cohort`, `name`, `Email`.

## Utilisation

1. Se connecter avec un compte administrateur.
2. Ouvrir **Imports Excel**.
3. Choisir **Initialisation annuelle** pour les deux premiers fichiers, ou **Mise a jour des etudiants** pour le troisieme.
4. Selectionner le fichier puis cliquer sur **Analyser**.
5. Verifier que le resultat indique que toutes les lignes sont valides.
6. Cliquer sur **Importer** uniquement lorsque les donnees affichees sont correctes.

## Acces des comptes fictifs

Les acteurs internes utilisent leur identifiant institutionnel SQU avec le SSO. Aucun mot de passe n'est stocke dans le fichier Excel. Les invites Industry sont importes avec le statut `PENDING_INVITATION` et une date `accessExpiresAt`; la plateforme leur envoie ensuite un lien d'activation a usage unique.

Pour une nouvelle initialisation, utiliser de preference le modele officiel `docs/templates/modele_initialisation_plateforme_fyp.xlsx`. Les deux classeurs d'exemple restent des jeux de donnees fictifs compatibles avec l'ancien format normalise `ACTORS`.

Les etudiants ne recoivent pas de compte utilisateur. Ils sont importes comme donnees academiques, puis associes aux equipes et aux projets.
