# 05: Search users

**What to build:** Any signed-in User can find other Users by date of birth, exact phone, name prefix and exact email, with pagination.

**Blocked by:** 02 Login and own profile

**Status:** done

- [x] `GET /api/users/search` accepts optional `dateOfBirth` (strictly after), `phone` (exact, normalized), `name` (case-insensitive prefix), `email` (exact, lowercased), `page` and `size`
- [x] Filters combine with AND; no filters returns everyone
- [x] `size` defaults to 20 and is at most 100; results are sorted by id and stable across pages
- [x] Each result shows id, name, date of birth, emails and phones; never password or Balance
- [x] No match is an empty page, not an error; invalid values (bad date, oversized page) get 400
- [x] Tests cover each filter, combinations, pagination and the hidden fields
