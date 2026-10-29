# 04: ADR 0004 and docs for caching

**What to build:** Record the caching decision and replace the "no caching" rule everywhere it appears.

**Blocked by:** 03

**Status:** done

- [x] `docs/adr/0004-redis-cache-for-read-mostly-user-data.md`
- [x] CLAUDE.md: caching rule (what is cached, what never is, eviction on Contact writes, Redis in compose and tests); commands mention Redis
- [x] README: caching paragraph replaces "No caching", Redis in the run instructions and configuration
- [x] `.scratch/banking-api/spec.md`: "Caching is deliberately excluded" and the out-of-scope line point to this spec
