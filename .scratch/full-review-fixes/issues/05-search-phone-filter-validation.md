# 05: Search ignores an invalid phone filter

**What to build:** Reject an invalid `phone` search filter with 400, as banking-api story 38 asks.

**Blocked by:** none

**Status:** done

`?phone=abc` is normalized to `""`, the filter is skipped, and the search returns every User with 200.

- [x] `UserController.search`: `phone` carries `@ValidPhone`; `ValidPhone.Validator` treats `null` as valid (Bean Validation convention; `PhoneRequest` keeps `@NotBlank`)
- [x] API tests: `?phone=abc` and `?phone=123` → 400 with a field error for `phone`; a formatted valid phone still matches
- [x] Existing phone tests (blank, letters, too short/long → 400) still pass
- [x] `./mvnw verify` passes

## Comments

`ValidPhone` now accepts `null`; `PhoneRequest` keeps `@NotBlank`, so blank phones there are still 400.
