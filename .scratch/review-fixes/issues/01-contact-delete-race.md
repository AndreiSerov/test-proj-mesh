# 01: Concurrent deletes can remove a User's last Contact

**What to build:** Make "each User keeps at least one email and one phone" hold under concurrency.

**Blocked by:** none

**Status:** done

`ContactService.delete` checks `countByUserId(userId) <= 1` and then deletes, with nothing locking the User in between. With two emails A and B, parallel `DELETE /api/users/{userId}/emails/{A}` and `DELETE /api/users/{userId}/emails/{B}` both see a count of 2 and both succeed, leaving zero emails. The same applies to phones.

- [x] Add `UserRepository.lockById(Long id)` with `@Lock(PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE` on `users`)
- [x] `ContactService.delete` locks the User before counting; `add` and `change` take the same lock so all Contact writes for one User are serialized
- [x] Lock order stays users-row only; it never touches `accounts`, so it cannot deadlock with transfers or interest (ADR 0002)
- [x] Concurrency test (style of `TransferConcurrencyTest`): User with two emails, two parallel deletes of different ids; exactly one returns 204, the other 422, and one email remains. Same for phones
- [x] `./mvnw verify` passes

## Comments

Added `UserRepository.lockById` (`@Lock(PESSIMISTIC_WRITE)`, users row only). `ContactService.add/change/delete` call it first (404 if the User is gone). Concurrency tests in `ContactConcurrencyTest` (service level, 10 rounds each): two parallel deletes of two emails / two phones leave exactly one, one call succeeds and one gets UNPROCESSABLE (the 204/422 of the API). Seen failing before the fix. Deviation: tested at service level like `TransferConcurrencyTest`, not through MockMvc.
