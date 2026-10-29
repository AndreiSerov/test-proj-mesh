# 04: A token without USER_ID causes a 500

**What to build:** Reject correctly signed tokens that lack a numeric `USER_ID` claim with 401.

**Blocked by:** none

**Status:** done

`TokenService.userId` casts `jwt.getClaim(USER_ID_CLAIM)` to `Number` with no null check, so a missing or non-numeric claim throws NPE/ClassCastException and the global handler returns 500.

- [x] Add a `JwtValidator` (an `OAuth2TokenValidator<Jwt>` on the `JwtDecoder` in `SecurityConfig`) requiring `USER_ID` to be present and numeric, combined with the default timestamp validator
- [x] Invalid tokens then fail in the resource-server filter and get the existing 401 problem body
- [x] API test: token signed with the test secret but without `USER_ID` → 401 on `GET /api/users/{userId}`

## Comments

`SecurityConfig.jwtDecoder` now uses `DelegatingOAuth2TokenValidator(JwtValidators.createDefault(), requireNumericUserId)`. A signed token with a missing or non-numeric `USER_ID` gets the existing 401 problem body. Test: `LoginAndProfileTest.correctlySignedTokenWithoutANumericUserId...` (missing claim and string claim). Seen failing (500) before the fix.
