# Refonte frontend premium

## Objectif

La refonte donne a la plateforme FYP Online Grading une identite visuelle institutionnelle, moderne et coherente, tout en conservant les routes, les appels REST, les permissions et les traitements metier existants.

## Direction artistique

- Palette inspiree de Sultan Qaboos University: vert institutionnel, or, bordeaux, bleu et turquoise utilises avec mesure.
- Typographie claire, hierarchie visuelle compacte et densite adaptee a un outil academique utilise regulierement.
- Photographie reelle d'une exposition FYP de SQU sur l'ecran de connexion.
- Compositions asymetriques sur l'authentification et organisation utilitaire dans l'espace de travail.
- Rayons limites, ombres discretes, contrastes lisibles et icones Lucide homogenes.
- Themes clair et sombre persistants dans le navigateur.

Source du visuel institutionnel: [Sultan Qaboos University - Annual Exhibition of Student Projects](https://anwaar.squ.edu.om/ar/%D9%86%D8%B4%D8%B1/ArticleID/4531/Annual-Exhibition-of-Student-Projects-at-the-College-of-Economics-and-Political-Science).

## Espaces par role

La connexion redirige chaque compte vers un tableau de bord et une navigation filtres par ses autorisations:

| Role | Experience principale |
| --- | --- |
| Administrateur | Configuration academique, comptes, imports, projets, equipes, phases, formulaires, affectations, extensions, notes, rapports et audit. |
| Encadrant | Projets encadres, evaluations FYP I/FYP II, brouillons, validation finale, echeances et demandes de prolongation. |
| Evaluateur academique | Rapports et presentations assignes, formulaires disponibles, brouillons, validations et suivi des delais. |
| Representant industriel | Evaluation Demo Day, pertinence industrielle, prototype, commentaires et prolongations. |
| Coordinateur FYP | Suivi global, progression des campagnes, consolidation, publication, exports et reporting. |

## Composants transversaux

- Barre laterale contextuelle et navigation mobile hors-canvas.
- Barre superieure avec recherche globale, actualisation, notifications, langue, theme et profil.
- Recherche avec suggestions vers les vues et projets autorises.
- Cartes de metriques animees alimentees par les donnees API.
- Tableaux, formulaires, filtres, actions et panneaux harmonises.
- Centre de notifications commun a tous les comptes.
- Calendrier FYP construit a partir des phases reelles.
- Tiroir de profil avec preferences d'affichage et de langue.
- Toasts de succes et d'erreur, skeleton loaders, etats vides, acces refuse, erreur 404 et erreur serveur.

## Responsive et accessibilite

- Grand ecran: navigation permanente et grille de donnees dense.
- Tablette: grilles simplifiees et panneaux reorganises.
- Mobile: menu coulissant, actions compactes, formulaires en une colonne et tableaux defilables sans debordement de page.
- Focus visible, libelles accessibles, zones tactiles stables et support de `prefers-reduced-motion`.
- Aucune taille de texte ne depend directement de la largeur du viewport.

## Animations

- Entree legere des pages et cartes.
- Transitions du menu mobile, du tiroir de profil et des resultats de recherche.
- Retour visuel sur les boutons, les validations et les notifications.
- Indicateurs de progression et graphiques animes avec des durees courtes.
- Animations desactivees ou reduites lorsque le systeme demande moins de mouvement.

## Compatibilite fonctionnelle

La refonte reutilise le client API et les composants metier existants. Les parcours suivants restent connectes a Spring Boot et PostgreSQL:

- authentification et session par role;
- CRUD administratifs;
- imports Excel et validation;
- affectations et gestion des equipes;
- creation et configuration des phases;
- saisie automatique des brouillons;
- validation et verrouillage des fiches;
- blocage par echeance et demandes de prolongation;
- consolidation, publication et rapports;
- notifications et journal d'audit.

## Fichiers principaux

| Fichier | Responsabilite |
| --- | --- |
| `frontend/src/App.jsx` | Routage interne, session, vues metier et composition de l'espace de travail. |
| `frontend/src/design-system.css` | Tokens, themes, responsive, animations et styles des composants. |
| `frontend/src/workspaceUi.jsx` | Recherche, calendrier, tiroir de profil, icones et etats UX reutilisables. |
| `frontend/src/i18n.js` | Traductions statiques et dynamiques francais/anglais. |
| `frontend/src/config.js` | Roles, menus et autorisations de navigation. |

## Validation executee

- `npm run lint`: reussi.
- `npm run test`: 7 modeles de notation verifies.
- `npm run build`: compilation Vite de production reussie.
- Parcours Playwright reel avec Spring Boot et PostgreSQL: connexion, dashboard, recherche, calendrier, mode sombre, profil et menu mobile.
- Viewports testes: 1440 x 1000, 900 x 1100 et 390 x 844.
- Aucun debordement horizontal de page et aucune erreur JavaScript pendant les parcours.
- Basculement francais/anglais verifie sur l'authentification et l'espace connecte.
- Backend: 11 tests executes, 0 echec; 2 tests Testcontainers ignores lorsque Docker Desktop est arrete.