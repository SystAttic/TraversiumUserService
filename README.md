# UserService

A microservice for managing users in the Traversium platform. This service handles user profiles, authentication, social features (followers/following), and integrates with other microservices in the ecosystem.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Service](#running-the-service)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Database](#database)
- [Integration](#integration)
- [Monitoring and Health](#monitoring-and-health)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)

## Features

### User Management
- User registration and profile management
- User search functionality
- User profile updates (display name, bio, photos, etc.)
- User deletion

### Social Features
- Follow/unfollow users
- Block/unblock users
- View followers and following lists
- Count followers and following

### Security
- Firebase Authentication integration
- JWT token validation
- Multi-tenancy support
- Tenant isolation

### Integration
- REST API and GraphQL endpoints
- gRPC communication with TripService and ModerationService
- Kafka event streaming for notifications and audit logs
- Prometheus metrics for monitoring

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Firebase project with service account credentials
- Kafka cluster (for event streaming)
- Docker (optional, for containerized deployment)

## Configuration

### Application Properties

The service is configured via `src/main/resources/application.properties`. Key configurations:

```properties
# Application
spring.application.name=UserService
server.port=8090

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/user_tenants_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# Config Server (optional)
spring.config.import=optional:configserver:http://localhost:8888

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/tenant

# Kafka
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.notification-topic=notification-topic
spring.kafka.audit-topic=audit-topic

# gRPC
grpc.trip.host=localhost
grpc.trip.port=9091
grpc.moderation.host=localhost
grpc.moderation.port=9090
```

### Kafka Configuration

Event streaming configuration for asynchronous communication:

- **`spring.kafka.bootstrap-servers`**: Kafka broker address for connecting to the Kafka cluster
- **`spring.kafka.notification-topic`**: Topic name for publishing notification events (user follows, etc.)
- **`spring.kafka.audit-topic`**: Topic name for publishing audit events (user actions tracking)

### gRPC Configuration

gRPC client configuration for inter-service communication:

**TripService Client:**
- **`grpc.trip.host`**: Hostname of the TripService gRPC server
- **`grpc.trip.port`**: Port of the TripService gRPC server (fetches user trip data)

**ModerationService Client:**
- **`grpc.moderation.host`**: Hostname of the ModerationService gRPC server
- **`grpc.moderation.port`**: Port of the ModerationService gRPC server (content moderation for user profiles)

### Resilience4j Configuration

Circuit breaker and retry mechanisms for gRPC calls:
```properties
resilience4j.circuitbreaker.instances.tripServiceGrpc.sliding-window-size=10
resilience4j.circuitbreaker.instances.tripServiceGrpc.failure-rate-threshold=50
resilience4j.retry.instances.tripServiceGrpc.max-attempts=3
```

- **`sliding-window-size`**: Number of calls to track for circuit breaker evaluation
- **`failure-rate-threshold`**: Percentage of failures that triggers circuit breaker to open (50%)
- **`max-attempts`**: Maximum retry attempts for failed gRPC calls
- **`wait-duration`**: Wait time between retry attempts (1 second)

## Running the Service

### Local Development

```bash
# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/UserService-1.2.0-SNAPSHOT.jar
```

### Using Docker

```bash
# Build Docker image
docker build -t traversium-user-service .

# Run container
docker run -p 8090:8090 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/user_tenants_db \
  traversium-user-service
```

### Verify Service is Running

```bash
# Health check
curl http://localhost:8090/actuator/health

# Liveness probe
curl http://localhost:8090/actuator/health/liveness

# Readiness probe
curl http://localhost:8090/actuator/health/readiness
```

## API Documentation

### REST API

Once the service is running, access the Swagger UI:

```
http://localhost:8090/swagger-ui.html
```

### Key Endpoints

**User Operations:**
- `POST /rest/v1/users` - Create user
- `GET /rest/v1/users?username={username}` - Get user by username
- `GET /rest/v1/users?email={email}` - Get user by email
- `PUT /rest/v1/users` - Update user
- `DELETE /rest/v1/users` - Delete user
- `GET /rest/v1/users/exists?username={username}` - Check if user exists
- `GET /rest/v1/users/search?query={query}` - Search users

**Social Features:**
- `POST /rest/v1/users/follow/{username}` - Follow user
- `POST /rest/v1/users/unfollow/{username}` - Unfollow user
- `GET /rest/v1/users/{username}/followers` - Get followers
- `GET /rest/v1/users/{username}/following` - Get following
- `POST /rest/v1/users/block/{username}` - Block user
- `POST /rest/v1/users/unblock/{username}` - Unblock user
- `GET /rest/v1/users/blocked` - Get blocked users

### GraphQL

GraphQL endpoint is available at:
```
http://localhost:8090/graphql
```

GraphiQL interface:
```
http://localhost:8090/graphiql
```

## Architecture

### Multi-Tenancy

The service implements schema-based multi-tenancy using the `common-multitenancy` library. Each tenant has an isolated database schema.

### Security

- **Firebase Authentication**: All requests must include a valid Firebase ID token in the Authorization header
- **Tenant Filter**: Extracts and validates tenant context from request headers
- **Principal**: User context is available via `TraversiumPrincipal` in secured endpoints

### Event-Driven Architecture

The service publishes events to Kafka:
- **User Events**: User creation, updates, deletion
- **Notification Events**: Trigger notifications for followers
- **Audit Events**: Track user actions for compliance

### Resilience Patterns

- **Circuit Breaker**: Prevents cascading failures when calling TripService
- **Retry**: Automatic retry for transient failures
- **Fallback**: Graceful degradation when dependencies are unavailable

## Database

### Schema Management

Database migrations are managed by Flyway. Migration scripts are located in:
```
src/main/resources/db/migration/tenant/
```

## Integration

### gRPC Clients

**TripService Client** (`src/main/kotlin/travesium/userservice/service/TripServiceGrpcClient.kt`):
- Fetches user trip data
- Protected by circuit breaker and retry

**ModerationService Client** (`src/main/kotlin/travesium/userservice/service/ModerationServiceGrpcClient.kt`):
- Content moderation for user profiles

### Kafka Integration

**Producers:**
- User events (topic: configured in KafkaProperties)
- Notification events
- Audit events

**Configuration:** See `src/main/kotlin/travesium/userservice/kafka/KafkaConfig.kt`

## Monitoring and Health

### Health Checks

- **Liveness**: `/actuator/health/liveness` - Indicates if the application is running
- **Readiness**: `/actuator/health/readiness` - Indicates if the application is ready to serve traffic
- **Database**: `/actuator/health/db` - Database connectivity check

### Metrics

Prometheus metrics exposed at:
```
http://localhost:8090/actuator/prometheus
```

Key metrics:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Database connection pool metrics
- Circuit breaker state
- Custom business metrics

### Logging

Logs are structured in JSON format (Logstash encoder) for ELK Stack integration:
- Application logs: Log4j2
- Request/response logging
- Error tracking
