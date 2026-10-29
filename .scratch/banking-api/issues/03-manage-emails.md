# 03: Manage emails

**What to build:** A signed-in User can add, change and delete their own emails, keeping them unique across all Users and never dropping their last one.

**Blocked by:** 02 Login and own profile

**Status:** done

- [x] `POST /api/users/{userId}/emails`, `PUT` and `DELETE /api/users/{userId}/emails/{id}` work on the caller's own emails only (403 when `{userId}` is not the caller)
- [x] Emails are lowercased and validated; invalid input gets 400 with field errors
- [x] An email used by another User gets 409
- [x] Deleting the last email gets 422
- [x] Changing or deleting an email that belongs to another User is rejected (404)
- [x] Re-submitting an email the caller already owns is handled predictably (no corruption, no duplicate row)
- [x] Tests cover each rule through the API
