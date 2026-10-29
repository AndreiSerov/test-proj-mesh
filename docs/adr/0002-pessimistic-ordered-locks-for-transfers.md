# Pessimistic row locks, acquired in id order, for Transfers

A Transfer locks both Accounts with `SELECT ... FOR UPDATE` in ascending account id order inside one transaction, then checks the Balance. Ordered acquisition rules out deadlocks between opposing Transfers, and locking guarantees the Balance never goes negative without retry logic. Optimistic versioning with retries was rejected: it adds failure and retry paths to a high-stakes operation, and the Interest accrual job updates the same rows.
