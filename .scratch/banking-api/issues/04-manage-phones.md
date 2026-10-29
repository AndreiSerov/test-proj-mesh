# 04: Manage phones

**What to build:** The same Contact management for phones: a signed-in User can add, change and delete their own phones, unique across all Users, never dropping the last one.

**Blocked by:** 03 Manage emails

**Status:** done

- [x] `POST /api/users/{userId}/phones`, `PUT` and `DELETE /api/users/{userId}/phones/{id}` work on the caller's own phones only (403 when `{userId}` is not the caller)
- [x] Phones are normalized to digits only, 10 to 15 digits; invalid input gets 400 with field errors
- [x] A phone used by another User (after normalization) gets 409
- [x] Deleting the last phone gets 422
- [x] Changing or deleting another User's phone is rejected (404)
- [x] Rules and error handling are shared with emails rather than copied
- [x] Tests cover each rule through the API
