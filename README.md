# Traversium User Service

A microservice for managing user profiles and social features within the Traversium application.

## Table of Contents

- [About](#about)
- [Features](#features)
- [Technologies](#technologies)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Docker](#docker)
- [Development](#development)

## About

**Traversium User Service** is a microservice that is part of the larger Traversium application. The service is responsible for:

- Managing user accounts and profiles
- User authentication and authorization via Firebase
- Social features (following users, blocking)
- Multi-tenant support
- Event-driven communication with other microservices via Kafka

The service is implemented in **Kotlin** and built on the **Spring Boot** framework with support for both REST and GraphQL APIs.

## Features

### User Management
- Create new user accounts
- Retrieve user profiles (by username or email)
- Update profile (display name, avatar, bio, etc.)
- Delete user account
- Check user existence

### Social Features
- Follow/unfollow users
- View followers and following lists (with pagination)
- Block/unblock users
- Manage blocked users list

### Integrations
- Firebase authentication with JWT tokens
- Kafka integration for event publishing (notifications)
- Multi-tenant support via shared library

### APIs
- RESTful API endpoints
- GraphQL API
- Swagger/OpenAPI documentation

## Technologies

### Backend Framework
- **Spring Boot** 3.5.6
- **Kotlin** 1.9.25
- **Java** 17

### Database
- **PostgreSQL** - primary database
- **Hibernate** 6.4.4 - ORM
- **Flyway** - database migrations

### Security & Authentication
- **Spring Security**
- **Firebase Admin SDK** 9.7.0

### API & Communication
- **Spring Web** - REST API
- **Spring GraphQL** - GraphQL support
- **Swagger/OpenAPI** 2.2.39
- **Apache Kafka** - message broker

### Additional
- **Log4j 2** - logging
- **Spring Boot Actuator** - monitoring
- **JUnit 5** - testing

## Prerequisites

Before installing and running the application, you need:

- **JDK 17** or newer
- **Maven 3.6+**
- **PostgreSQL** database
- **Apache Kafka** (optional, for event-driven functionality)
- **Firebase** project with service account credentials

## Installation

### 1. Clone the repository

```bash
git clone <repository-url>
cd UserService
```

### 2. Configure the database

Create a PostgreSQL database:

```sql
CREATE DATABASE user_tenants_db;
CREATE USER <username> WITH PASSWORD '<password>';
GRANT ALL PRIVILEGES ON DATABASE user_tenants_db TO <username>;
```

### 3. Configure Firebase

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Generate a Service Account key (JSON file)
3. Save the file as `conf/traversium.json`

### 4. Update configuration files

Edit `src/main/resources/application.properties` or `conf/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/user_tenants_db
spring.datasource.username=<your_username>
spring.datasource.password=<your_password>

# Kafka - not necessary for local testing but required for full functionality
spring.kafka.bootstrap-servers=<your_kafka_servers>
spring.kafka.notification-topic=<your_notification_topic>

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/tenant
```

### 5. Build the project

```bash
# Full build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

## Running the Application

### Local Run

```bash
# With Maven
mvn spring-boot:run

# Or with JAR file
java -jar target/UserService-1.1.0-SNAPSHOT.jar
```

The application will be available at: `http://localhost:8090`

### Docker Run

```bash
docker run -p 8090:8090 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/user_db \
  -e DATABASE_USERNAME=user \
  -e DATABASE_PASSWORD=password \
  ghcr.io/<your-username>/user-service:latest
```

## API Documentation

### REST API Endpoints

All REST endpoints are accessible under `/rest/v1/users`

#### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/rest/v1/users` | Create a new user |
| `GET` | `/rest/v1/users?username={username}` | Get user by username |
| `GET` | `/rest/v1/users?email={email}` | Get user by email |
| `PUT` | `/rest/v1/users` | Update current user profile |
| `DELETE` | `/rest/v1/users` | Delete current user account |
| `GET` | `/rest/v1/users/exists` | Check if user exists |
| `POST` | `/rest/v1/users/userList` | Get multiple users (paginated) |

#### Following

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/rest/v1/users/follow/{username}` | Follow a user |
| `POST` | `/rest/v1/users/unfollow/{username}` | Unfollow a user |
| `GET` | `/rest/v1/users/{username}/followers` | Get user's followers |
| `GET` | `/rest/v1/users/{username}/followers/count` | Count followers |
| `GET` | `/rest/v1/users/{username}/following` | Get users being followed |
| `GET` | `/rest/v1/users/{username}/following/count` | Count following |

#### Blocking

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/rest/v1/users/block/{username}` | Block a user |
| `POST` | `/rest/v1/users/unblock/{username}` | Unblock a user |
| `GET` | `/rest/v1/users/blocked` | Get list of blocked users |
| `GET` | `/rest/v1/users/blocked/count` | Count blocked users |

### GraphQL API

GraphQL endpoint: `http://localhost:8090/graphql`

Example query:

```graphql
query {
  user(username: "johndoe") {
    userId
    username
    email
    displayName
    description
    avatarPhotoReference
    coverPhotoReference
  }
}
```

### Swagger UI

Interactive API documentation is available at:
- **Swagger UI**: `http://localhost:8090/swagger-ui.html`
- **OpenAPI spec**: `http://localhost:8090/v3/api-docs`

## Configuration

### Application Settings

Main configuration properties (`application.properties`):

```properties
# Server
server.port=8090

# Application
spring.application.name=UserService

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/user_tenants_db
spring.datasource.username=<DATABASE_USERNAME>
spring.datasource.password=<DATABASE_PASSWORD>
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update

# Kafka
spring.kafka.bootstrap-servers=<KAFKA_BOOTSTRAP_SERVERS>
spring.kafka.notification-topic=<NOTIFICATION_TOPIC>

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/tenant
```

### Environment Variables

| Variable                  | Description                   |
|---------------------------|-------------------------------|
| `DATABASE_URL`            | JDBC connection string        |
| `DATABASE_USERNAME`       | Database username             |
| `DATABASE_PASSWORD`       | Database password             |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers                 |
| `NOTIFICATION_TOPIC`      | Kafka topic for notifications |

## Project Structure

```
UserService/
├── src/
│   ├── main/
│   │   ├── kotlin/traversium/userservice/
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── db/                  # Entities and repositories
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── event/               # Event listeners (Kafka)
│   │   │   ├── exceptions/          # Exception classes
│   │   │   ├── graphql/             # GraphQL controllers
│   │   │   ├── kafka/               # Kafka configuration
│   │   │   ├── mapper/              # DTO/Entity mappers
│   │   │   ├── rest/                # REST API controllers
│   │   │   ├── security/            # Security filters
│   │   │   ├── service/             # Business logic
│   │   │   ├── swagger/             # Swagger configuration
│   │   │   └── UserServiceApplication.kt
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── graphql/schema.graphqls
│   │       └── db/migration/tenant        # Flyway migrations
│   └── test/kotlin/                 # Tests
├── conf/                            # Runtime configuration
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Docker image
├── CHANGELOG.md                     # Version history
└── README.md                        # This document
```

## Testing

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=UserServiceTest

# Integration tests
mvn verify
```

### Test Coverage

The project includes:
- **Unit tests** for service logic
- **Mock tests** with Mockito Kotlin

## Docker

### Building Docker Image

```bash
# Simple build
docker build -t user-service:latest .

# Multi-platform build (amd64, arm64)
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t user-service:latest \
  .
```

### Docker Compose

Example `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: user_tenants_db
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"

  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "29092:29092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092

  user-service:
    build: .
    ports:
      - "8090:8090"
    depends_on:
      - postgres
      - kafka
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/user_tenants_db
      DATABASE_USERNAME: user
      DATABASE_PASSWORD: password
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

Run:
```bash
docker-compose up -d
```

## Development

### CI/CD

The project uses **GitHub Actions** for CI/CD:

- **docker-image.yml**: Automatic build and publish Docker images for SNAPSHOT versions
- **release.yml**: Release workflow for production versions
- Multi-platform support (amd64, arm64)
- Published to GitHub Container Registry (ghcr.io)

### Business Rules

- Users cannot follow themselves
- Users cannot follow blocked users
- Blocking automatically removes mutual following
- Following triggers a notification via Kafka
- User deletion marks the account as deleted (soft delete)

### Dependencies

The project uses custom dependencies:
- `common-multitenancy:1.1.0` - Multi-tenant support
- `notification-models:1.1.0` - Notification models

## Database Schema

### Main Tables

```sql
-- Core user table
user_table (
  user_id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE,
  email VARCHAR(255) UNIQUE,
  firebase_id VARCHAR(255) UNIQUE,
  display_name VARCHAR(255),
  description TEXT,
  avatar_photo_reference VARCHAR(255),
  cover_photo_reference VARCHAR(255),
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  country_of_origin VARCHAR(255),
  gender VARCHAR(255),
  created_at TIMESTAMP,
  deleted BOOLEAN
)

-- Following relationships
user_followers (
  follower_id BIGINT (FK),
  followed_id BIGINT (FK),
  PRIMARY KEY (follower_id, followed_id)
)

-- Blocked users
blocked (
  user_id BIGINT (FK),
  blocked_user__id BIGINT (FK),
  PRIMARY KEY (user_id, blocked_user__id)
)
```

**Notes:**
- For more information on Firebase configuration, see [Firebase documentation](https://firebase.google.com/docs/admin/setup)
- For Kafka setup, see [Apache Kafka documentation](https://kafka.apache.org/documentation/)
