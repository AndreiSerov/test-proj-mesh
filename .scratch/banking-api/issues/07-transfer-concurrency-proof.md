# 07: Transfer concurrency proof

**What to build:** Evidence that Transfers are safe under load, and fixes for whatever the evidence turns up. Many parallel Transfers in opposing directions between the same Accounts must leave the system consistent.

**Blocked by:** 06 Money transfer

**Status:** done

- [x] An integration test on real PostgreSQL runs many parallel opposing and overlapping Transfers
- [x] Total money across all Accounts is identical before and after
- [x] No Balance is negative and no Transfer is partially applied
- [x] The test finishes without deadlock or timeout
- [x] Any defect found in 06 is fixed, with the test left in place as a regression guard
