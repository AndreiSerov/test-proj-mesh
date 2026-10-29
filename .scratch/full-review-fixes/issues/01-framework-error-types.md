# 01: Framework errors come back with `type: about:blank`

**What to build:** Every error response carries a stable `urn:problem:*` type, including the ones Spring raises itself.

**Blocked by:** none

**Status:** done

`GlobalExceptionHandler` sets a type only for `ApiException` and the two validation handlers. Errors handled by the inherited `ResponseEntityExceptionHandler` (type mismatch, unreadable JSON, 405, 415, missing parameter) keep `about:blank`, against banking-api spec "RFC 9457 `ProblemDetail` for every error, with a stable `type`".

- [x] Override `handleExceptionInternal`: when the body is a `ProblemDetail` whose type is `about:blank`, set the type from the `ErrorKind` matching the status (400 → `urn:problem:invalid-input`, 404 → `not-found`, ...), else `urn:problem:http-<status>`
- [x] API tests assert `$.type`: `GET /api/users/abc` → `urn:problem:invalid-input`; malformed JSON body → `invalid-input`; `?dateOfBirth=bad` → `invalid-input`; wrong method on `/api/transfers` → 405 with a non-`about:blank` type
- [x] `./mvnw verify` passes

## Comments

Spring Framework 7 leaves the default `ProblemDetail` type `null` (not `about:blank`), so `handleExceptionInternal` fills in both cases via `ErrorKind.typeFor(status)`. 405 maps to `urn:problem:http-405` (no ErrorKind for it). Tests in `ErrorTypesTest`; the 3 slices after the first went green without new code, since they share the same hook.
