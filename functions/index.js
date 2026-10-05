import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { defineSecret } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import OpenAI from "openai";

initializeApp();

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");
const db = getFirestore();

const MAX_GENERATIONS_PER_DAY = 20;
const MAX_SHARED_SPARKS = 5;
const MAX_OUTPUT_CHARS = 180;
const MAX_PAIR_HISTORY = 6;

const STARTER_ANGLES = [
  "ask for a recommendation",
  "ask about a recent experience",
  "ask a playful either-or question",
  "ask for an opinion or hot take",
  "ask what they would try next",
  "ask for a short story or memorable moment",
];

const INTEREST_FIELDS = [
  ["favoriteFood", "Favorite food"],
  ["music", "Music"],
  ["games", "Games"],
  ["hobbies", "Hobbies"],
  ["books", "Books"],
  ["pets", "Pets"],
  ["currentlyInto", "Currently into"],
  ["weekendVibe", "Weekend vibe"],
  ["talkAbout", "Talk for hours"],
];

function pairId(uidA, uidB) {
  return [uidA.trim(), uidB.trim()].sort().join("__");
}

function tokens(raw) {
  return String(raw ?? "")
    .split(/[,/;|•\n]/)
    .map((value) => value.replace(/\s+/g, " ").trim())
    .filter(Boolean)
    .map((display) => ({ display, normalized: display.toLocaleLowerCase("en-US") }));
}

function sharedSparks(mine, theirs) {
  const matches = [];
  for (const [field, label] of INTEREST_FIELDS) {
    const mineTokens = tokens(mine?.[field]);
    const theirTokens = tokens(theirs?.[field]);
    const theirsSet = new Set(theirTokens.map((token) => token.normalized));
    const shared = mineTokens.find((token) => theirsSet.has(token.normalized));
    if (shared) matches.push({ label, value: shared.display });
    if (matches.length >= MAX_SHARED_SPARKS) break;
  }
  return matches;
}


