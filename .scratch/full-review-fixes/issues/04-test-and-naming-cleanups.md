# 04: Test helper and naming cleanups

**What to build:** Small readability fixes from the full review; no behaviour change.

**Blocked by:** 03 (touches `UserProfileTest`)

**Status:** done

- [x] `TransferApiTest` and `UserProfileTest` use `profileUri(user)` instead of building `"/api/users/" + id`
- [x] Replace `contactUri(user, String kind)` with `emailsUri(user)` and `phonesUri(user)` so a typo cannot build a wrong path
- [x] Rename `ContactRequest` to `EmailRequest` (it validates emails only; GLOSSARY "Contact" means email or phone)
- [x] `./mvnw verify` passes

## Comments

Refactor only, covered by the existing tests. `ContactRequest` renamed to `EmailRequest`.
