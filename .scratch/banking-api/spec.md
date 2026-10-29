Status: done

# Banking API

## Problem Statement

Registered Users need a small, trustworthy service in which each of them holds exactly one Account, watches its Balance grow through periodic Interest accrual, keeps their own Contacts up to date, finds other Users, and sends money to them by Transfer. Money handling must be correct under concurrency: a Balance must never go negative and money must never be created or destroyed by a Transfer, even when many Transfers and Interest accruals run at once.

## Solution

A REST API (Spring Boot, PostgreSQL, Maven) with three layers: API, service, and DAO. Users sign in with an email or a phone plus a password and receive a JWT whose only claim is the User id. A signed-in User can view their own profile and Balance at `/api/users/{userId}` (other Users' profiles without the Balance), add, change and delete their own emails and phones, search other Users with filters and pagination, and make Transfers to other Users. A scheduled job applies Interest accrual to every Balance every 30 seconds. Users themselves are provisioned by database migrations and are never created through the API.

## User Stories

### Authentication
1. As a User, I want to log in with my email and password, so that I can use the API without sharing my password on every call.
2. As a User, I want to log in with my phone and password, so that I can use whichever Contact I remember.
3. As a User, I want a generic "invalid credentials" response when login fails, so that nobody can discover which emails or phones are registered.
4. As a User, I want my token to carry only my User id, so that nothing sensitive is exposed if the token is inspected.
5. As a User, I want my token to expire after one hour, so that a leaked token is only briefly useful.
6. As a User, I want every endpoint except login, API documentation and health to reject requests without a valid token, so that my data and money stay private.
7. As a User, I want an expired, malformed or tampered token to be rejected with 401, so that forged tokens never work.
8. As an operator, I want the JWT signing secret and the password hashes to be kept out of logs and responses, so that credentials never leak.

### Profile and Account
9. As a User, I want to view my own profile (name, date of birth, emails, phones), so that I can check what the service knows about me.
10. As a User, I want to see my current Balance in my profile, so that I can follow how Interest accrual and Transfers affect it.
11. As a User, I want exactly one Account, so that there is no ambiguity about where my money is.
12. As an operator, I want each Account to start with an Initial deposit set at creation, so that the Interest cap has a fixed reference.
13. As a User, I want my name, date of birth and password to be read-only through the API, so that they cannot be changed by mistake or by someone with my token.
14. As a User, I want other Users never to see my Balance, so that my finances stay private.

### Managing Contacts
15. As a User, I want to add an email to my profile, so that I can log in with it and be found by it.
16. As a User, I want to add a phone to my profile, so that I can log in with it and be found by it.
17. As a User, I want to change one of my emails, so that I can keep my profile current.
18. As a User, I want to change one of my phones, so that I can keep my profile current.
19. As a User, I want to delete one of my emails, so that I can drop an address I no longer use.
20. As a User, I want to delete one of my phones, so that I can drop a number I no longer use.
21. As a User, I want an attempt to use an email already used by another User to be rejected with a clear conflict error, so that Contacts stay unique across Users.
22. As a User, I want an attempt to use a phone already used by another User to be rejected with a clear conflict error, so that Contacts stay unique across Users.
23. As a User, I want emails compared case-insensitively, so that "A@x.com" and "a@x.com" count as the same address.
24. As a User, I want phones normalized to digits only before comparison, so that formatting differences do not defeat uniqueness.
25. As a User, I want invalid emails and phones (bad format, wrong digit count) rejected with field-level validation errors, so that I know what to fix.
26. As a User, I want to be unable to delete my last email or my last phone, so that I always keep at least one way to log in and be reached.
27. As a User, I want to be unable to change or delete a Contact that belongs to someone else, so that nobody can tamper with another profile.
28. As a User, I want re-submitting a Contact I already own to be handled predictably, so that retries do not corrupt my profile.

### Searching Users
29. As a User, I want to search Users born after a given date, so that I can find people by age group.
30. As a User, I want to search by exact phone, so that I can find a person from a number I have.
31. As a User, I want to search by exact email, so that I can find a person from an address I have.
32. As a User, I want to search by name prefix, case-insensitively, so that I can find people without knowing the full name.
33. As a User, I want to combine several filters, so that they narrow the result together.
34. As a User, I want search results to be paginated with a default size of 20 and a maximum of 100, so that large result sets stay fast.
35. As a User, I want stable ordering by id between pages, so that I never see the same User twice or miss one.
36. As a User, I want each search result to show id, name, date of birth, emails and phones but never password or Balance, so that private data stays private.
37. As a User, I want an empty result to be an empty page rather than an error, so that clients handle "nobody found" simply.
38. As a User, I want invalid filter values (bad date, oversized page) to be rejected with a clear 400, so that I can correct the request.

### Interest accrual
39. As a User, I want my Balance to increase by 10% of its current value every 30 seconds, so that my savings grow.
40. As a User, I want accrual to stop lifting my Balance once it reaches 207% of my Initial deposit, so that growth is bounded.
41. As a User, I want a Balance that is already above the Interest cap to be left unchanged, so that money I received is never taken away.
42. As a User, I want a Balance below the cap to resume earning Interest after I send money away, so that spending does not permanently disable growth.
43. As a User, I want the last accrual before the cap to stop exactly at the cap, so that I never exceed it through rounding.
44. As an operator, I want accrual to be a single atomic database operation, so that it cannot interleave badly with Transfers.
45. As an operator, I want each accrual run logged with how many Accounts were updated, so that I can see the job is alive.
46. As an operator, I want a failed accrual run to be logged and retried at the next tick without crashing the application, so that one error does not stop the job.
47. As an operator, I want rounding to follow a single documented rule (two decimals, half-even), so that Balances are predictable.

### Transfers
48. As a User, I want to send a chosen amount to another User, so that I can pay them.
49. As a User, I want my Transfer rejected when the amount is zero, negative, or has more than two decimals, so that nonsense amounts never move money.
50. As a User, I want a Transfer to myself rejected, so that I do not create meaningless records.
51. As a User, I want a Transfer to an unknown User rejected with 404, so that money cannot vanish.
52. As a User, I want a Transfer rejected when my Balance is insufficient, so that my Balance never goes negative.
53. As a User, I want the sender's debit and the recipient's credit to happen together or not at all, so that money is never lost or duplicated.
54. As a User, I want simultaneous Transfers, in either direction, to produce a correct result without deadlocks, so that the service is safe under load.
55. As a User, I want the total money across all Accounts to be unchanged by Transfers, so that the system is trustworthy.
56. As a User, I want every successful Transfer recorded with sender, recipient, amount and time, so that there is an audit trail.
57. As a User, I want a successful Transfer to return 201 with the Transfer record, so that I have confirmation.
58. As a User, I want to be able to send only from my own Account, never on behalf of someone else, so that a Transfer cannot be forged by supplying another sender.
59. As an operator, I want each Transfer attempt logged with the Transfer outcome and ids but never tokens, so that incidents can be investigated.

### Errors, documentation, operations
60. As an API client, I want all errors in the RFC 9457 problem format with a stable type, so that clients can handle them programmatically.
61. As an API client, I want validation errors to list each offending field, so that I can fix all of them at once.
62. As a developer, I want interactive API documentation (Swagger UI), so that I can explore and try the API.
63. As a developer, I want seeded Users available through migrations, so that I can log in immediately on a fresh database.
64. As a developer, I want the database schema owned entirely by migrations, so that environments stay reproducible.
65. As a developer, I want the project to build and test with one Maven command, so that onboarding is trivial.

## Implementation Decisions

- **Stack and deviation:** Java 21, Spring Boot 4.1, Maven, PostgreSQL. This deviates from the Java 11 in the source task and is recorded in ADR 0001.
- **Layering:** three top-level layers: API (controllers, request/response models, exception mapping), service (business rules, transactions), DAO (Spring Data JPA repositories, entities). Controllers never touch repositories; the DAO layer holds no business rules.
- **Schema, owned by Flyway migrations:**
  - User: id, name, date of birth, password hash.
  - Account: one-to-one with User; balance and initial deposit, both `NUMERIC(19,2)`; a check constraint keeps the balance non-negative as a last line of defense.
  - Email and Phone: child rows of User, each value globally unique (emails lowercased, phones digits-only); the "at least one of each" rule is enforced in the service layer, with seed data satisfying it.
  - Transfer: id, sender, recipient, amount, timestamp.
  - Seed Users (with BCrypt hashes, emails, phones, Accounts and Initial deposits) are inserted by a migration. Tests create their own Users directly through the DAO.
- **Money:** `BigDecimal` with scale 2 and half-even rounding in Java, `NUMERIC(19,2)` in the database.
- **Authentication:** login endpoint accepts an identifier (email or phone) plus password; BCrypt verification; response carries a JWT signed with HS256 using a secret from configuration/environment, expiring after one hour, with `USER_ID` as the only claim. No refresh tokens. A stateless security filter chain authenticates every request except login, API documentation and health. Failed login returns one generic 401.
- **Current-user resolution:** the authenticated User id comes only from the verified token. No endpoint accepts a sender or owner id from the request body. User-owned resources are addressed by the owner's id in the path, `/api/users/{userId}/...`; a write is allowed only when `{userId}` equals the token's `USER_ID`, otherwise 403, and services still receive the owner from the token (ADR 0003, `.scratch/user-resource-paths/spec.md`).
- **API contracts:**
  - `POST` login: identifier and password in, token out.
  - `GET /api/users/{userId}`: a User's profile; the Balance is included only when `{userId}` is the caller.
  - `POST /api/users/{userId}/emails`, `PUT` and `DELETE /api/users/{userId}/emails/{id}`; the same three for `/phones`; 403 when `{userId}` is not the caller.
  - `GET /api/users/search`: optional `dateOfBirth` (strictly after), `phone` (exact, normalized), `name` (case-insensitive prefix), `email` (exact, lowercased), `page`, `size` (default 20, max 100); filters ANDed; sorted by id; results exclude password and Balance.
  - `POST /api/transfers`: `{toUserId, amount}`; 201 with the Transfer record.
- **Contact rules:** a value used by another User is a 409; deleting the last email or last phone is a 422; invalid formats are 400 with field errors; phones are 10 to 15 digits after normalization; emails are lowercased and validated.
- **Transfer algorithm:** within a single transaction, lock both Accounts with pessimistic row locks acquired in ascending account id order, re-check the Balance after locking, debit and credit, and persist the Transfer record. Rejections: non-positive or over-precise amount (400), self-Transfer (422), unknown recipient (404), insufficient Balance (422). Rationale is in ADR 0002.
- **Interest accrual:** a scheduled job every 30 seconds on a single application instance (no distributed lock). It runs one atomic update: the new Balance is the lesser of the current Balance times 1.10 and the Interest cap, but never below the current Balance, so Balances above the cap are untouched. The accrual itself lives in a service method that tests can call directly.
- **Errors:** RFC 9457 `ProblemDetail` for every error, with a stable `type` and a per-field list on validation failures. A write under another User's id is a 403 (`urn:problem:forbidden`).
- **Optional extras included:** minimal springdoc Swagger configuration and meaningful SLF4J logging (no tokens, passwords or secrets in logs). Redis caching of other Users' profiles and search pages, never of Balances (`.scratch/redis-caching/spec.md`, ADR 0004); Elasticsearch is not used.
- **Vocabulary:** terms follow `GLOSSARY.md` (User, Account, Balance, Initial deposit, Interest accrual, Interest cap, Transfer, Contact).

## Testing Decisions

- **What makes a good test:** exercise external behavior only (HTTP responses, persisted state, resulting Balances), not internal calls or private structure. Assertions state the business rule being protected.
- **Seams (confirmed with the user):**
  1. **API seam:** MockMvc over a real PostgreSQL via Testcontainers. At least one end-to-end flow (login, then Transfer, then check Balances); additional API cases may reuse the same harness. This is the highest seam.
  2. **Transfer service seam:** unit tests with a mocked DAO for validation rules and arithmetic (rejections, exact debit/credit amounts).
  3. **Concurrency test:** on real PostgreSQL, run many parallel Transfers in opposing directions and assert that total money is conserved, no Balance is negative, and nothing deadlocks.
  4. **Interest accrual service seam:** a Testcontainers test that calls the accrual service directly (never waits 30 seconds) and asserts growth, the cap, and the above-cap and resume-after-spend cases.
- **Prior art:** none yet; this is a fresh project, so the first tests establish the Testcontainers base configuration that the others reuse.
- Full coverage is not expected; the priority is Transfer correctness, the Balance invariants, and one working API path.

## Out of Scope

- Creating, deleting or administering Users through the API; registration; password change or reset; roles.
- Editing name, date of birth or password.
- Idempotency keys for Transfers, Transfer history or listing endpoints, reversals, fees, currencies.
- Refresh tokens, token revocation, rate limiting.
- Multi-instance deployment and distributed job locking.
- Caching of Balances, Accounts or Transfers; Elasticsearch; any frontend.
- Gradle, Docker packaging and CI setup (can be added later with the devops skills).

## Further Notes

- Source requirements: `.scratch/banking-api/requirements.md`. Decisions: `docs/adr/0001-java-21-spring-boot-4.md` and `docs/adr/0002-pessimistic-ordered-locks-for-transfers.md`.
- The Initializr skeleton currently has only web, validation, actuator and lombok; JPA, PostgreSQL, Flyway, security, JWT, springdoc and Testcontainers dependencies still need to be added.
- Deliverable: a public GitHub repository link (agent skills under `.claude/skills/` are git-ignored and must not be pushed).
