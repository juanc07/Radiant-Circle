#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { applicationDefault, getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { DEFAULT_PROJECT_ID, buildSummary } from "./radiant-cup-operator.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function readArgs(argv) {
  const positional = [];
  const values = {};
  const flags = new Set();
  const booleanFlags = new Set(["apply", "force-close-early", "reset-failed", "replace-funding-wallet"]);
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) {
      positional.push(token);
      continue;
    }
    const key = token.slice(2);
    if (booleanFlags.has(key)) {
      flags.add(key);
      continue;
    }
    const value = argv[i + 1];
    if (!value || value.startsWith("--")) throw new Error(`Missing value for --${key}`);
    values[key] = value;
    i += 1;
  }
  return { command: positional[0] ?? "help", values, flags };
}

function usage() {
  return `
Radiant Cup Operator — safe orchestration layer over the proven Phase 12 tools

Read-only dashboard:
  node radiant-cup.mjs status --week 2026-W39

Create/update Cup from a small config file (DRY RUN by default):
  node radiant-cup.mjs setup --config cups/2026-W39.json
  node radiant-cup.mjs setup --config cups/2026-W39.json --apply --confirm-project ${DEFAULT_PROJECT_ID}

Verify liquid SKR funding (DRY RUN by default):
  node radiant-cup.mjs funding --week 2026-W39 --funding-wallet <PUBLIC_WALLET>
  ...add --apply --confirm-project ${DEFAULT_PROJECT_ID} after review

Finalize (DRY RUN by default):
  node radiant-cup.mjs finalize --week 2026-W39
  ...add --apply --confirm-project ${DEFAULT_PROJECT_ID} after review

Prepare payout manifest (DRY RUN by default):
  node radiant-cup.mjs payout-prepare --week 2026-W39
  ...add --apply --confirm-project ${DEFAULT_PROJECT_ID} after review

Approve payout (digest remains an explicit human gate):
  node radiant-cup.mjs payout-approve --week 2026-W39 --review-ref operator-review-w39 --confirm-digest <SHA256>
  ...add --apply --confirm-project ${DEFAULT_PROJECT_ID} after review

Dry-run / execute one winner transfer (exact values remain explicit):
  node radiant-cup.mjs transfer --week 2026-W39 --placement 1
  node radiant-cup.mjs transfer --week 2026-W39 --placement 1 \\
    --confirm-digest <SHA256> --confirm-funding-wallet <WALLET> \\
    --confirm-recipient <WINNER> --confirm-amount-atomic <ATOMIC> \\
    --apply --confirm-project ${DEFAULT_PROJECT_ID}

Reconcile one transfer:
  node radiant-cup.mjs reconcile --week 2026-W39 --placement 1 [--signature <SIG>]
  ...add --apply --confirm-project ${DEFAULT_PROJECT_ID} only when the underlying reconciliation tool requests it

Complete payout after every winner item is PAID/FINALIZED:
  node radiant-cup.mjs payout-complete --week 2026-W39 --confirm-digest <SHA256>
  ...add --apply --confirm-project ${DEFAULT_PROJECT_ID} after review

Generate judge/operator evidence report:
  node radiant-cup.mjs report --week 2026-W39 --out docs/evidence/RADIANT_CUP_2026_W39_REPORT.md

Common:
  --project <firebase-project>   defaults to ${DEFAULT_PROJECT_ID}

Safety model:
- status/report are read-only
- all wrapped existing tools stay DRY RUN unless --apply is supplied
- exact project/digest/wallet/recipient/amount confirmations are preserved
- this operator never auto-verifies a player run and never silently sends SKR
`;
}

function projectFrom(values) {
  return String(values.project ?? DEFAULT_PROJECT_ID).trim();
}

function required(values, key) {
  const value = String(values[key] ?? "").trim();
  if (!value) throw new Error(`--${key} is required.`);
  return value;
}

function optionalArg(args, values, key) {
  const value = values[key];
  if (value != null && String(value).trim()) args.push(`--${key}`, String(value));
}

function forwardFlags(args, flags) {
  for (const flag of ["apply", "force-close-early", "reset-failed", "replace-funding-wallet"]) {
    if (flags.has(flag)) args.push(`--${flag}`);
  }
}

