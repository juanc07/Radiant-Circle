# Radiant Rush — Known Limitations

## Memo proof wallet handoff

The devnet memo proof depends on external wallet authorization through Mobile Wallet Adapter. On some fresh wallet sessions, especially after reinstalling the app or clearing wallet state, the first memo proof attempt may require a retry.

Current mitigation:

- The app locks buttons while actions are running.
- The app shows clearer syncing/loading states.
- The memo flow includes an authorization refresh retry path.
- The profile stores proof data after success.

Demo recommendation:

- Connect wallet first.
- Then sign daily proof.
- Then send memo proof.
- If memo does not return immediately, tap retry once and approve the wallet transaction.

## SKR balance on test wallets

A test wallet may have zero SKR. This is expected. The SKR Passport quest can still complete honestly because the app performed a real read-only scan of the connected wallet.

Zero SKR behavior:

- Balance: `0 SKR`.
- Tier: `Explorer`.
- SKR Radiant badge remains locked.
- No fake balance is created.

## Devnet vs mainnet

Radiant Rush intentionally uses two network behaviors:

- Devnet for safe memo proof transactions.
- Mainnet-beta read-only RPC for SKR balance scan.

This avoids fake SKR and avoids risky token movement.

## Automated testing limit

Automated tests cover business rules, UI smoke checks, and APK build. Wallet approval cannot be fully automated safely because it depends on an external wallet consent screen.

Manual test still required:

- Wallet connect.
- Message signing.
- Memo transaction approval.
- SKR scan on real device.
