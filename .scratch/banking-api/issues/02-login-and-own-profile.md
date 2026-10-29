# 02: Login and own profile

**What to build:** A User can log in with an email or a phone plus password, receive a JWT that carries only `USER_ID`, and use it to read their own profile and Balance. Every endpoint except login, API documentation and health is rejected without a valid token.

**Blocked by:** 01 Foundation

**Status:** done

- [x] Login accepts an email or phone (phone compared after normalization) plus password, verified with BCrypt
- [x] The token is HS256, expires after 1 hour, has `USER_ID` as its only claim, and the secret comes from configuration or the environment
- [x] A failed login returns one generic 401 that does not reveal whether the identifier exists
- [x] Missing, expired, malformed or tampered tokens get 401 on protected endpoints; login, docs and health are open
- [x] `GET /api/users/{userId}` returns name, date of birth, emails, phones and own Balance, never the password hash
- [x] The current User comes only from the verified token
- [x] MockMvc + Testcontainers API test: log in by email, log in by phone, call `/api/users/{userId}`, and cover the 401 cases
