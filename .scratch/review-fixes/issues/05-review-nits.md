# 05: Small cleanups from the code review

**What to build:** Low-risk tidy-ups; no behaviour change except the overflow case.

**Blocked by:** none

**Status:** done

- [x] `JwtProperties`: measure the secret with `getBytes(StandardCharsets.UTF_8)`, the same charset `SecurityConfig.key` uses
- [x] Balance overflow: `TransferRequest` allows 15 integer digits but `NUMERIC(19,2)` holds 17; a recipient Balance near the limit overflows on `add()` and returns 500. In `TransferService`, reject with 422 when `recipient.balance + amount` exceeds `99999999999999999.99`; unit test in `TransferServiceTest`
- [x] `UserService`: extract one helper that maps a Contact list to `List<ContactView>` and use it in `profile` and `summary` (four duplicated lambdas today)
- [x] `./mvnw verify` passes

## Comments

`JwtProperties` measures the secret as UTF-8. `TransferService` rejects with 422 when `recipient.balance + amount > 99999999999999999.99` (checked after the insufficient-funds check, before any mutation); `TransferServiceTest` has an overflow case and an exactly-at-limit case. `UserService.contactViews` replaces the four duplicated lambdas.
