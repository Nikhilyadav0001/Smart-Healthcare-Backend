# PostgreSQL Service (`db-postgres`) & Auth Service Data Layer Architecture

This document provides the full architectural context, operational guide, and service integration specifications for `infra/docker/dbs/postgres/compose.yml` and its role in powering the `auth` within the Smart Healthcare ecosystem.

---

## 1. Architectural Overview & Context

In the Smart Healthcare platform, each business microservice owns its private data persistence layer (**Database-per-Service** pattern). 

PostgreSQL 16 serves as the primary relational database for services requiring ACID guarantees, relational integrity, and strict access controls—specifically:
1. **`auth`** (`auth_db`): Handles identity, user credentials, JWT session tracking, and transactional outbox events.
2. **`prediction-service`** (`prediction_db`): Handles ML clinical risk predictions, model metadata, and inference audit logs.

```
+---------------------------------------------------------------------------------------+
|                                    Docker Network                                     |
|                                                                                       |
|   +-----------------------+              +----------------------------------------+   |
|   |     auth-service      |              |          prediction-service            |   |
|   |  (Spring Boot / Java) |              |             (Python/Go)                |   |
|   +-----------+-----------+              +-------------------+--------------------+   |
|               |                                              |                        |
|   JDBC:5432   | (auth_user)                      JDBC:5432   | (prediction_user)      |
|               v                                              v                        |
|   +-------------------------------------------------------------------------------+   |
|   |                              db-postgres:16                                   |   |
|   |                                                                               |   |
|   |  +-----------------------------+             +-----------------------------+  |   |
|   |  |           auth_db           |             |        prediction_db        |  |   |
|   |  |   (OWNER: auth_user)        |             |   (OWNER: prediction_user)  |  |   |
|   |  |   - users                   |             |   - ml_predictions          |  |   |
|   |  |   - refresh_tokens          |             |   - risk_assessments        |  |   |
|   |  |   - outbox_events           |             |                             |  |   |
|   |  +-----------------------------+             +-----------------------------+  |   |
|   +---------------------------------------+---------------------------------------+   |
|                                           |                                           |
+-------------------------------------------|-------------------------------------------+
                                            v
                             +-----------------------------+
                             | Named Volume: pgdata        |
                             | (/var/lib/postgresql/data)  |
                             +-----------------------------+
```

---

## 2. PostgreSQL Compose Specification

File location: `infra/docker/dbs/postgres/compose.yml`

```yaml
services:
  postgres:
    image: postgres:16
    container_name: db-postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      AUTH_DB_PASSWORD: ${AUTH_DB_PASSWORD}
      PREDICTION_DB_PASSWORD: ${PREDICTION_DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  pgdata:
```

### Key Highlights
* **Version:** PostgreSQL 16 (Debian-based official distribution).
* **Port Mapping:** `5432:5432` exposed to host for local microservice debugging.
* **Volume Persistence:** Named volume `pgdata` preserves database catalogs across restarts and upgrades.
* **Initialization Hook:** Mounts `./init:/docker-entrypoint-initdb.d:ro`. Scripts in `./init` execute sequentially on first volume initialization.
* **Healthcheck:** Periodically executes `pg_isready -U postgres`. Dependent compose configurations can use `condition: service_healthy`.

---

## 3. Database Initialization & Security Isolation

File location: `infra/docker/dbs/postgres/init/01-create-databases.sh`

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
  CREATE USER auth_user WITH PASSWORD '${AUTH_DB_PASSWORD}';
  CREATE DATABASE auth_db OWNER auth_user;

  CREATE USER prediction_user WITH PASSWORD '${PREDICTION_DB_PASSWORD}';
  CREATE DATABASE prediction_db OWNER prediction_user;

  REVOKE CONNECT ON DATABASE auth_db FROM PUBLIC;
  REVOKE CONNECT ON DATABASE prediction_db FROM PUBLIC;
