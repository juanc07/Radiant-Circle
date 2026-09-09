#!/usr/bin/env bash
set -euo pipefail

./gradlew --stop
./gradlew :app:testDebugUnitTest :app:assembleDebug

cat <<'MSG'

Phase 7 local automated checks passed.

Optional emulator/phone smoke test:
  ./gradlew :app:connectedDebugAndroidTest

Manual wallet QA still required for Phantom/MWA approval:
  Connect Wallet -> Sign Daily Proof -> Send Memo Proof -> Scan SKR Passport
MSG
