# 02: Login timing reveals which emails and phones are registered

**What to build:** Make a failed login take the same time whether or not the identifier exists.

**Blocked by:** none

**Status:** done

`AuthService.login` only runs BCrypt when a User is found. An unknown identifier answers in about 1 ms, a known one with a wrong password in about 80 ms, so an attacker can enumerate registered contacts.

- [x] When no User matches, still call `passwordEncoder.matches(password, DUMMY_HASH)` against a fixed BCrypt hash (same cost as real hashes) and then return 401
- [x] Response body and log line stay identical for "unknown identifier" and "wrong password"
- [x] Unit or API test asserts the encoder is invoked on the unknown-identifier path (do not assert wall-clock timing)
- [x] README notes that there is no login throttling (out of scope for the task)

## Comments

`AuthService.login` always calls `passwordEncoder.matches` (against `DUMMY_HASH`, a cost-10 `$2a$` hash of a random string, when no User matches, including identifiers with no digits). Same log line and 401 body in both cases. `AuthServiceTest` (Mockito) verifies the encoder is called on the unknown email, unknown phone and digit-less paths. README notes the lack of login throttling.
