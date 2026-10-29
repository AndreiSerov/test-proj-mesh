# Banking API

A REST service where registered users hold one account, watch its balance grow through
periodic interest, manage their contacts, search other users and send money to them.
Domain vocabulary lives in [`GLOSSARY.md`](GLOSSARY.md).

**Stack:** Java 21, Spring Boot 4.1, Maven, PostgreSQL, Spring Data JPA, Flyway, Spring Security (JWT).
This deliberately deviates from the task's "Java 11" (which pins Spring Boot 2.7, end of life), see
[ADR 0001](docs/adr/0001-java-21-spring-boot-4.md).

Layers: `api` (controllers, error mapping) -> `service` (rules, transactions) -> `dao` (JPA repositories).

## Run

### Local development

```bash
docker compose up -d                                  # PostgreSQL on :5432 (db/user/password: bank), Redis on :6379
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile (`application-local.yml`) points at the compose database and sets a development-only
JWT secret. In an IDE, set the run configuration's active profile to `local`
(`SPRING_PROFILES_ACTIVE=local`). `docker compose down -v` wipes the data.

### Other environments

Without the `local` profile, configure the app through environment variables:

```bash
JWT_SECRET='change-me-to-at-least-32-random-bytes!!' ./mvnw spring-boot:run
```

| Variable | Default | Meaning |
|----------|---------|---------|
| `JWT_SECRET` | none, required (>= 32 bytes) | HS256 signing key |
| `DB_URL` | `jdbc:postgresql://localhost:5432/bank` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `bank` / `bank` | database credentials |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | cache; the API keeps working (uncached) if Redis is down |

Swagger UI: <http://localhost:8080/swagger-ui.html> (use *Authorize* with the token from login).

### Seed users

Created by migrations; all share the password `password123`.

| Name | Email(s) | Phone(s) | Balance |
|------|----------|----------|---------|
| Alice Johnson | alice@example.com, alice.j@example.com | 79990000001 | 1000.00 |
| Bob Smith | bob@example.com | 79990000002, 79990000012 | 500.00 |
| Alina Petrova | alina@example.com | 79990000003 | 250.00 |

## API

| Endpoint | Purpose |
|----------|---------|
| `POST /api/auth/login` | `{identifier, password}` (email or phone) -> `{token}`; JWT carries only `USER_ID`, expires in 1 hour |
| `GET /api/users/{userId}` | any user's profile; the balance only on your own |
| `POST` `/api/users/{userId}/emails`, `PUT` `DELETE` `/api/users/{userId}/emails/{id}` | manage own emails (403 for another user's id) |
| `POST` `/api/users/{userId}/phones`, `PUT` `DELETE` `/api/users/{userId}/phones/{id}` | manage own phones (403 for another user's id) |
| `GET /api/users/search` | filters `dateOfBirth` (born after), `phone`, `name` (prefix), `email`; `page`, `size` (default 20, max 100) |
| `POST /api/transfers` | `{toUserId, amount}` from the authenticated user |

Errors follow RFC 9457 (`application/problem+json`) with a stable `type` such as `urn:problem:conflict`;
validation failures list each field under `errors`.

## Rules worth knowing

- **Ownership:** anything under `/api/users/{userId}/` that changes data is allowed only when `{userId}` is your own token's id, otherwise 403 ([ADR 0003](docs/adr/0003-owner-id-in-path.md)).
- **Contacts** are unique across users; emails are lowercased, phones stored as digits (10 to 15). A user always keeps at least one email and one phone.
- **Interest:** every 30 seconds each balance grows by 10%, capped at 207% of the initial deposit (half-even rounding to 2 decimals). A balance above the cap is left alone; one below it resumes earning. Runs on a single instance, no distributed lock.
- **Transfers** lock both accounts in ascending id order in one transaction ([ADR 0002](docs/adr/0002-pessimistic-ordered-locks-for-transfers.md)), so balances never go negative and money is conserved under concurrency. Each transfer is stored.
- **Login** has no throttling or lockout (out of scope for the task). Failed logins cost the same BCrypt work whether or not the identifier exists.
- **Caching** ([ADR 0004](docs/adr/0004-redis-cache-for-read-mostly-user-data.md)): Redis caches other users' profiles (10 min) and search pages (1 min); any contact change evicts them. Balances, accounts and transfers are never cached: stale reads on money cost more than they save.

## Test

```bash
./mvnw verify
```

Needs a container runtime for Testcontainers (Docker or Podman). Highlights: API tests with MockMvc over real
PostgreSQL, unit tests for transfer logic, a concurrency test (parallel opposing transfers; verified to fail
without the row locks) and interest accrual tests that call the service directly instead of waiting 30 seconds.

`verify` also runs Checkstyle (`config/checkstyle/checkstyle.xml`, fails the build on any violation) and writes a
JaCoCo coverage report to `target/site/jacoco/index.html`. Lint alone: `./mvnw checkstyle:check`.