EOSQL
```

### Security & Principle of Least Privilege:
1. **Isolated Role per Service:** `auth` connects solely as `auth_user`. It has zero rights over `prediction_db` or the default `postgres` catalog.
2. **Revocation of Public Access:** `REVOKE CONNECT ON DATABASE ... FROM PUBLIC` ensures standard PostgreSQL users cannot access auth credentials or patient prediction data.
3. **Environment Injection:** Passwords are not hard-coded; they are injected via environment variables (`${AUTH_DB_PASSWORD}`) sourced from `infra/docker/.env`.

---

## 4. Full Context: The Role of `auth`

### 4.1 Responsibilities
1. **User Authentication & Identity:** Validates login credentials (BCrypt/Argon2 password hashing) and issues JWT Access & Refresh Tokens.
2. **Registration & Role Management:** Registers patients, doctors, and clinic admins (`PATIENT`, `DOCTOR`, `ADMIN`).
3. **Saga Initiator (Event-Driven Onboarding):**
   * Emits the `user.registered` event to Kafka topic `user.registered`.
   * Follows the **Transactional Outbox Pattern** to ensure database mutation and Kafka messaging stay atomic.
4. **Account Recovery & Verification:** Coordinates email verification tokens via Mailpit (`tool-mailpit:1025`).

---

### 4.2 Data Schema for `auth_db`

The `auth` requires the following database tables (managed via Flyway migrations under `src/main/resources/db/migration/`):

#### 1. `users` Table
Stores authentication identity and lifecycle state:
```sql
CREATE TABLE users (
    id            CHAR(36)      NOT NULL PRIMARY KEY,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL, -- 'PATIENT', 'DOCTOR', 'ADMIN'
    status        VARCHAR(30)   NOT NULL DEFAULT 'PENDING_VERIFICATION', -- 'PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED'
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_email ON users(email);
```

#### 2. `refresh_tokens` Table
Maintains active refresh token families for token rotation & revocation:
```sql
CREATE TABLE refresh_tokens (
    id         CHAR(36)     NOT NULL PRIMARY KEY,
    user_id    CHAR(36)     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

#### 3. `outbox_events` Table (Transactional Outbox)
Guarantees dual-write consistency between Postgres and Kafka:
```sql
CREATE TABLE outbox_events (
    id           CHAR(36)      NOT NULL PRIMARY KEY,
    topic        VARCHAR(100)  NOT NULL,
    event_key    VARCHAR(100)  NOT NULL,
    payload      TEXT          NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ   NULL
);
CREATE INDEX idx_outbox_unpublished ON outbox_events(published_at, created_at);
```

#### 4. `processed_events` Table (Idempotent Consumer)
```sql
CREATE TABLE processed_events (
    event_id     CHAR(36)    NOT NULL PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

### 4.3 End-to-End User Onboarding Flow

```
[ Client ]
    │
    │ 1. POST /api/v1/auth/register { email, password, fullName, role }
    ▼
[ api-gateway:8080 ]
    │
    │ 2. Routes to auth-service
    ▼
[ auth-service:8081 ]
    │
    │ 3. Atomic Local Postgres Transaction (auth_db):
    │    ├── INSERT INTO users (id, email, password_hash, role, status='PENDING')
    │    └── INSERT INTO outbox_events (topic='user.registered', payload=JSON)
    ▼
( Transaction Committed to auth_db )
    │
    │ 4. Outbox Publisher poll / CDC
    ▼
[ Kafka Topic: user.registered ] (Partitioned by userId)
    │
    ├───────────────────────────────────────────────┐
    ▼                                               ▼
[ user-profile-service ]                     [ Notification Service ]
(Creates profile in profile_db)              (Sends verification email via Mailpit)
    │                                               │
    ├─ Success: emits user.profile.created          └─ verification link: /verify?token=...
    └─ Failure: emits user.profile.failed
```

---

## 5. Operations & Troubleshooting Guide

### Running and Managing PostgreSQL

```bash
# Sourcing environment variables from infra/docker/.env
cd infra/docker/dbs/postgres

# Start PostgreSQL detached
docker compose --env-file ../../.env up -d

# Check real-time logs
docker compose logs -f postgres

# Run automated platform healthcheck
bash ../../check.sh

# Teardown (keep volume)
docker compose down

# Wipe volume and restart from clean slate (DESTROYS ALL DATA)
docker compose down -v
docker compose --env-file ../../.env up -d
```

### CLI Inspection Commands

```bash
# Check if databases exist
docker exec db-postgres psql -U postgres -c "\l"

# Connect to auth_db using application credentials
docker exec -it db-postgres psql -U auth_user -d auth_db

# Query user records in auth_db
docker exec -it db-postgres psql -U auth_user -d auth_db -c "SELECT * FROM users;"

# Query unpublished outbox events
docker exec -it db-postgres psql -U auth_user -d auth_db -c "SELECT id, topic, created_at FROM outbox_events WHERE published_at IS NULL;"
```

---

## 6. Spring Boot Configuration for `auth`

In `services/auth-service/src/main/resources/application.yml` or Spring Cloud Config:

```yaml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: auth_user
    password: ${AUTH_DB_PASSWORD:root}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
```
