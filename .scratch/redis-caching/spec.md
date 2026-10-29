Status: done

# Redis caching for read-mostly User data

## Problem Statement

The task lists "sensible caching at API or DAO layer" as optional, with Redis allowed. Every profile read and every search goes to PostgreSQL, although the data they return (name, date of birth, emails, phones) changes rarely and only through the Contact endpoints.

## Solution

Cache, in Redis through Spring Cache, the two reads that return no money: another User's public profile and search result pages. Evict them whenever a Contact is added, changed or deleted. Never cache Balances, Accounts or Transfers: they change every 30 seconds through Interest accrual and on every Transfer, and must be read fresh under locks.

## User Stories

1. As a User, I want another User's profile to be served quickly when I look at it repeatedly, so that browsing other Users is cheap.
2. As a User, I want repeated identical searches to be served quickly, so that paging back and forth is cheap.
3. As a User, I want my own profile, with my Balance, always read fresh, so that I never see a stale Balance.
4. As a User, I want a Contact I add, change or delete to show up in other Users' views of my profile and in search right away, so that caching never hides my changes.
5. As an operator, I want the API to keep working (uncached) when Redis is down, so that a cache outage is not an API outage.
6. As an operator, I want every cached entry to expire on its own and to be readable JSON, so that Redis never fills up and entries survive record changes predictably.

## Implementation Decisions

- **Provider:** `spring-boot-starter-data-redis`; one `RedisCacheManager` in `config/CacheConfig` with `@EnableCaching`.
- **Caches and TTLs:**
  - `userProfiles`: key `userId`, value `ProfileView` without Balance, TTL 10 minutes.
  - `userSearch`: key built from all criteria plus page and size, value `PageView<UserSummary>`, TTL 1 minute.
  - Default TTL 30 minutes for any other cache; nulls never cached.
- **Where:** a separate bean `PublicProfileReader` (`service/impl`) holds the cached public-profile read, so `UserServiceImpl.profile` calls it through the proxy (no self-invocation). The owner's own profile bypasses the cache and loads the Balance fresh. `UserServiceImpl.search` is `@Cacheable`.
- **Eviction:** `AbstractContactService.add/change/delete` evict `userProfiles[userId]` and all of `userSearch` (a Contact change can move a User into or out of any page). The cache manager is transaction-aware, so evictions run after the commit and a concurrent reader cannot re-cache pre-commit data.
- **Serialization:** keys as strings; values as JSON with a typed `JacksonJsonRedisSerializer` per cache (no polymorphic `@class` hints, nothing to whitelist).
- **Failure mode:** a `CacheErrorHandler` logs and proceeds, so a Redis error is a cache miss, not a 500. `spring.data.redis.timeout` is 2 s.
- **Config:** `spring.data.redis.host/port` from `REDIS_HOST`/`REDIS_PORT`, default `localhost:6379`; docker-compose gains `redis:8.10.2-alpine` with `allkeys-lru` and a 256 MB limit.
- **Single instance stays the assumption** (interest job); Redis would also keep the cache consistent across several instances.
- ADR 0004 records the decision and why money is never cached; CLAUDE.md's "No caching on purpose" rule is replaced.

## Testing Decisions

- API seam (MockMvc + Testcontainers Postgres and Redis). Caching is observed from outside: a change written straight to the database (bypassing the service) stays invisible while the entry is cached, and becomes visible after a Contact write through the API.
- Cases: other profile cached; own profile and Balance never cached; Contact add/change/delete evict the profile; search cached and evicted by a Contact write.
- Caches are cleared before every API test so tests never see each other's entries.
- Redis-down behaviour: `CacheOutageTest` runs in its own context, stops its own Redis container and checks that cached reads and evicting writes still answer 200/201.

## Out of Scope

- Caching Balances, Accounts, Transfers, login lookups.
- Rate limiting, sessions, Redis as a broker.
- Distributed locking for the interest job.
