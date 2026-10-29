# test-proj-mesh

Banking-style REST API: users with contacts, JWT login, search, scheduled interest, money transfers.
Java 21, Spring Boot 4.1, Maven, PostgreSQL, Spring Data JPA, Flyway, Spring Security (JWT).
Java 21 instead of the task's Java 11 is deliberate, see `docs/adr/0001-java-21-spring-boot-4.md`.
Original task: `.scratch/banking-api/requirements.md`. Spec: `.scratch/banking-api/spec.md`. All 9 tickets are done.
Domain vocabulary: `GLOSSARY.md` (User, Account, Balance, Initial deposit, Interest accrual, Interest cap, Transfer, Contact).

## Commands

```bash
docker compose up -d                                        # local PostgreSQL (db/user/password: bank) and Redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=local     # run the app (profile: application-local.yml)
./mvnw verify                                               # all tests; needs Docker or Podman for Testcontainers
./mvnw test -Dtest=TransferServiceTest                      # one test class
./mvnw checkstyle:check                                     # lint only (also runs in validate, fails the build)
```

`verify` writes the JaCoCo coverage report to `target/site/jacoco/index.html`.
Checkstyle rules live in `config/checkstyle/checkstyle.xml` (4-space indent, 120 columns, no unused imports); fix the code, don't loosen the rules.

Outside the `local` profile the app needs `JWT_SECRET` (>= 32 bytes); `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT` are optional.
Swagger UI: `/swagger-ui.html`. Seed users (password `password123`): alice@example.com, bob@example.com, alina@example.com.

On this machine Docker runs through Podman: if `docker ps` cannot connect, run `podman machine start`.

## Layout (`src/main/java/com/example/testprojmesh`)

Three layers, each split by role:

- `api/` controller, dto, error (`ApiException` carrying an `HttpStatus`, `ProblemResponses`, `GlobalExceptionHandler`; RFC 9457 problem responses)
- `service/` one interface per service (`AuthService`, `UserService`, `EmailService`/`PhoneService` over `ContactService`, `TransferService`, `InterestService`); `service/impl` holds the `*Impl` classes with the business rules and transactions; `service/model` holds the view records. Callers and tests depend on the interfaces; only unit tests construct an `*Impl` directly
- `dao/` entity, repository (Spring Data JPA; `UserSpecifications` for search)
- `security/` JWT config and `TokenService` (token carries only `USER_ID`); `config/` OpenAPI, scheduling, interest properties
- `scheduler/` `InterestScheduler`, the 30-second job that calls `InterestService.accrue()`; `util/` `ContactNormalizer` (email lowercasing, phone digits)

The schema is owned by Flyway migrations in `src/main/resources/db/migration` (never use `ddl-auto` to change it).

## Rules that are easy to break

- Money is `BigDecimal` scale 2 / `NUMERIC(19,2)`; never `double`. Rounding is half-even (`round_half_even` SQL function).
- Transfers lock both Accounts with `lockByUserIds` (`FOR UPDATE`, ascending account id) inside one transaction; keep the interest `UPDATE` locking in the same order (ADR 0002).
- User resources are addressed as `/api/users/{userId}` and `/api/users/{userId}/emails|phones`. Writes are owner-only: `SecurityConfig.ownerOnly` refuses a `{userId}` other than the token's `USER_ID` with 403 (ADR 0003), and a new owner-only path must be added to that matcher.
- The sender of a Transfer, and the owner passed to any Contact service, still comes only from the JWT (`TokenService.userId`), never from the path or a request body. The Balance is returned only on the caller's own profile.
- Emails are stored lowercased, phones as digits only (10 to 15); both are unique across all Users and each User keeps at least one of each.
- Errors go through `ApiException` (400 invalid, 401, 403 not the owner, 404, 409 conflict, 422 unprocessable); security filters write theirs with `ProblemResponses.write` from an `HttpStatus`. The problem `type` is derived from the status (`urn:problem:not-found`, `bad-request`, `unprocessable-content`, ...), never hand-written. Do not log tokens, passwords or Balances.
- Caching (ADR 0004, `config/CacheConfig`): Redis caches only `userProfiles` (another User's profile, no Balance, via `PublicProfileReader`) and `userSearch`. Every Contact write must evict both (`AbstractContactService`). Never cache Balances, Accounts, Transfers or the owner's own profile; every cache needs a TTL. Interest runs on a single instance, no distributed lock.

## Testing

- API tests extend `ApiIntegrationTest` (MockMvc + Testcontainers Postgres and Redis; every cache is cleared before each test); create users with `TestUsers`, one fresh user per test, never rely on seed balances beyond `LoginAndProfileTest`/`SeedDataTest`.
- Tests that call interest accrual use their own context (`@TestPropertySource(properties = "test.context=...")`) because accrual touches every account.
- The scheduler is off in tests (`app.interest.enabled=false`); call `InterestService.accrue()` directly instead of waiting 30 seconds.
- Use the `tdd` skill, test only at the agreed seams: API (MockMvc), transfer service unit tests, concurrency test, accrual service.
- Caching is tested from outside in `CachingTest`: a change written with `JdbcTemplate` stays invisible while cached, and a Contact write through the API makes it visible.

## Agent skills

`.claude/skills/` is git-ignored 

### Issue tracker

Issues and specs are local markdown under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
