import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
  SCHEMA_VERSION,
  SKR_DECIMALS,
} from "./weekly-cup-config.mjs";

export const FUNDING_VERIFICATION_AUTHORITY = "trusted-admin-phase12c";
export const FUNDING_VERIFICATION_SCHEMA_VERSION = 1;
export const FUNDING_NETWORK = "mainnet-beta";
export const FUNDING_COMMITMENT = "finalized";
export const DEFAULT_MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com";

const VERIFIABLE_CUP_STATUSES = new Set(["DRAFT", "ANNOUNCED", "OPEN"]);
const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;
const POSITIVE_ATOMIC = /^[0-9]{1,30}$/;

export function assertSolanaPublicAddress(value, label = "funding wallet") {
  const clean = String(value ?? "").trim();
  if (!SOLANA_ADDRESS.test(clean)) {
    throw new Error(`${label} must be a Solana public address (32-44 base58 characters).`);
  }
  return clean;
}

export function parsePositiveAtomic(value, label = "atomic amount") {
  const raw = String(value ?? "").trim();
  if (!POSITIVE_ATOMIC.test(raw)) {
    throw new Error(`${label} must be a non-negative integer string.`);
  }
  return BigInt(raw);
}

export function assertCupFundingTarget(cup, requestedWallet, { allowWalletReplace = false } = {}) {
  if (!cup) throw new Error("Weekly Cup config does not exist.");
  if (Number(cup.schemaVersion) !== SCHEMA_VERSION) {
    throw new Error(`Cup schemaVersion must be ${SCHEMA_VERSION} for Phase 12C verification.`);
  }
  if (String(cup.configurationAuthority ?? "") !== CONFIGURATION_AUTHORITY) {
    throw new Error("Cup configurationAuthority is not the trusted Phase 12B authority.");
  }
  if (!VERIFIABLE_CUP_STATUSES.has(String(cup.status ?? "").toUpperCase())) {
    throw new Error("Funding can be verified only while the Cup is DRAFT, ANNOUNCED, or OPEN.");
  }
  if (String(cup.prizeAssetSymbol ?? "").toUpperCase() !== "SKR") {
    throw new Error("Cup prize asset is not SKR.");
  }
  if (String(cup.prizeMint ?? "") !== OFFICIAL_SKR_MINT) {
    throw new Error("Cup prize mint does not match the official SKR mint.");
  }
  if (Number(cup.prizeDecimals) !== SKR_DECIMALS) {
    throw new Error(`Cup prizeDecimals must be ${SKR_DECIMALS}.`);
  }
  const requiredAtomic = parsePositiveAtomic(cup.prizeAmountAtomic, "Cup prizeAmountAtomic");
  if (requiredAtomic <= 0n) throw new Error("Cup prizeAmountAtomic must be greater than zero.");
  if (cup.trustedResultsRequired !== true) {
    throw new Error("Cup must require trusted results before funding verification can be used.");
  }
  if (cup.payoutEnabled === true) {
    throw new Error("Cup has payoutEnabled=true. Phase 12C refuses to verify a payout-enabled config.");
  }

  const wallet = assertSolanaPublicAddress(requestedWallet);
  const existingWallet = String(cup.fundingWalletAddress ?? "").trim();
  if (existingWallet && existingWallet !== wallet && !allowWalletReplace) {
    throw new Error(
      `Cup already points to funding wallet ${existingWallet}. ` +
        "Use --replace-funding-wallet only when intentionally moving the prize source.",
    );
  }

  const existingAuthority = String(cup.fundingVerificationAuthority ?? "").trim();
  if (existingAuthority && existingAuthority !== FUNDING_VERIFICATION_AUTHORITY) {
    throw new Error(`Unknown funding verification authority: ${existingAuthority}. Refusing to overwrite it.`);
  }

  return {
    wallet,
    requiredAtomic,
    weekKey: String(cup.weekKey ?? "").trim(),
  };
}

export function buildGetTokenAccountsByOwnerRequest(walletAddress) {
  const wallet = assertSolanaPublicAddress(walletAddress);
  return {
    jsonrpc: "2.0",
    id: 1,
    method: "getTokenAccountsByOwner",
    params: [
      wallet,
      { mint: OFFICIAL_SKR_MINT },
      { encoding: "jsonParsed", commitment: FUNDING_COMMITMENT },
    ],
  };
}

