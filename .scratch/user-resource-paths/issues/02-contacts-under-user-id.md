# 02: Contacts at `/api/users/{userId}/emails|phones` with a 403 ownership guard

**What to build:** Move Contact endpoints under the owner's id and refuse writes under any other id with 403, before validation.

**Blocked by:** 01

**Status:** done

- [x] `EmailController` / `PhoneController` map to `/api/users/{userId}/emails` and `/phones`; they keep passing `TokenService.userId(jwt)` to the services
- [x] `SecurityConfig`: an `AuthorizationManager` on `/api/users/{userId}/emails`, `/emails/**`, `/phones`, `/phones/**` grants only when `{userId}` parses to the token's `USER_ID`
- [x] `AccessDeniedHandler` writes the RFC 9457 403 body (`urn:problem:forbidden`); `ErrorKind.FORBIDDEN` added
- [x] Tests: 403 for `POST`, `PUT`, `DELETE` on another User's emails and phones; 403 with an invalid body too; the victim's Contacts are unchanged
- [x] Existing Contact tests use the new paths; all previous rules (409, 422, 400, 404) still pass
- [x] `./mvnw verify` passes

## Comments

The matcher is `/api/users/{userId}/emails/**` and `/phones/**` only: `/**` also matches zero segments, so the collection paths are covered (verified by the 403 `POST` tests). A non-numeric `{userId}` on a write path is a 403, not a 500 (`ManageEmailsTest.writesUnderANonNumericUserIdAreForbiddenNotAServerError`). The 403 body is written by the `AccessDeniedHandler` like the existing 401 entry point; `ErrorKind.FORBIDDEN` exists for vocabulary but no `ApiException` throws it yet. The old "victim's contact id under the attacker's own path" tests stay and still give 404.
