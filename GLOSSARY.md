# Banking API

A REST service where registered users hold a single account whose balance grows by periodic interest and can be moved to other users by transfer.

## Language

**User**:
A registered person who signs in and owns exactly one Account. Users are never created through the API.
_Avoid_: Customer, client, member

**Account**:
The single money-holding record belonging to a User, carrying a Balance and an Initial deposit.
_Avoid_: Wallet

**Balance**:
The amount of money currently in an Account. Never negative.
_Avoid_: Funds, amount (for the account total)

**Initial deposit**:
The Balance an Account was opened with. Fixed for the life of the Account and unaffected by transfers or interest.
_Avoid_: Starting balance, principal

**Interest accrual**:
The scheduled increase of every Balance by 10% of its current value, never lifting it past the Interest cap.
_Avoid_: Bonus, yield

**Interest cap**:
207% of the Initial deposit, the highest Balance that Interest accrual alone may produce. A Balance already above it (from received Transfers) is left unchanged, never reduced.
_Avoid_: Limit, ceiling

**Transfer**:
A movement of money from the authenticated User's Account to another User's Account, recorded permanently.
_Avoid_: Payment, send, transaction

**Contact**:
An email address or phone number belonging to a User and unique across all Users. Each User keeps at least one email and one phone.
_Avoid_: Credential (a Contact is also used to log in, but is not a secret)
