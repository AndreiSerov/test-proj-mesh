# 08: Interest accrual job

**What to build:** Every 30 seconds each Balance grows by 10% of its current value, never lifting it past the Interest cap (207% of the Initial deposit), and Balances above the cap are left alone.

**Blocked by:** 01 Foundation

**Status:** done

- [x] A scheduled job runs every 30 seconds on a single instance and calls an accrual service method
- [x] Accrual is one atomic database update: the lesser of Balance x 1.10 and the Interest cap, never below the current Balance; rounded to 2 decimals, half-even
- [x] A Balance above the cap is unchanged; a Balance below the cap resumes earning after a Transfer sends money away
- [x] The last accrual before the cap lands exactly on the cap
- [x] A failed run is logged and retried at the next tick without stopping the application; each run logs how many Accounts were updated
- [x] A Testcontainers test calls the accrual service directly (no waiting 30 seconds) and covers growth, the cap, the above-cap case and resume-after-spend
