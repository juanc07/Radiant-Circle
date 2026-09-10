# Radiant Rush Phase 9.1.1 leaderboard duplicate fix

Changed-files-only patch. Copy the contents of this folder into the project root and overwrite matching files.

This fixes a legacy case where an old anonymous Firebase leaderboard row could still have a stale full `walletAddress` even after older code wrote `walletAddressShort = "No wallet"`.

After applying, run:

```bash
./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Then install/run on the phone and open **Ranks**. Each visible row now shows a short wallet label. The same wallet should appear only once. If two rows remain with two different short wallet labels, those are genuinely different wallet accounts and should not be merged automatically.
