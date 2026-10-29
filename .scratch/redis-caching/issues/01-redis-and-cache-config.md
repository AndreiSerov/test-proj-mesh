# 01: Redis connection, cache manager and test container

**What to build:** Redis wired into the app, local compose and tests, with one configured cache manager.

**Blocked by:** none

**Status:** done

- [x] `spring-boot-starter-data-redis`; test dependency `com.redis:testcontainers-redis:3.0.0` (Testcontainers 2.x line)
- [x] `config/CacheConfig`: `@EnableCaching`, `RedisCacheManager` with string keys, typed JSON values, TTLs (`userProfiles` 10m, `userSearch` 1m, default 30m), no null values, `transactionAware()`; `CacheErrorHandler` that logs and proceeds
- [x] `application.yml`: `spring.data.redis.host/port/timeout`; docker-compose `redis:8.10.2-alpine` (`allkeys-lru`, 256 MB, healthcheck)
- [x] `IntegrationTestContainers`: `@ServiceConnection RedisContainer`
- [x] `ApiIntegrationTest` clears every cache before each test
- [x] `./mvnw verify` passes

## Comments

testcontainers-redis 3.0.0 instead of the skill's 2.2.4: 2.2.4 is built on Testcontainers 1.x, the project uses 2.0.5; 3.0.1 was only a day old. Values use a typed `JacksonJsonRedisSerializer` per cache instead of `GenericJacksonJsonRedisSerializer` with default typing, so no `@class` hints or package whitelist are needed. Added `CacheOutageTest` (Redis stopped → API still answers) beyond the checklist.
