# Next Chat Start Prompt

Copy this into the next ChatGPT/project session when continuing the Android/Solana hackathon app.

```text
We are continuing the RadiantSolanaHackatonAndroid project. I gave you Blox Monster Adventure only as a reference source. Do not port Roblox/Luau code. Use the extracted BMA audit lessons to enforce source authority, smallest safe patches, mobile-first UI, honest testing, and clear authority boundaries.

Before coding, read these docs in order:
1. README.md
2. AGENTS.md
3. ARCHITECTURE.md
4. SOLANA_SECURITY_AND_DATA_RULES.md
5. MOBILE_UI_UX_STANDARDS.md
6. TESTING_AND_RELEASE.md
7. BACKLOG_AND_ROADMAP.md
8. CHANGELOG.md
9. BMA_SOURCE_AUDIT_FOR_ANDROID.md

Your first job is to audit the actual Android source tree, identify the package/module structure, current build status, and the safest next step for a Solana hackathon demo app. Do not invent files, APIs, SDK versions, wallet behavior, transaction results, or test results. If you patch code, make the smallest focused change and report exact files changed plus checks actually run.
```


## Updated direction

Product direction is now `Radiant Rush`: a native Android Solana Mobile quest/streak app using Firebase, Mobile Wallet Adapter, Solana RPC, and SKR-powered boosts/status. Read `PHASED_DELIVERY_PLAN.md` before coding major features.
