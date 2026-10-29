# 09: Swagger, logging and README

**What to build:** The optional extras and the handover: interactive API documentation, trustworthy logs, and a README so someone else can run and test the project.

**Blocked by:** 06 Money transfer, 08 Interest accrual job

**Status:** done

- [x] Minimal springdoc configuration; Swagger UI is reachable without a token and documents all endpoints, including how to pass the JWT
- [x] Logs reviewed: login failures, Transfers and accrual runs are logged meaningfully; no tokens, passwords, secrets or Balances appear
- [x] README covers what the service does, how to run it, how to run tests, the seed Users, configuration (JWT secret), and the Java 21 / Boot 4 deviation from the task (ADR 0001)
- [x] `./mvnw verify` passes from a clean checkout
