# Dexwin

A Spring Boot backend service for managing users and notes with role-based access, pagination, soft deletes, and JSON content storage. The project uses PostgreSQL, Spring Security (JWT), JPA/Hibernate, Flyway, and Swagger/OpenAPI.

## Features

- User authentication and authorization (JWT)
- Role-based access control (e.g., ADMIN, USER)
- Notes CRUD with:
  - JSON content (stored as serialized JSON)
  - Tags filtering and search
  - Pagination and sorting by last update
  - Soft delete and restore
  - Optimistic locking to prevent concurrent update conflicts
- API documentation via Swagger UI
- Database migrations via Flyway
- Docker Compose for local development

## Tech Stack

- Java 17+
- Spring Boot
- Spring Data JPA (Hibernate)
- Spring Security (JWT)
- PostgreSQL
- Flyway
- Maven
- JUnit/Mockito

## Getting Started

### Prerequisites

- Java 17 or newer
- Maven 3.9+
- Docker (optional, for running Postgres via docker-compose)

### Clone the Repository

```sh
git clone https://github.com/yourusername/dexwin.git
cd Dexwin
```

### Configure Environment Variables

You can use either `.env` (preferred for local dev) or `src/main/resources/application.yml`.

Sample variables are in `sample.env`. Copy and adapt them:

```sh
cp sample.env .env
```

Key variables:
- SERVER_PORT=8080
- DB_HOST=localhost
- DB_PORT=5432
- DB_NAME=dexwin
- DB_USERNAME=postgres
- DB_PASSWORD=postgres
- JWT_SECRET=your-very-strong-secret

If using `application.yml`, ensure the same values are configured under `spring.datasource` and any security properties.

### Start Dependencies with Docker (optional)

If you prefer containers, a PostgreSQL service is defined in `docker-compose.yaml`.

```sh
docker compose up -d
```

### Build and Run

- Build:
```sh
mvn clean install
```

- Run:
```sh
mvn spring-boot:run
```

The app will start at: http://localhost:${SERVER_PORT:-8080}

### API Documentation

- Swagger UI: http://localhost:${SERVER_PORT:-8080}/swagger-ui/index.html
- OpenAPI JSON: http://localhost:${SERVER_PORT:-8080}/v3/api-docs
- Note: If the app is deployed behind a proxy or with a custom context path, adjust the base URL accordingly.

### Running Tests

```sh
mvn test
```

### Packaging

Create a runnable jar:
```sh
mvn -DskipTests package
```
The artifact will be under `target/`.



## Project Structure

- `src/main/java/com/clement/dexwin` – application source code
  - `web` – controllers (e.g., `AuthController`)
  - `domain` – models, DTOs, repositories, services
  - `config` – Spring and security configuration
  - `exceptions` – custom exceptions
  - `utils` – helper classes/constants
- `src/main/resources` – `application.yml`, Flyway migrations (`db/migration`), static/templates
- `src/test/java` – unit and integration tests

## Common Endpoints

- Auth: `/api/auth/**`
- Notes: `/api/notes/**`

Note: Exact paths may vary; check Swagger UI for authoritative documentation.

## Notes Domain Highlights

- Content is stored as JSON. The service serializes/deserializes using Jackson (`ObjectMapper`).
- List endpoints support pagination, search, and tag filters.
- Soft delete sets `deletedAt`; restore is supported.
- Optimistic locking resolves concurrent updates with clear conflict messages.

## Troubleshooting

- Database connection errors:
  - Verify Postgres is running and credentials match `.env`/`application.yml`.
  - If using Docker, run `docker compose ps` to confirm the container is healthy.
- Swagger not loading:
  - Ensure the app started successfully and port is not in use.
- 409 Conflict during note update:
  - Another client modified the note. Reload the note and retry.

## Run

- Local (Dev):
  - mvn spring-boot:run
- Local (Jar):
  - mvn -DskipTests package
  - java -jar target/Dexwin-0.0.1-SNAPSHOT.jar
- With Docker:
  - docker build -t dexwin:local .
  - docker run --rm -p 8080:8080 --env-file .env dexwin:local

## Test

- Run unit tests:
  - mvn test
- Generate coverage (JaCoCo):
  - mvn test
  - Then open target/site/jacoco/index.html in your browser for the coverage report.

## Swagger/OpenAPI

- Swagger UI: http://localhost:${SERVER_PORT:-8080}/swagger-ui/index.html
- OpenAPI JSON: http://localhost:${SERVER_PORT:-8080}/v3/api-docs

## Dockerfile

A production-ready multi-stage Dockerfile is included at the project root. It builds the jar and runs it on a minimized JRE base image. See the Run section above for usage.

## Flyway Scripts

- Migrations live under: src/main/resources/db/migration
- Naming convention: V<version>__<description>.sql (e.g., V1__init.sql)
- Flyway runs automatically on application startup.

## Decisions

- Authentication: JWT-based stateless security via Spring Security.
- Data store: PostgreSQL with JPA/Hibernate.
- Migrations: Flyway for deterministic schema changes.
- API Docs: springdoc-openapi with Swagger UI enabled.
- Domain modeling: Notes content stored as JSON; soft-deletes implemented; optimistic locking enabled to handle concurrent updates.
- Testing: JUnit + Mockito; code coverage via JaCoCo.

## License
This project is proprietary unless otherwise noted by the repository owner. Update this section if you intend to open-source it.









