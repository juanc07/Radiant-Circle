# Phase 4 Patch — Apply and Test

## Apply

Copy the patch contents into the project root and overwrite existing files.

## Build

```bash
./gradlew --stop
./gradlew :app:assembleDebug
```

PowerShell:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :app:assembleDebug
```

## Test on phone

Use Phantom in test/devnet mode.

```text
1. Open Radiant Rush.
2. Confirm Firebase sync says connected.
3. Connect wallet.
4. Tap Sign Daily Proof.
5. Approve in Phantom.
6. Confirm XP/badge updates and Firestore completedQuests has sign-daily-proof_<date>.
7. Tap On-Chain Memo Proof.
8. Approve in Phantom.
9. Confirm a transaction signature appears and Firestore completedQuests has on-chain-proof_<date>.
```

If the memo transaction fails, check that the Phantom test wallet has devnet SOL.