export function parseTokenAccountsByOwnerResponse(payload, expectedWalletAddress = null) {
  const root = typeof payload === "string" ? JSON.parse(payload) : payload;
  if (!root || typeof root !== "object") throw new Error("Solana RPC returned an invalid JSON response.");
  if (root.error) {
    const message = root.error?.message || "Unknown Solana RPC error";
    throw new Error(`Solana mainnet RPC error: ${message}`);
  }

  const result = root.result;
  if (!result || typeof result !== "object") {
    throw new Error("Solana RPC response did not include a result object.");
  }
  const slot = Number(result.context?.slot);
  if (!Number.isSafeInteger(slot) || slot <= 0) {
    throw new Error("Solana RPC response did not include a valid context slot.");
  }
  if (!Array.isArray(result.value)) {
    throw new Error("Solana RPC response did not include token-account values.");
  }

  let observedAtomic = 0n;
  let tokenAccountCount = 0;
  let frozenTokenAccountCount = 0;
  const expectedWallet = expectedWalletAddress == null ? null : assertSolanaPublicAddress(expectedWalletAddress);
  const seenTokenAccounts = new Set();

  for (const item of result.value) {
    const tokenAccountPubkey = String(item?.pubkey ?? "").trim();
    if (tokenAccountPubkey) {
      if (seenTokenAccounts.has(tokenAccountPubkey)) {
        throw new Error(`Solana RPC returned duplicate token account ${tokenAccountPubkey}.`);
      }
      seenTokenAccounts.add(tokenAccountPubkey);
    }
    const info = item?.account?.data?.parsed?.info;
    const tokenAmount = info?.tokenAmount;
    if (!info || !tokenAmount) {
      throw new Error("SKR token account was not jsonParsed as expected.");
    }
    if (String(info.mint ?? "") !== OFFICIAL_SKR_MINT) {
      throw new Error("RPC returned a token account for an unexpected mint.");
    }
    if (expectedWallet && String(info.owner ?? "") !== expectedWallet) {
      throw new Error("RPC returned an SKR token account owned by a different wallet.");
    }
    if (Number(tokenAmount.decimals) !== SKR_DECIMALS) {
      throw new Error(`RPC returned SKR token decimals=${tokenAmount.decimals}; expected ${SKR_DECIMALS}.`);
    }

    const raw = parsePositiveAtomic(tokenAmount.amount ?? "0", "SKR token amount");
    const state = String(info.state ?? "").toLowerCase();
    tokenAccountCount += 1;
    if (state === "frozen") {
      frozenTokenAccountCount += 1;
      continue;
    }
    if (state && state !== "initialized") {
      throw new Error(`SKR token account has unsupported state=${state}.`);
    }
    observedAtomic += raw;
  }

  return {
    observedAtomic,
    observedAmountAtomic: observedAtomic.toString(),
    tokenAccountCount,
    frozenTokenAccountCount,
    slot,
  };
}

export function buildFundingEvidence({ cup, walletAddress, snapshot, checkedAtEpochMillis = Date.now() }) {
  const { wallet, requiredAtomic } = assertCupFundingTarget(cup, walletAddress, {
    allowWalletReplace: true,
  });
  const observedAtomic = parsePositiveAtomic(snapshot?.observedAmountAtomic, "Observed SKR amount");
  const slot = Number(snapshot?.slot);
  if (!Number.isSafeInteger(slot) || slot <= 0) throw new Error("Funding snapshot requires a valid RPC slot.");
  const tokenAccountCount = Number(snapshot?.tokenAccountCount ?? 0);
  const frozenTokenAccountCount = Number(snapshot?.frozenTokenAccountCount ?? 0);
  if (!Number.isInteger(tokenAccountCount) || tokenAccountCount < 0) {
    throw new Error("Funding snapshot tokenAccountCount must be a non-negative integer.");
  }
  if (!Number.isInteger(frozenTokenAccountCount) || frozenTokenAccountCount < 0) {
    throw new Error("Funding snapshot frozenTokenAccountCount must be a non-negative integer.");
  }
  const checkedAt = Number(checkedAtEpochMillis);
  if (!Number.isSafeInteger(checkedAt) || checkedAt <= 0) {
    throw new Error("Funding snapshot requires a valid check timestamp.");
  }

  const verificationStatus = observedAtomic >= requiredAtomic ? "VERIFIED" : "NOT_VERIFIED";
  return {
    fundingWalletAddress: wallet,
    fundingVerificationStatus: verificationStatus,
    fundingRequiredAmountAtomic: requiredAtomic.toString(),
    fundingObservedAmountAtomic: observedAtomic.toString(),
    fundingTokenAccountCount: tokenAccountCount,
    fundingFrozenTokenAccountCount: frozenTokenAccountCount,
    fundingVerificationSlot: slot,
    fundingVerificationNetwork: FUNDING_NETWORK,
    fundingVerificationMint: OFFICIAL_SKR_MINT,
    fundingVerificationCommitment: FUNDING_COMMITMENT,
    fundingVerificationAuthority: FUNDING_VERIFICATION_AUTHORITY,
    fundingVerificationSchemaVersion: FUNDING_VERIFICATION_SCHEMA_VERSION,
    fundingCheckedAtEpochMillis: checkedAt,
    fundingVerifiedAtEpochMillis: verificationStatus === "VERIFIED" ? checkedAt : null,
    payoutEnabled: false,
  };
}

export function formatAtomicSkr(value) {
  const atomic = BigInt(String(value));
  const divisor = 10n ** BigInt(SKR_DECIMALS);
  const whole = atomic / divisor;
  const fraction = (atomic % divisor).toString().padStart(SKR_DECIMALS, "0").replace(/0+$/, "");
  return `${whole}${fraction ? `.${fraction}` : ""} SKR`;
}

export function fundingFieldsForPreservation(existing) {
  if (!existing || typeof existing !== "object") return {};
  const names = [
    "fundingWalletAddress",
    "fundingVerificationStatus",
    "fundingRequiredAmountAtomic",
    "fundingObservedAmountAtomic",
    "fundingTokenAccountCount",
    "fundingFrozenTokenAccountCount",
    "fundingVerificationSlot",
    "fundingVerificationNetwork",
    "fundingVerificationMint",
    "fundingVerificationCommitment",
    "fundingVerificationAuthority",
    "fundingVerificationSchemaVersion",
    "fundingCheckedAt",
    "fundingVerifiedAt",
  ];
  return Object.fromEntries(names.filter((name) => existing[name] !== undefined).map((name) => [name, existing[name]]));
}