function runExisting(scriptName, args) {
  const script = path.join(__dirname, scriptName);
  console.log(`\n→ ${scriptName} ${args.join(" ")}\n`);
  const result = spawnSync(process.execPath, [script, ...args], {
    cwd: __dirname,
    env: process.env,
    stdio: "inherit",
    windowsHide: true,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status ?? 1);
}

async function loadSummary(projectId, weekKey) {
  if (getApps().length === 0) initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const resultRef = db.collection("weeklyCupResults").doc(weekKey);
  const payoutRef = db.collection("weeklyCupPayouts").doc(weekKey);
  const [cupSnap, receiptsSnap, verificationsSnap, locksSnap, resultSnap, winnersSnap, payoutSnap, payoutItemsSnap] = await Promise.all([
    cupRef.get(),
    db.collection("competitionRunSubmissions").where("utcWeekKey", "==", weekKey).get(),
    db.collection("competitionRunVerifications").where("weekKey", "==", weekKey).get(),
    db.collection("weeklyCupCompetitionWalletLocks").doc(weekKey).collection("accounts").get(),
    resultRef.get(),
    resultRef.collection("winners").orderBy("placement").get(),
    payoutRef.get(),
    payoutRef.collection("items").orderBy("placement").get(),
  ]);
  return buildSummary({
    cup: cupSnap.exists ? cupSnap.data() : null,
    receipts: receiptsSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
    verifications: verificationsSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
    locks: locksSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
    result: resultSnap.exists ? resultSnap.data() : null,
    winners: winnersSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
    payout: payoutSnap.exists ? payoutSnap.data() : null,
    payoutItems: payoutItemsSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
  });
}

function iso(ms) {
  return ms > 0 ? new Date(ms).toISOString() : "—";
}

function short(value, left = 6, right = 5) {
  const text = String(value ?? "");
  if (text.length <= left + right + 3) return text || "—";
  return `${text.slice(0, left)}…${text.slice(-right)}`;
}

function printSummary(summary, projectId, weekKey) {
  console.log("\n╔════════════════════════════════════════════════════════════╗");
  console.log(`  RADIANT CUP OPERATOR — ${weekKey}`);
  console.log("╚════════════════════════════════════════════════════════════╝");
  console.log(`Project:              ${projectId}`);
  console.log(`Cup status:           ${summary.cupStatus}`);
  console.log(`Prize:                ${summary.prizeAmountUi} ${summary.prizeAssetSymbol}`);
  console.log(`Funding:              ${summary.fundingStatus}`);
  if (summary.fundingWalletAddress) console.log(`Funding wallet:       ${short(summary.fundingWalletAddress)}`);
  if (summary.fundingStatus === "VERIFIED") console.log(`Observed liquid SKR:  ${summary.fundingObservedUi}`);
  console.log(`Starts:               ${iso(summary.cupStartsAtMillis)}`);
  console.log(`Ends:                 ${iso(summary.cupEndsAtMillis)}`);
  console.log(`Participants/locks:   ${summary.participants} / ${summary.walletLockCount}`);
  console.log(`Ranked receipts:      ${summary.receiptCount}`);
  console.log(`  verified:           ${summary.verifiedReceipts}`);
  console.log(`  rejected:           ${summary.rejectedReceipts}`);
  console.log(`  needs review:       ${summary.unverifiedReceipts}`);
  console.log(`Result:               ${summary.resultStatus}`);
  console.log(`Winners:              ${summary.winnerCount}`);
  console.log(`Payout:               ${summary.payoutStatus}`);
  if (summary.payoutManifestDigestSha256) console.log(`Manifest SHA-256:     ${summary.payoutManifestDigestSha256}`);
  if (summary.payoutItems.length) {
    console.log("Payout items:");
    for (const item of summary.payoutItems) {
      console.log(`  #${item.placement} ${item.amountUi || item.amountAtomic || "?"} SKR → ${short(item.recipientWalletAddress)} | ${item.status}/${item.transferStatus}${item.transactionSignature ? ` | ${short(item.transactionSignature, 8, 8)}` : ""}`);
    }
  }
  console.log("\nNEXT SAFE ACTION");
  console.log(`${summary.nextAction.code}: ${summary.nextAction.label}`);
}

async function commandStatus(values) {
  const projectId = projectFrom(values);
  const weekKey = required(values, "week");
  const summary = await loadSummary(projectId, weekKey);
  printSummary(summary, projectId, weekKey);
}

async function commandReport(values) {
  const projectId = projectFrom(values);
  const weekKey = required(values, "week");
  const summary = await loadSummary(projectId, weekKey);
  const lines = [
    `# Radiant Cup ${weekKey} — Operator Evidence Report`,
    "",
    `- Firebase project: \`${projectId}\``,
    `- Cup status: **${summary.cupStatus}**`,
    `- Prize: **${summary.prizeAmountUi} ${summary.prizeAssetSymbol}**`,
    `- Funding: **${summary.fundingStatus}**`,
    `- Funding wallet: \`${summary.fundingWalletAddress || "not configured"}\``,
    `- Cup starts: ${iso(summary.cupStartsAtMillis)}`,
    `- Cup ends: ${iso(summary.cupEndsAtMillis)}`,
    `- Competition wallet locks: ${summary.walletLockCount}`,
    `- Ranked receipts: ${summary.receiptCount}`,
    `- Trusted VERIFIED receipts: ${summary.verifiedReceipts}`,
    `- Rejected receipts: ${summary.rejectedReceipts}`,
    `- Receipts awaiting review: ${summary.unverifiedReceipts}`,
    `- Trusted result: **${summary.resultStatus}**`,
    `- Winner count: ${summary.winnerCount}`,
    `- Payout status: **${summary.payoutStatus}**`,
    `- Payout manifest SHA-256: \`${summary.payoutManifestDigestSha256 || "not prepared"}\``,
    "",
    "## Winners",
    "",
  ];
  if (summary.winners.length === 0) lines.push("No finalized winners yet.");
  for (const winner of summary.winners) {
    lines.push(`- #${winner.placement}: wallet \`${winner.walletAddress ?? winner.competitionWalletAddress ?? "unknown"}\`, score ${winner.score ?? "unknown"}`);
  }
  lines.push("", "## Payout items", "");
  if (summary.payoutItems.length === 0) lines.push("No payout manifest yet.");
  for (const item of summary.payoutItems) {
    lines.push(`- #${item.placement}: ${item.amountUi || item.amountAtomic || "?"} SKR → \`${item.recipientWalletAddress}\` — ${item.status}/${item.transferStatus}${item.transactionSignature ? ` — tx \`${item.transactionSignature}\`` : ""}`);
  }
  lines.push("", "## Next safe action", "", `**${summary.nextAction.code}** — ${summary.nextAction.label}`, "", "---", "Generated by `scripts/firebase-admin/radiant-cup.mjs report`. This report is an operational snapshot, not a replacement for immutable Firestore/Solana evidence.", "");
  const output = lines.join("\n");
  if (values.out) {
    const outPath = path.resolve(process.cwd(), values.out);
    await mkdir(path.dirname(outPath), { recursive: true });
    await writeFile(outPath, output, "utf8");
    console.log(`Report written: ${outPath}`);
  } else {
    console.log(output);
  }
}

async function commandSetup(values, flags) {
  const configPath = path.resolve(process.cwd(), required(values, "config"));
  const config = JSON.parse(await readFile(configPath, "utf8"));
  const projectId = String(values.project ?? config.project ?? DEFAULT_PROJECT_ID);
  const args = [
    "--project", projectId,
    "--week", String(config.week),
    "--status", String(config.status ?? "ANNOUNCED"),
    "--sponsor", String(config.sponsor),
    "--prize-skr", String(config.prizeSkr),
    "--note", String(config.note ?? "Trusted Weekly Radiant Cup."),
  ];
  if (config.fundingWallet) args.push("--funding-wallet", String(config.fundingWallet));
  if (config.placements) args.push("--placements", typeof config.placements === "string" ? config.placements : Object.entries(config.placements).map(([k, v]) => `${k}:${v}`).join(","));
  optionalArg(args, values, "confirm-project");
  forwardFlags(args, flags);
  runExisting("manage-weekly-cup.mjs", args);
}

async function commandWrapped(command, values, flags) {
  const projectId = projectFrom(values);
  const weekKey = required(values, "week");
  const args = ["--project", projectId, "--week", weekKey];
  let script;
  switch (command) {
    case "funding":
      script = "verify-weekly-cup-funding.mjs";
      args.push("--funding-wallet", required(values, "funding-wallet"));
      optionalArg(args, values, "confirm-project");
      break;
    case "finalize":
      script = "finalize-weekly-cup.mjs";
      optionalArg(args, values, "early-close-reason");
      optionalArg(args, values, "confirm-early-close");
      optionalArg(args, values, "confirm-ranking-digest");
      optionalArg(args, values, "confirm-project");
      break;
    case "payout-prepare":
      script = "prepare-weekly-cup-payout.mjs";
      optionalArg(args, values, "confirm-project");
      break;
    case "payout-approve":
      script = "approve-weekly-cup-payout.mjs";
      args.push("--review-ref", required(values, "review-ref"));
      optionalArg(args, values, "confirm-digest");
      optionalArg(args, values, "confirm-project");
      break;
    case "transfer":
      script = "execute-weekly-cup-skr-transfer.mjs";
      args.push("--placement", required(values, "placement"));
      for (const key of ["confirm-digest", "confirm-funding-wallet", "confirm-recipient", "confirm-amount-atomic", "confirm-project"]) optionalArg(args, values, key);
      break;
    case "reconcile":
      script = "reconcile-weekly-cup-skr-transfer.mjs";
      args.push("--placement", required(values, "placement"));
      optionalArg(args, values, "signature");
      optionalArg(args, values, "confirm-project");
      break;
    case "payout-complete":
      script = "complete-weekly-cup-payout.mjs";
      optionalArg(args, values, "confirm-digest");
      optionalArg(args, values, "confirm-project");
      break;
    default:
      throw new Error(`Unsupported wrapped command: ${command}`);
  }
  forwardFlags(args, flags);
  runExisting(script, args);
}

async function main() {
  const { command, values, flags } = readArgs(process.argv.slice(2));
  if (["help", "--help", "-h"].includes(command)) {
    console.log(usage());
    return;
  }
  if (command === "status") return commandStatus(values);
  if (command === "report") return commandReport(values);
  if (command === "setup") return commandSetup(values, flags);
  if (["funding", "finalize", "payout-prepare", "payout-approve", "transfer", "reconcile", "payout-complete"].includes(command)) {
    return commandWrapped(command, values, flags);
  }
  throw new Error(`Unknown command: ${command}\n${usage()}`);
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
