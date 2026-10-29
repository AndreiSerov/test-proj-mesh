# 06: Money transfer

**What to build:** A signed-in User can send money to another User. The debit, the credit and the Transfer record happen together or not at all, and the Balance never goes negative. Follows ADR 0002: both Accounts are locked pessimistically in ascending id order inside one transaction, and the Balance is checked after locking.

**Blocked by:** 02 Login and own profile

**Status:** done

- [x] `POST /api/transfers` with `{toUserId, amount}` returns 201 with the Transfer record (id, sender, recipient, amount, time)
- [x] The sender is always the authenticated User; no endpoint accepts a sender id
- [x] Amount must be positive with at most 2 decimals (400); self-Transfer gets 422; unknown recipient gets 404; insufficient Balance gets 422
- [x] Unit tests with a mocked DAO cover every rejection and the exact debit and credit amounts
- [x] An API test (MockMvc + Testcontainers) logs in, makes a Transfer, and checks both Balances through `/api/users/{userId}`
- [x] Each Transfer attempt is logged with outcome and ids, never tokens
