# 03: Cache search pages

**What to build:** identical `GET /api/users/search` requests are served from `userSearch`, and any Contact write empties it.

**Blocked by:** 02

**Status:** done

- [x] `UserServiceImpl.search` is `@Cacheable("userSearch")` with a key from all criteria, page and size
- [x] `AbstractContactService.add/change/delete` also evict all of `userSearch`
- [x] API tests: a repeated search is cached (a direct DB change stays invisible); a Contact write through the API makes the next search fresh; different criteria are different entries
- [x] `./mvnw verify` passes
