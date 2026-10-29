# 03: Every integrity violation on Contact save is reported as "already in use"

**What to build:** Map only the unique-value violation to 409; anything else must not masquerade as a conflict.

**Blocked by:** none

**Status:** done

`ContactService.save` turns any `DataIntegrityViolationException` into 409 "is already in use". `add` uses `users.getReferenceById(userId)`, so a still-valid token for a User that no longer exists fails on the `user_id` foreign key and the client is told the email/phone is taken.

- [x] Name the unique constraints in a new Flyway migration (`emails_value_key`, `phones_value_key` are the Postgres defaults; confirm and reference them explicitly) — never edit V1
- [x] `save` maps to 409 only when the violated constraint is the value-unique one (inspect `ConstraintViolationException.getConstraintName()` from the cause); otherwise rethrow
- [x] `add` resolves the User with `findById` and returns 404 if missing, instead of relying on the FK
- [x] API test: duplicate value still returns 409

## Comments

The constraint names were already stable: V1's inline `UNIQUE` gives `emails_value_key` / `phones_value_key` (confirmed in `pg_constraint`), so no V4 migration was needed. `ContactService` takes the constraint name from `EmailService` / `PhoneService`; `save` maps to 409 only when the cause is a Hibernate `ConstraintViolationException` with that name, otherwise rethrows. `add` resolves the User via `lockById` (ticket 01) instead of `findById`, so a missing User is 404. Tests: `ManageEmailsTest.tokenOfAUserThatNoLongerExists...` (404) and `ContactConcurrencyTest.twoUsersAddingTheSameEmailAtOnce...` (exercises the real unique violation in `save`, expects 409). The plain duplicate-409 API tests already existed and still pass.
