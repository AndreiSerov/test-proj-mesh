# 03: ADR 0003, docs and Swagger for the new paths

**What to build:** Record the changed ownership rule and update every document that names the old paths.

**Blocked by:** 02

**Status:** done

- [x] `docs/adr/0003-owner-id-in-path.md`: owner may appear in the path but must equal `USER_ID`; enforced once in `SecurityConfig`; services still take the owner from the token; why `/me` was dropped
- [x] `.scratch/banking-api/spec.md`: API contracts and current-user resolution point to this spec
- [x] CLAUDE.md "Rules that are easy to break": the new ownership rule and the 403 error
- [x] README endpoint table uses the new paths
- [x] `ApiDocsAndLoggingTest` asserts the new path set in `/v3/api-docs`
