# 03: `UserProfileTest` can fail at random

**What to build:** Remove the substring check that collides with random test data.

**Blocked by:** none

**Status:** done

`UserProfileTest.anotherUsersProfileIsReadableButHidesTheBalance` asserts the body does not contain `"999"`, but the other User's random phone, email suffix or id can contain it.

- [x] Drop `doesNotContain("999")`; keep `doesNotContainKey("balance")`; if a value check is wanted, use a Balance that cannot occur in generated data (e.g. `"12345.67"` and assert `doesNotContain("12345.67")`)
- [x] `./mvnw verify` passes

## Comments

No red step: the test failed only at random. The other User now has Balance 12345.67, which generated phones, ids and hex email suffixes cannot contain.
