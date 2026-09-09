$ErrorActionPreference = "Stop"

.\gradlew.bat --stop
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug

Write-Host ""
Write-Host "Phase 7 local automated checks passed."
Write-Host ""
Write-Host "Optional emulator/phone smoke test:"
Write-Host "  .\gradlew.bat :app:connectedDebugAndroidTest"
Write-Host ""
Write-Host "Manual wallet QA still required for Phantom/MWA approval:"
Write-Host "  Connect Wallet -> Sign Daily Proof -> Send Memo Proof -> Scan SKR Passport"
