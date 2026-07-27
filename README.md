# FYP Online Grading Platform

Plateforme bilingue français/anglais de gestion et de notation des Final Year Projects de Sultan Qaboos University.

Le dépôt contient le frontend React, le backend Spring Boot et toute l’infrastructure nécessaire pour démarrer l’application avec Docker Compose.

## Démarrage rapide avec Docker

### Prérequis

Installez uniquement :

- Git ;
- Docker Desktop avec Docker Compose.

### Installation

```bash
git clone https://github.com/ranym-eng/FYP-Online-Grading-Platform.git
cd FYP-Online-Grading-Platform
docker compose up --build -d
```

Le premier démarrage peut prendre quelques minutes, car Docker télécharge les images et compile les deux applications.

### URLs

| Service | URL |
| --- | --- |
| Application React | http://localhost:3000 |
| Backend Spring Boot | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| État du backend | http://localhost:8080/actuator/health |
| Mailpit | http://localhost:8025 |
| PostgreSQL depuis la machine | localhost:5433 |

Compte administrateur créé automatiquement :

```text
E-mail      : admin@squ.edu.om
Mot de passe: Admin@123
```

Ces identifiants sont réservés au développement. Changez-les avant tout déploiement réel.

## Commandes Docker utiles

Afficher l’état des services :

```bash
docker compose ps
```

Suivre les logs :

```bash
docker compose logs -f
```

Suivre uniquement Spring Boot :

```bash
docker compose logs -f backend
```

Arrêter les services en conservant PostgreSQL :

```bash
docker compose down
```

Reconstruire après une modification :

```bash
docker compose up --build -d
```

Supprimer les conteneurs et toutes les données PostgreSQL :

```bash
docker compose down -v
```

Attention : l’option `-v` supprime définitivement la base Docker.

## Configuration

Le projet fonctionne sans fichier `.env` grâce à des valeurs de développement par défaut.

Pour personnaliser les ports ou les identifiants :

```powershell
Copy-Item .env.example .env
```

Sous macOS ou Linux :

```bash
cp .env.example .env
```

Variables disponibles :

| Variable | Valeur par défaut |
| --- | --- |
| `POSTGRES_DB` | `fyp_grading_platform` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `root` |
| `POSTGRES_PORT` | `5433` |
| `BACKEND_PORT` | `8080` |
| `FRONTEND_PORT` | `3000` |
| `MAILPIT_SMTP_PORT` | `1025` |
| `MAILPIT_UI_PORT` | `8025` |

PostgreSQL utilise le port hôte `5433` afin de ne pas entrer en conflit avec une installation locale utilisant déjà `5432`. À l’intérieur de Docker, le backend communique avec PostgreSQL sur le port standard `5432`.

## Architecture du dépôt

```text
.
├── backend/                 Spring Boot 3.5, Java 21, Maven
├── frontend/                React 19, Vite et Nginx
├── compose.yaml             Orchestration complète
├── .env.example             Exemple de configuration
└── README.md                Documentation principale
```

Services Docker :

```text
Navigateur -> Nginx/React -> Spring Boot -> PostgreSQL
                                |
                                +----------> Mailpit
```

Nginx transmet automatiquement les requêtes `/api` au backend. Le frontend n’a donc pas besoin d’une adresse API codée en dur.

## Démarrage sans Docker

### Backend

Prérequis : Java 21 et PostgreSQL.

```powershell
cd backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/fyp_grading_platform"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="root"
.\mvnw.cmd spring-boot:run
```

### Frontend

Prérequis : Node.js 22.

```powershell
cd frontend
npm ci
npm run dev
```

Vite transmet automatiquement `/api` vers `http://localhost:8080` en développement.

## Tests

Backend :

```powershell
cd backend
.\mvnw.cmd test
```

Frontend :

```powershell
cd frontend
npm ci
npm run lint
npm run build
```

Les tests d’intégration Testcontainers nécessitent Docker actif.

## Données et e-mails

- Hibernate crée et met à jour le schéma PostgreSQL au démarrage.
- Les données Docker sont conservées dans le volume `postgres_data`.
- Les e-mails de développement sont capturés par Mailpit et visibles sur http://localhost:8025.
- Les secrets réels ne doivent jamais être ajoutés au dépôt. Le fichier `.env` est ignoré par Git.

## Dépannage

Si un port est déjà utilisé, modifiez sa valeur dans `.env`, puis relancez :

```bash
docker compose up --build -d
```

Pour vérifier la configuration Compose :

```bash
docker compose config
```

Pour repartir avec une base Docker vide :

```bash
docker compose down -v
docker compose up --build -d
```