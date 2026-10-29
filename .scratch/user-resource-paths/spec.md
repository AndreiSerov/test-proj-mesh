Status: done

# User resource paths: `/api/users/{userId}` instead of `/api/users/me`

## Problem Statement

The API addresses the caller's own resources through the alias `/api/users/me`. Every User therefore has no stable, canonical URL, the profile of another User cannot be fetched directly (only through search), and the paths read less like REST resources than `/api/users/{userId}`.

## Solution

Every User-owned resource is addressed by the owner's id: `/api/users/{userId}` for the profile and `/api/users/{userId}/emails|phones` for Contacts. The `/me` alias is removed. Any authenticated User may read any profile; the Balance is shown only to its owner. Only the owner may change Contacts: a write under another User's id is refused with 403 by a single Spring Security rule before any validation or business logic runs. The ownership rule changes from "the owner never appears in the request" to "the owner may appear in the path, but must equal the token's `USER_ID`" (ADR 0003).

## User Stories

1. As a User, I want my profile at `GET /api/users/{myId}`, including my Balance, so that every User has one canonical URL.
2. As a User, I want to read another User's profile at `GET /api/users/{theirId}` without their Balance, so that I can look someone up without searching, while finances stay private (spec story 14 of banking-api still holds).
3. As a User, I want `GET /api/users/{unknownId}` to return 404, so that clients can tell "nobody" from "forbidden".
4. As a User, I want to manage my Contacts at `/api/users/{myId}/emails` and `/api/users/{myId}/phones` with the same rules as before (409 in use, 422 last one, 400 invalid, 404 unknown Contact id).
5. As a User, I want any `POST`, `PUT` or `DELETE` under another User's id to be refused with 403, so that nobody can tamper with my Contacts.
6. As a User, I want that 403 to come before body validation, so that an attacker learns nothing about my Contacts from validation errors.
7. As an API client, I want a non-numeric `{userId}` to be a 400 (or 403 on write paths), never a 500.

## Implementation Decisions

- **Paths:**
  - `GET /api/users/{userId}`: `ProfileView`; `balance` present only when `userId` equals the token's `USER_ID`, omitted from the JSON otherwise.
  - `POST /api/users/{userId}/emails`, `PUT` and `DELETE /api/users/{userId}/emails/{id}`; the same three for `/phones`.
  - `GET /api/users/search` and `POST /api/transfers` unchanged (literal `search` wins over `{userId}`; the Transfer sender still comes only from the token).
  - `/api/users/me` and `/api/users/me/**` are removed (no alias, no redirect).
- **Ownership guard:** in `SecurityConfig`, the matchers `/api/users/{userId}/emails/**` and `/api/users/{userId}/phones/**` (and the collection paths without a trailing segment) use an `AuthorizationManager` that grants access only when `{userId}` parses as a long equal to `USER_ID`. A refusal goes through an `AccessDeniedHandler` writing an RFC 9457 body with `type` `urn:problem:forbidden`, status 403, built by `ProblemResponses` from `HttpStatus.FORBIDDEN`.
- **Defense in depth:** Contact controllers keep passing `TokenService.userId(jwt)` to the services; `{userId}` only addresses the resource. Removing the guard therefore cannot let one User write another's Contacts.
- **Profile read:** `UserService.profile(viewerId, userId)` loads the User (404 if absent) and includes the Balance only when `viewerId == userId`. `ProfileView.balance` is serialized with `@JsonInclude(NON_NULL)`.
- **Docs:** ADR 0003 records the new ownership rule; banking-api `spec.md` points here for the changed contracts; CLAUDE.md rule and README endpoint table are updated; Swagger lists the new paths.

## Testing Decisions

- API seam only (MockMvc over Testcontainers), extending `ApiIntegrationTest`, one fresh User per test.
- Existing Contact and profile tests move to the new paths unchanged in intent.
- New cases: own profile has Balance; other profile has no `balance` key; unknown id 404; `/api/users/me` no longer resolves to a profile; 403 for `POST`/`PUT`/`DELETE` on another User's emails and phones; 403 even with an invalid body; Contacts of the victim unchanged after a refused write.
- `ApiDocsAndLoggingTest` asserts the new path set.

## Out of Scope

- Changing `/api/transfers` to `/api/users/{userId}/transfers`.
- Admin roles or any way to write another User's data.
- Keeping `/me` as a compatibility alias.
