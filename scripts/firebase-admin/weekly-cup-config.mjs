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

export function assertSafeExistingConfig(existing) {
  if (!existing) return;

  const schemaVersion = Number(existing.schemaVersion ?? 0);
  if (schemaVersion > SCHEMA_VERSION) {
    throw new Error(
      `Existing config uses newer schemaVersion=${schemaVersion}. Refusing to overwrite it with Phase 12B tooling.`,
    );
  }
  if (String(existing.fundingVerificationStatus ?? "").toUpperCase() === "VERIFIED") {
    throw new Error("Existing config has VERIFIED funding. Refusing to reset trusted Phase 12C+ state.");
  }
  if (existing.payoutEnabled === true) {
    throw new Error("Existing config has payoutEnabled=true. Refusing to overwrite trusted payout state.");
  }
}

function isoWeekKeyFromDate(date) {
  const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil((((d - yearStart) / 86_400_000) + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, "0")}`;
}
