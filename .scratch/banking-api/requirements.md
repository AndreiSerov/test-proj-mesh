# Source requirements (from the task document)

Deviation: Java 21 + Spring Boot 4.1 instead of Java 11 (decided with the user).

## Required
1. Stack: PostgreSQL, Spring Boot, REST API, Maven. Redis/Elasticsearch optional.
2. Architecture: three layers: API, service, DAO.
3. Users: regular users only. Created via migrations; tests create them directly in the DAO. Multiple emails and phones (at least one each), exactly one account. Initial balance set at creation. Balance never negative. Input validation required.
4. User update: a user can add/change/delete their own emails and phones, if not used by another user. Other fields read-only.
5. User search: any user can search others; filters: date of birth later than given date, exact phone, name prefix (LIKE), exact email. Pagination required.
6. JWT auth: token carries only the USER_ID claim. Login by email or phone + password. Keep it simple.
7. Interest job: every 30 seconds each balance +10%, capped at 207% of the initial deposit.
8. Money transfer: from authenticated user to another user. High-stakes: thorough validation and thread safety.

## Optional
- Minimal Swagger config
- Meaningful logging
- Sensible caching at API or DAO layer

## Testing
- Unit tests for transfer logic
- At least one API operation tested with Testcontainers + MockMvc

## Deliverable
Public GitHub repo link.
