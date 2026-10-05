# Radiant AI Spark Starter — Judge Evidence

## Problem solved

Meeting someone is not the same as knowing what to say. Radiant Circle already discovers accepted social connections through Shared Sparks. AI Spark Starter turns those shared interests into an optional first-message suggestion.

## User flow

1. Two users become accepted Circle connections.
2. Open the private Circle chat.
3. Tap **Generate AI icebreaker**.
4. The backend verifies the accepted Circle edge and derives Shared Sparks from both profiles.
5. OpenAI GPT-6 Luna generates one short contextual conversation starter.
6. The user can request another suggestion or tap **Use this** to place it in the composer.
7. The user still chooses whether to send the message. AI never sends on the user's behalf.

## Architecture

Android -> Firebase callable `generateSparkStarter` -> Firestore relationship/profile verification -> OpenAI Responses API (`gpt-6-luna`) -> suggestion -> Android composer.

## Privacy and security

- The OpenAI API key is stored only in Google Secret Manager through Firebase Functions secrets.
- No OpenAI credential is embedded in the APK or committed to Git.
- Android sends only the accepted peer UID to the callable function.
- The backend verifies the Circle relationship before generating.
- Shared interests are read server-side from existing Circle profiles.
- Wallet address, ORE/SKR balances, email, location, private chat history, seed phrases, and signing material are not sent to OpenAI.
- Generation is limited to 20 requests per authenticated user per UTC day.
- OpenAI request storage is disabled with `store: false`.

## Implementation evidence

- Android client: `app/src/main/java/com/thinkblox/radiantrush/firebase/AiSparkStarterRepository.kt`
- UI: `app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt`
- UI/application state: `app/src/main/java/com/thinkblox/radiantrush/data/CircleModels.kt` and `ui/RadiantRushApp.kt`
- Server function: `functions/index.js`
- Firebase Functions configuration: `firebase.json` and `functions/package.json`

## Deployment

The Firebase project must be on the Blaze plan to deploy Cloud Functions. From the repository root:

```bash
firebase login:list
firebase projects:list
firebase use --add
cd functions
npm install
cd ..
firebase functions:secrets:set OPENAI_API_KEY
firebase deploy --only functions:generateSparkStarter
```

Never place the OpenAI API key in `.env` committed to Git, Android resources, `BuildConfig`, `local.properties`, or `google-services.json`.

## Variation and anti-repeat behavior

The production backend keeps a small server-side history of the last six AI Spark Starters for each accepted Circle pair. Each generation rotates the focus across the pair's verified Shared Sparks and across multiple conversation angles (recommendation, recent experience, playful either-or, opinion, next-to-try, and memorable moment). Different Circle pairs receive different initial rotation offsets. Previous generated starters are supplied only as anti-repeat context so "Another" produces a genuinely different suggestion without exposing private chat history.


## Concurrency-safe variation

The backend reserves a unique generation sequence for each Circle pair inside a Firestore transaction before calling OpenAI. If both people tap **Another** at nearly the same time, transaction retries ensure they receive different sequence numbers, which select different focus/angle combinations instead of both generating from the same stale pair state. Generated text is then appended to the pair's recent anti-repeat history.

## Compact chat UI

The AI Spark Starter panel is collapsed by default so it does not displace private chat messages. A persistent **Show** control expands it on demand; **Hide** collapses it again. Selecting **Use this** fills the human-controlled composer and automatically collapses the AI panel. AI never sends a message automatically.
