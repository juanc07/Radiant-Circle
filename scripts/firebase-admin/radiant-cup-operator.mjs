export const DEFAULT_PROJECT_ID = "radiant-rush-10a9c";

export function asUpper(value, fallback = "UNKNOWN") {
  const text = String(value ?? "").trim();
  return text ? text.toUpperCase() : fallback;
}

export function timestampToMillis(value) {
  if (value == null) return 0;
  if (typeof value.toMillis === "function") return value.toMillis();
  if (typeof value.toDate === "function") return value.toDate().getTime();
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  if (typeof value === "object" && Number.isFinite(value._seconds)) {
    return Number(value._seconds) * 1000 + Math.floor(Number(value._nanoseconds ?? 0) / 1_000_000);
  }
  return 0;
}

export function formatAtomic(amountAtomic, decimals = 6) {
  const raw = String(amountAtomic ?? "0").trim();
  if (!/^\d+$/.test(raw)) return raw || "0";
  const scale = 10n ** BigInt(decimals);
  const amount = BigInt(raw);
  const whole = amount / scale;
  const fractionRaw = String(amount % scale).padStart(decimals, "0").replace(/0+$/, "");
  return fractionRaw ? `${whole}.${fractionRaw}` : String(whole);
}

export function deriveNextAction(summary, nowEpochMillis = Date.now()) {
  if (!summary.cupExists) {
    return { code: "SETUP_CUP", label: "Create the trusted Weekly Radiant Cup configuration." };
  }

  if (summary.fundingStatus !== "VERIFIED") {
    return { code: "VERIFY_FUNDING", label: "Verify the Cup funding wallet and liquid SKR on Solana." };
  }

  if (!summary.resultExists) {
    if (summary.unverifiedReceipts > 0) {
      return {
        code: "REVIEW_RUNS",
        label: `Review ${summary.unverifiedReceipts} unverified Ranked receipt${summary.unverifiedReceipts === 1 ? "" : "s"}.`,
      };
    }
    if (summary.cupEndsAtMillis > nowEpochMillis) {
      return { code: "WAIT_FOR_CUP_END", label: "Cup is funded and current trusted receipts are reviewed. Wait for the configured Cup end." };
    }
    return { code: "FINALIZE_CUP", label: "Dry-run finalization, review winners/digest, then finalize the Cup." };
  }

  if (!summary.payoutExists) {
    return { code: "PREPARE_PAYOUT", label: "Prepare the immutable review-only payout manifest." };
  }

  if (summary.payoutStatus === "READY_FOR_REVIEW") {
    return { code: "APPROVE_PAYOUT", label: "Review the payout manifest and explicitly approve its exact digest." };
  }

  const unpaid = summary.payoutItems.find((item) => asUpper(item.status) !== "PAID");
  if (unpaid) {
    const transferStatus = asUpper(unpaid.transferStatus, "NOT_STARTED");
    if (["SUBMITTED", "RECONCILIATION_REQUIRED"].includes(transferStatus)) {
      return {
        code: "RECONCILE_TRANSFER",
        label: `Reconcile placement #${unpaid.placement}; never resend an ambiguous transaction automatically.`,
        placement: unpaid.placement,
      };
    }
    if (transferStatus === "AWAITING_EXTERNAL_SIGNATURE") {
      return {
        code: "EXTERNAL_SIGNATURE",
        label: `Placement #${unpaid.placement} is awaiting the external sponsor-wallet signature.`,
        placement: unpaid.placement,
      };
    }
    return {
      code: "EXECUTE_TRANSFER",
      label: `Dry-run the approved SKR transfer for placement #${unpaid.placement}, review exact values, then explicitly execute/sign.`,
      placement: unpaid.placement,
    };
  }

  if (summary.payoutStatus !== "PAID") {
    return { code: "COMPLETE_PAYOUT", label: "All winner transfers are finalized. Complete the payout batch as PAID." };
  }

  return { code: "COMPLETE", label: "Cup lifecycle is complete and payout is PAID." };
}

export function buildSummary({ cup, receipts = [], verifications = [], locks = [], result, winners = [], payout, payoutItems = [] }) {
  const decisions = verifications.map((entry) => asUpper(entry.decision));
  const unverifiedReceipts = receipts.filter((entry) => asUpper(entry.verificationStatus, "UNVERIFIED") === "UNVERIFIED").length;
  const verifiedReceipts = decisions.filter((entry) => entry === "VERIFIED").length;
  const rejectedReceipts = decisions.filter((entry) => entry === "REJECTED").length;
  const ownerUids = new Set(receipts.map((entry) => String(entry.ownerUid ?? "").trim()).filter(Boolean));
  const decimals = Number.isInteger(Number(cup?.prizeDecimals)) ? Number(cup.prizeDecimals) : 6;

  const summary = {
    cupExists: Boolean(cup),
    weekKey: String(cup?.weekKey ?? result?.weekKey ?? payout?.weekKey ?? ""),
    cupStatus: asUpper(cup?.status, "NOT_CONFIGURED"),
    fundingStatus: asUpper(cup?.fundingVerificationStatus, "NOT_VERIFIED"),
    fundingWalletAddress: String(cup?.fundingWalletAddress ?? ""),
    prizeAssetSymbol: String(cup?.prizeAssetSymbol ?? "SKR"),
    prizeAmountAtomic: String(cup?.prizeAmountAtomic ?? "0"),
    prizeAmountUi: formatAtomic(cup?.prizeAmountAtomic ?? "0", decimals),
    fundingObservedUi: formatAtomic(cup?.fundingObservedAmountAtomic ?? "0", decimals),
    cupStartsAtMillis: timestampToMillis(cup?.startsAt),
    cupEndsAtMillis: timestampToMillis(cup?.endsAt),
    participants: Math.max(ownerUids.size, locks.length),
    receiptCount: receipts.length,
    unverifiedReceipts,
    verifiedReceipts,
    rejectedReceipts,
    walletLockCount: locks.length,
    resultExists: Boolean(result),
    resultStatus: asUpper(result?.finalizationStatus, result ? "UNKNOWN" : "NOT_FINALIZED"),
    winnerCount: winners.length,
    winners,
    payoutExists: Boolean(payout),
    payoutStatus: asUpper(payout?.status, payout ? "UNKNOWN" : "NOT_PREPARED"),
    payoutManifestDigestSha256: String(payout?.payoutManifestDigestSha256 ?? ""),
    payoutItems: payoutItems
      .map((item) => ({
        placement: Number(item.placement),
        status: asUpper(item.status),
        transferStatus: asUpper(item.transferStatus, "NOT_STARTED"),
        recipientWalletAddress: String(item.recipientWalletAddress ?? ""),
        amountAtomic: String(item.amountAtomic ?? ""),
        amountUi: String(item.amountUi ?? ""),
        transactionSignature: String(item.transactionSignature ?? ""),
      }))
      .sort((a, b) => a.placement - b.placement),
  };
  summary.nextAction = deriveNextAction(summary);
  return summary;
}
