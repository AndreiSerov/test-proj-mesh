# 02: Cache other Users' public profiles

**What to build:** `GET /api/users/{userId}` for another User is served from `userProfiles`; the owner's view never is.

**Blocked by:** 01

**Status:** done

- [x] `PublicProfileReader.publicProfile(userId)` is `@Cacheable("userProfiles")`, returns `ProfileView` with `balance = null`, throws 404 for an unknown User (not cached)
- [x] `UserServiceImpl.profile`: owner → fresh load with Balance; anyone else → `PublicProfileReader`
- [x] `AbstractContactService.add/change/delete` evict `userProfiles[userId]`
- [x] API tests: another User's profile is cached (a direct DB change stays invisible); own Balance is never cached; adding, changing or deleting a Contact evicts it
- [x] `./mvnw verify` passes

## Comments

The own-Balance guard passed without new code (the owner path never used the cache); the other-profile and eviction tests were red first.
