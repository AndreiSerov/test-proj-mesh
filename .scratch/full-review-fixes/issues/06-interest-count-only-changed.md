# 06: Interest accrual counts Balances that did not change

**What to build:** `accrue()` updates, locks and counts only Accounts whose Balance actually grows.

**Blocked by:** none

**Status:** done

For a Balance of 0.00 to 0.04, `round_half_even(balance * 1.10)` equals the Balance, yet the row is locked, rewritten and counted on every tick; the Javadoc says it returns "how many Balances changed".

- [x] Add `LEAST(round_half_even(balance * :growth), round_half_even(initial_deposit * :capFactor)) > balance` to both the locking subquery and the outer `WHERE`; keep `ORDER BY id ... FOR UPDATE` (ADR 0002)
- [x] Accrual test (own context, like the other accrual tests): an Account at 0.04 is not counted and stays 0.04; a normal Account is counted
- [x] Existing accrual tests (growth, cap, above-cap, resume) still pass
- [x] `./mvnw verify` passes

## Comments

The filter makes the old `GREATEST(balance, ...)` redundant (the new value is always greater), so it was dropped. Test `InterestAccrualTest.aBalanceTooSmallToGrowAfterRoundingIsNotCountedAsChanged` first lifts every other account above its cap so the count is exact.
