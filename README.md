# Suivi Économique des Projets

Application web fullstack de suivi des **coûts, revenus et marges** des projets, avec détection des projets critiques et notifications en temps réel.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Angular](https://img.shields.io/badge/Angular-17.3-red)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)

## Sommaire

1. [Fonctionnalités](#fonctionnalités)
2. [Stack technique](#stack-technique)
3. [Architecture](#architecture)
4. [Prérequis](#prérequis)
5. [Installation](#installation)
6. [Configuration](#configuration)
7. [Lancement](#lancement)
8. [API REST](#api-rest)
9. [Sécurité](#sécurité)
10. [Temps réel (WebSocket)](#temps-réel-websocket)
11. [Tests](#tests)
12. [Dépannage](#dépannage)
13. [Licence](#licence)
14. [Auteur](#auteur)

## Fonctionnalités

- Authentification par **JWT** (login, déconnexion)
- Gestion des rôles : **Admin** et **Manager**
- Réinitialisation du mot de passe par **code OTP envoyé par email**
- Gestion des projets : coûts, revenus, marges
- **Détection automatique des projets critiques** (marge faible ou négative)
- **Tableau de bord temps réel** (WebSocket + STOMP)
- **Notifications en direct** lors d'un changement d'état d'un projet

## Stack technique

| Couche | Technologie | Version |
|---|---|---|
| Langage backend | Java (JDK) | 17 |
| Framework backend | Spring Boot | 3.2.5 |
| Persistance | Spring Data JPA / Hibernate | 6.4.x |
| Pool de connexions | HikariCP | 5.1.x |
| Sécurité | Spring Security | 6.2.x |
| JWT | jjwt | 0.12.5 |
| Email | spring-boot-starter-mail | 3.2.5 |
| Temps réel | spring-boot-starter-websocket (STOMP) | 3.2.5 |
| Base de données | MySQL | 8.0 |
| Build backend | Maven | 3.9.6 |
| Frontend | Angular | 17.3 |
| UI | Angular Material | 17.3 |
| Graphiques | Chart.js | 4.4.2 |
| Client STOMP | @stomp/stompjs | 7.0.0 |
| Runtime frontend | Node.js / npm | 20 LTS / 10 |

## Architecture

```
Fullstack-Web-App/
├── backend/                         # API Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/project/
│       │   ├── ProjectApplication.java
│       │   ├── config/              # Sécurité, CORS, WebSocket
│       │   ├── controller/
│       │   │   └── auth/            # LoginController
│       │   ├── DTO/
│       │   │   └── auth/            # LoginRequest, ...
│       │   ├── entity/              # Entités JPA
│       │   ├── repository/          # Interfaces Spring Data
│       │   ├── service/             # Logique métier
│       │   └── security/            # Filtre JWT, UserDetails
│       └── resources/
│           └── application.properties
└── frontend/                        # Application Angular
    ├── package.json
    └── src/app/
        ├── core/                    # Guards, intercepteurs, services
        ├── features/                # Modules : auth, projets, dashboard
        └── shared/                  # Composants communs
```

## Prérequis

- JDK 17
- Maven 3.9.6
- MySQL 8.0
- Node.js 20 LTS et npm
- Angular CLI 17.3 : `npm install -g @angular/cli@17.3.0`
- Un compte SMTP pour l'envoi des OTP (Gmail, Mailtrap, ...)

## Installation

```bash
git clone https://github.com/korachia1KA/expense-project.git
cd expense-project
```

### Base de données

```sql
CREATE DATABASE expense_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'expense_user'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON expense_db.* TO 'expense_user'@'localhost';
FLUSH PRIVILEGES;
```

### Backend

```bash
cd backend
mvn clean install
```

### Frontend

```bash
cd frontend
npm install
```

## Configuration

### backend/pom.xml

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
</parent>

<properties>
    <java.version>17</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### backend/src/main/resources/application.properties

```properties
# Serveur
server.port=8080

# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/expense_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=expense_user
spring.datasource.password=change_me

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# JWT
app.jwt.secret=<chaine-base64-d-au-moins-256-bits>
app.jwt.expiration-ms=86400000

# Email (OTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<votre-email>
spring.mail.password=<mot-de-passe-application>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.otp.expiration-minutes=10

# CORS
app.cors.allowed-origins=http://localhost:4200
```

Ne commit jamais `application.properties` avec de vrais secrets. Utilise des variables d'environnement ou un fichier `application-local.properties` ajouté au `.gitignore`.

### frontend/src/environments/environment.ts

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'http://localhost:8080/ws'
};
```

## Lancement

### Backend

```bash
cd backend
mvn spring-boot:run
```

API disponible sur `http://localhost:8080`.

### Frontend

```bash
cd frontend
ng serve
```

Application disponible sur `http://localhost:4200`.

### Build de production

```bash
# Backend
cd backend && mvn clean package
java -jar target/*.jar

# Frontend
cd frontend && ng build --configuration production
```

## API REST

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| POST | `/api/auth/login` | Connexion, retourne un JWT | Public |
| POST | `/api/auth/forgot-password` | Envoie un OTP par email | Public |
| POST | `/api/auth/verify-otp` | Vérifie le code OTP | Public |
| POST | `/api/auth/reset-password` | Définit un nouveau mot de passe | Public |
| GET | `/api/projects` | Liste des projets | Admin, Manager |
| GET | `/api/projects/{id}` | Détail d'un projet | Admin, Manager |
| POST | `/api/projects` | Création d'un projet | Admin, Manager |
| PUT | `/api/projects/{id}` | Modification d'un projet | Admin, Manager |
| DELETE | `/api/projects/{id}` | Suppression d'un projet | Admin |
| GET | `/api/projects/critical` | Projets critiques | Admin, Manager |
| GET | `/api/users` | Gestion des utilisateurs | Admin |

Exemple de connexion :

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"motdepasse"}'
```

Les endpoints protégés demandent l'en-tête `Authorization: Bearer <token>`.

## Sécurité

- Mots de passe hachés avec **BCrypt**
- Authentification **stateless** par JWT
- Autorisation par rôle (`ROLE_ADMIN`, `ROLE_MANAGER`) via `@PreAuthorize`
- Codes OTP à durée de vie limitée, usage unique
- CORS restreint à l'origine du frontend

## Temps réel (WebSocket)

- Endpoint de connexion : `/ws` (avec fallback SockJS)
- Préfixe applicatif : `/app`
- Topics :
  - `/topic/dashboard` : mises à jour du tableau de bord
  - `/topic/notifications` : alertes (projet critique, changement de marge)

## Tests

```bash
# Backend
cd backend && mvn test

# Frontend
cd frontend && ng test
```

## Dépannage

### `NoClassDefFoundError: com/example/project/DTO/auth/LoginRequest`

1. Lancer `mvn clean install` pour supprimer les classes compilées obsolètes.
2. Vérifier la casse du package : la ligne `package` de `LoginRequest.java` doit correspondre exactement au dossier (`DTO` et non `dto`), et les imports aussi. Sous Windows, une casse différente compile mais échoue au démarrage.
3. Vérifier que `LoginRequest.java` existe dans `src/main/java/com/example/project/DTO/auth/`.

### `Cannot find template location: classpath:/templates/`

Warning sans gravité pour une API REST. Pour le masquer :
`spring.thymeleaf.check-template-location=false`

### Erreur de connexion MySQL

Vérifier que MySQL tourne, que la base `expense_db` existe et que l'utilisateur et le mot de passe correspondent à `application.properties`.

### Erreur CORS depuis Angular

Vérifier que `app.cors.allowed-origins` contient `http://localhost:4200`.

### L'email OTP n'arrive pas

Avec Gmail, il faut un **mot de passe d'application** (validation en deux étapes activée), pas le mot de passe du compte.

## Licence

Projet de stage d'été 2024. Tous droits réservés.

## Auteur

**Abdelghafour Korachi** : [GitHub](https://github.com/korachia1KA)