# 01: Foundation

**What to build:** A running Spring Boot application on a real PostgreSQL with the whole domain schema in place, seed Users to log in with later, consistent `ProblemDetail` errors, and a shared Testcontainers test base that every later ticket reuses. Layers are `api` / `service` / `dao`. Follows ADR 0001 (Java 21, Boot 4.1) and the vocabulary in `GLOSSARY.md`.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Dependencies added: JPA, PostgreSQL driver, Flyway, security, JWT support, springdoc, Testcontainers
- [x] Flyway migrations create User, Account (balance and Initial deposit as `NUMERIC(19,2)`, balance >= 0 check), Email and Phone (globally unique values), and Transfer
- [x] A migration seeds several Users with BCrypt password hashes, emails, phones, Accounts and Initial deposits
- [x] Global exception handling returns RFC 9457 `ProblemDetail` with a stable `type` and a field-error list for validation failures
- [x] A reusable Testcontainers base test class starts Postgres once and runs the migrations
- [x] A test proves the application context starts and the seeded Users and Accounts are readable through the DAO
- [x] `./mvnw verify` passes
