# 02: 401 and 403 bodies bypass `ErrorKind`

**What to build:** Build the security error bodies from `ErrorKind`, so CLAUDE.md "errors go through `ApiException`/`ErrorKind`" holds and `ErrorKind.FORBIDDEN` is used.

**Blocked by:** none

**Status:** done

`SecurityConfig.unauthorizedEntryPoint` and `forbiddenHandler` write hand-typed JSON strings; a change to `ErrorKind` slugs would not reach them.

- [x] One writer (e.g. `ProblemResponses.write(response, ErrorKind, detail)`) used by both handlers; it serializes a `ProblemDetail` built from the `ErrorKind` (status, title, `type()`)
- [x] Existing 401/403 tests keep passing unchanged (`urn:problem:unauthorized`, `urn:problem:forbidden`, content type `application/problem+json`)
- [x] A test asserts the 403 body's `status` and `title` fields, not only `type`
- [x] `./mvnw verify` passes

## Comments

New `api/error/ProblemResponses.write` builds type/title/status/detail/instance from the `ErrorKind` and serializes with Boot's `JsonMapper`. Red step: 401/403 bodies now include `instance` like every other problem (`ErrorTypesTest.securityErrorsHaveTheSameProblemShapeAsEveryOtherError`).
