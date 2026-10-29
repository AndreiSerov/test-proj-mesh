# 07: The token carries an `iat` claim

**What to build:** The JWT carries only `USER_ID` plus the `exp` needed for the one-hour expiry (requirements item 6).

**Blocked by:** none

**Status:** done

- [x] `TokenService.issue` stops setting `issuedAt`
- [x] `LoginAndProfileTest.tokenCarriesOnlyTheUserIdClaim` expects exactly `USER_ID` and `exp`, with `exp` about one hour from now
- [x] Expired-token and default-validator tests still pass
- [x] `./mvnw verify` passes

## Comments

The claim test now checks `exp` is about an hour from now instead of `exp - iat`.