function stableHash(raw) {
  let hash = 2166136261;
  for (const char of String(raw)) {
    hash ^= char.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

async function reservePairGeneration(uid, peerUid) {
  const id = pairId(uid, peerUid);
  const ref = db.collection("aiSparkPairHistory").doc(id);

  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const data = snap.data() ?? {};
    const generationIndex = Number(data.generationCount ?? 0);
    const starters = Array.isArray(data.starters)
      ? data.starters.map((value) => cleanStarter(value)).filter(Boolean).slice(-MAX_PAIR_HISTORY)
      : [];

    // Reserve the next sequence number before OpenAI is called. Firestore transaction
    // retries make simultaneous requests for the same Circle pair receive different
    // generation indexes instead of choosing the same focus/angle.
    tx.set(
      ref,
      {
        memberUids: [uid, peerUid].sort(),
        generationCount: generationIndex + 1,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );

    return {
      ref,
      pairKey: id,
      generationIndex,
      starters,
    };
  });
}

async function savePairStarter(ref, starter) {
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const data = snap.data() ?? {};
    const existing = Array.isArray(data.starters)
      ? data.starters.map((value) => cleanStarter(value)).filter(Boolean)
      : [];
    tx.set(
      ref,
      {
        starters: [...existing, starter].slice(-MAX_PAIR_HISTORY),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
  });
}

function utcDayKey() {
  return new Date().toISOString().slice(0, 10);
}

async function reserveDailyGeneration(uid) {
  const ref = db.collection("aiSparkUsage").doc(`${uid}__${utcDayKey()}`);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const current = Number(snap.get("count") ?? 0);
    if (current >= MAX_GENERATIONS_PER_DAY) {
      throw new HttpsError("resource-exhausted", "Daily AI Spark Starter limit reached.");
    }
    tx.set(
      ref,
      {
        uid,
        dayKey: utcDayKey(),
        count: current + 1,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
  });
}

function cleanStarter(raw) {
  return String(raw ?? "")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/^["“”']+|["“”']+$/g, "")
    .slice(0, MAX_OUTPUT_CHARS)
    .trim();
}

export const generateSparkStarter = onCall(
  {
    region: "us-central1",
    secrets: [OPENAI_API_KEY],
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign in before using Radiant AI.");

    const peerUid = String(request.data?.peerUid ?? "").trim();
    if (!peerUid || peerUid === uid || peerUid.length > 128) {
      throw new HttpsError("invalid-argument", "Choose a valid Circle connection.");
    }

    const edgeRef = db.collection("circleEdges").doc(pairId(uid, peerUid));
    const edge = await edgeRef.get();
    const members = Array.isArray(edge.get("memberUids")) ? edge.get("memberUids") : [];
    if (!edge.exists || edge.get("status") !== "ACCEPTED" || !members.includes(uid) || !members.includes(peerUid)) {
      throw new HttpsError("permission-denied", "AI Spark Starter requires an accepted Circle connection.");
    }

    const [mineSnap, theirsSnap] = await Promise.all([
      db.collection("circleProfiles").doc(uid).get(),
      db.collection("circleProfiles").doc(peerUid).get(),
    ]);
    if (!mineSnap.exists || !theirsSnap.exists) {
      throw new HttpsError("failed-precondition", "Both Circle profiles need shared interests first.");
    }

    const sparks = sharedSparks(mineSnap.data(), theirsSnap.data());
    if (sparks.length === 0) {
      throw new HttpsError("failed-precondition", "No shared Sparks were found for this Circle connection.");
    }

    await reserveDailyGeneration(uid);

    const reservation = await reservePairGeneration(uid, peerUid);
    const seed = stableHash(reservation.pairKey);
    const sequence = reservation.generationIndex;
    const focusSpark = sparks[(seed + sequence) % sparks.length];
    const angle = STARTER_ANGLES[(seed + sequence) % STARTER_ANGLES.length];

    const openai = new OpenAI({ apiKey: OPENAI_API_KEY.value() });
    const context = sparks.map((spark) => `${spark.label}: ${spark.value}`).join("\n");
    const previous = reservation.starters.length
      ? reservation.starters.map((starter, index) => `${index + 1}. ${starter}`).join("\n")
      : "None yet";

    try {
      const response = await openai.responses.create({
        model: "gpt-6-luna",
        store: false,
        reasoning: { effort: "none" },
        max_output_tokens: 60,
        instructions:
          "You are Radiant Spark, a friendly social icebreaker assistant inside Radiant Circle. " +
          "Generate exactly one natural, casual first-message idea grounded in the users' real shared interests. " +
          "Keep it under 20 words. Make it sound like something a real person would actually send, not an interview question. " +
          "Be friendly and non-romantic by default. Never invent personal facts. Never mention location, money, wallets, " +
          "crypto holdings, private data, or AI. Never repeat or closely paraphrase a previous starter. " +
          "Return only the conversation starter with no quotation marks or explanation.",
        input:
          `All Shared Sparks:\n${context}\n\n` +
          `Focus this attempt on: ${focusSpark.label}: ${focusSpark.value}\n` +
          `Conversation angle: ${angle}\n\n` +
          `Previous starters to avoid:\n${previous}`,
      });

      const starter = cleanStarter(response.output_text);
      if (!starter) throw new Error("OpenAI returned empty output_text");
      if (reservation.starters.some((previousStarter) => previousStarter.toLocaleLowerCase("en-US") === starter.toLocaleLowerCase("en-US"))) {
        throw new Error("OpenAI repeated a previous starter");
      }

      await savePairStarter(reservation.ref, starter);

      logger.info("AI Spark Starter generated", {
        uid,
        peerUid,
        sharedSparkCount: sparks.length,
        focusSparkLabel: focusSpark.label,
        angle,
        generationSequence: sequence,
        priorStarterCount: reservation.starters.length,
        model: "gpt-6-luna",
      });

      return {
        starter,
        sharedSparkCount: sparks.length,
        model: "gpt-6-luna",
      };
    } catch (error) {
      logger.error("AI Spark Starter generation failed", {
        uid,
        peerUid,
        message: error instanceof Error ? error.message : String(error),
      });
      throw new HttpsError("internal", "Radiant AI couldn't generate a starter right now.");
    }
  },
);
