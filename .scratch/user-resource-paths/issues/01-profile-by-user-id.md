# 01: Profile at `GET /api/users/{userId}`

**What to build:** Replace `GET /api/users/me` with `GET /api/users/{userId}`; any authenticated User can read any profile, the Balance only their own.

**Blocked by:** none

**Status:** done

- [x] `UserController`: `GET /{userId}` replaces `GET /me`; `/search` still resolves to search
- [x] `UserService.profile(viewerId, userId)`: 404 for an unknown User; Balance only when `viewerId == userId`
- [x] `ProfileView.balance` omitted from JSON when null (`@JsonInclude(NON_NULL)`)
- [x] Tests: own profile has Balance; another User's profile has no `balance` key; unknown id 404; non-numeric id 400; `/api/users/me` no longer returns a profile
- [x] Existing tests that read `/api/users/me` use `/api/users/{id}`
- [x] `./mvnw verify` passes
