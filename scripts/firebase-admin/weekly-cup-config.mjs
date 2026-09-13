export const OFFICIAL_SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3";
export const SKR_DECIMALS = 6;
export const SCHEMA_VERSION = 2;
export const CONFIGURATION_AUTHORITY = "trusted-admin-phase12b";

const ALLOWED_STATUSES = new Set(["DRAFT", "ANNOUNCED", "OPEN", "CLOSED", "CANCELLED"]);

export function isoWeekBounds(weekKey) {
  const match = /^(\d{4})-W(\d{2})$/.exec(String(weekKey ?? "").trim());
  if (!match) throw new Error("--week must use ISO format YYYY-Www, for example 2026-W37.");

  const year = Number(match[1]);
  const week = Number(match[2]);
  if (week < 1 || week > 53) throw new Error("ISO week must be between W01 and W53.");

  const jan4 = new Date(Date.UTC(year, 0, 4));
  const jan4MondayOffset = (jan4.getUTCDay() + 6) % 7;
  const weekOneMondayMs = jan4.getTime() - jan4MondayOffset * 86_400_000;
  const startMs = weekOneMondayMs + (week - 1) * 7 * 86_400_000;
  const endMs = startMs + 7 * 86_400_000;

  const normalized = isoWeekKeyFromDate(new Date(startMs));
  if (normalized !== `${year}-W${String(week).padStart(2, "0")}`) {
    throw new Error(`${weekKey} is not a valid ISO week for year ${year}.`);
  }

  return { startMs, endMs };
}

export function parseSkrAmountAtomic(value) {
  const raw = String(value ?? "").trim();
  if (!/^\d+(?:\.\d{1,6})?$/.test(raw)) {
    throw new Error("--prize-skr must be a positive decimal with at most 6 decimal places.");
  }

  const [whole, fraction = ""] = raw.split(".");
  const atomic = BigInt(whole) * 1_000_000n + BigInt((fraction + "000000").slice(0, 6));
  if (atomic <= 0n) throw new Error("--prize-skr must be greater than zero.");
  return atomic.toString();
}

export function parsePlacements(value = "1:50,2:30,3:20") {
  const result = {};
  let totalPercent = 0;

  for (const part of String(value).split(",")) {
    const [rankRaw, percentRaw] = part.split(":").map((item) => item?.trim());
    const rank = Number(rankRaw);
    const percent = Number(percentRaw);

    if (!Number.isInteger(rank) || rank < 1 || rank > 10) {
      throw new Error("Placement ranks must be whole numbers from 1 to 10.");
    }
    if (!Number.isInteger(percent) || percent <= 0 || percent > 100) {
      throw new Error("Placement percentages must be whole numbers from 1 to 100.");
    }
    if (Object.prototype.hasOwnProperty.call(result, String(rank))) {
      throw new Error(`Duplicate placement rank ${rank}.`);
    }

    result[String(rank)] = percent * 100;
    totalPercent += percent;
  }

  if (totalPercent !== 100) {
    throw new Error(`Placement percentages must total 100; received ${totalPercent}.`);
  }
  return result;
}

export function buildWeeklyCupConfig({
  weekKey,
  status,
  sponsorName,
  sponsorNote = null,
  prizeSkr,
  fundingWalletAddress = null,
  placements = "1:50,2:30,3:20",
}) {
  const cleanStatus = String(status ?? "").trim().toUpperCase();
  if (!ALLOWED_STATUSES.has(cleanStatus)) {
    throw new Error(`--status must be one of ${[...ALLOWED_STATUSES].join(", ")}.`);
  }

  const cleanSponsor = String(sponsorName ?? "").trim();
  if (!cleanSponsor || cleanSponsor.length > 80) {
    throw new Error("--sponsor is required and must be at most 80 characters.");
  }

  const cleanNote = sponsorNote == null ? null : String(sponsorNote).trim();
  if (cleanNote && cleanNote.length > 180) {
    throw new Error("--note must be at most 180 characters.");
  }

  const cleanFundingWallet = fundingWalletAddress == null ? null : String(fundingWalletAddress).trim();
  if (cleanFundingWallet && !/^[1-9A-HJ-NP-Za-km-z]{32,64}$/.test(cleanFundingWallet)) {
    throw new Error("--funding-wallet must look like a Solana public address (32-64 base58 characters).");
  }

  const { startMs, endMs } = isoWeekBounds(weekKey);
  const prizeAmountAtomic = parseSkrAmountAtomic(prizeSkr);
  const placementAllocationsBps = parsePlacements(placements);

  return {
    schemaVersion: SCHEMA_VERSION,
    weekKey,
    status: cleanStatus,
    sponsorName: cleanSponsor,
    sponsorNote: cleanNote || null,
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: SKR_DECIMALS,
    prizeAmountAtomic,
    placementAllocationsBps,
    startsAtEpochMillis: startMs,
    endsAtEpochMillis: endMs,
    fundingWalletAddress: cleanFundingWallet || null,
    fundingVerificationStatus: cleanFundingWallet ? "NOT_VERIFIED" : "NOT_CONFIGURED",
    trustedResultsRequired: true,
    payoutEnabled: false,
    configurationAuthority: CONFIGURATION_AUTHORITY,
  };
}

export function assertSafeExistingConfig(existing, proposed = null) {
  if (!existing) return { preservePhase12cEvidence: false };

  const schemaVersion = Number(existing.schemaVersion ?? 0);
  if (schemaVersion > SCHEMA_VERSION) {
    throw new Error(
      `Existing config uses newer schemaVersion=${schemaVersion}. Refusing to overwrite it with Phase 12B tooling.`,
    );
  }
  if (existing.payoutEnabled === true) {
    throw new Error("Existing config has payoutEnabled=true. Refusing to overwrite trusted payout state.");
  }

  const hasPhase12cEvidence =
    String(existing.fundingVerificationAuthority ?? "").trim() === "trusted-admin-phase12c" ||
    existing.fundingCheckedAt != null ||
    existing.fundingRequiredAmountAtomic != null ||
    existing.fundingObservedAmountAtomic != null;

  if (hasPhase12cEvidence) {
    if (!proposed) {
      throw new Error(
        "Existing config contains Phase 12C funding evidence. A proposed config is required to prove the update preserves it.",
      );
    }
    if (String(existing.prizeMint ?? "") !== String(proposed.prizeMint ?? "") ||
        Number(existing.prizeDecimals) !== Number(proposed.prizeDecimals) ||
        String(existing.prizeAmountAtomic ?? "") !== String(proposed.prizeAmountAtomic ?? "")) {
      throw new Error(
        "Phase 12C funding evidence is tied to the existing prize amount/mint. Re-verify funding before changing the prize.",
      );
    }
    const existingWallet = String(existing.fundingWalletAddress ?? "").trim();
    const proposedWallet = String(proposed.fundingWalletAddress ?? "").trim();
    if (proposedWallet && proposedWallet !== existingWallet) {
      throw new Error(
        "Phase 12C funding evidence is tied to the existing funding wallet. Use the Phase 12C verifier to replace/re-verify the wallet.",
      );
    }
    return { preservePhase12cEvidence: true };
  }

  if (String(existing.fundingVerificationStatus ?? "").toUpperCase() === "VERIFIED") {
    throw new Error("Existing config says VERIFIED without Phase 12C evidence. Refusing to overwrite ambiguous trusted state.");
  }

  return { preservePhase12cEvidence: false };
}

function isoWeekKeyFromDate(date) {
  const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil((((d - yearStart) / 86_400_000) + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, "0")}`;
}
