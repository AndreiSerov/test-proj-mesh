# 08: Re-adding an own Contact says it belongs to someone else

**What to build:** Re-submitting a Contact the User already owns gives a 409 that says so, distinct from a clash with another User (banking-api story 28).

**Blocked by:** none

**Status:** done

- [x] `ContactRepository.existsByValueAndUserId`; `ContactService.add` throws 409 "You already have this email/phone" when the caller owns the value, and keeps "is already in use" for another User's value
- [x] API tests for emails and phones assert the two different `detail` messages
- [x] `./mvnw verify` passes

## Comments

Checked before the global `existsByValue`, under the same user lock from review-fixes 01.
