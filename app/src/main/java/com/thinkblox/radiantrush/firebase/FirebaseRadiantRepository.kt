package com.thinkblox.radiantrush.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.thinkblox.radiantrush.data.BadgePreview
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.LeaderboardPreview
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.RadiantChestPreview
import com.thinkblox.radiantrush.data.RadiantChestStatus
import com.thinkblox.radiantrush.data.RadiantRunPreview
import com.thinkblox.radiantrush.data.RetentionGoalPreview
import com.thinkblox.radiantrush.data.RetentionPreview
import com.thinkblox.radiantrush.data.RunCompetitionPreview
import com.thinkblox.radiantrush.data.RunLeaderboardPreview
import com.thinkblox.radiantrush.data.WeeklyCupPreview
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.data.UserPreview
import com.thinkblox.radiantrush.logic.LeaderboardCandidate
import com.thinkblox.radiantrush.logic.LeaderboardRules
import com.thinkblox.radiantrush.logic.Phase11CompetitionRules
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.logic.RetentionRules
import com.thinkblox.radiantrush.logic.RunCompetitionMode
import com.thinkblox.radiantrush.logic.RunLeaderboardCandidate
import com.thinkblox.radiantrush.logic.RunPersonalBest
import com.thinkblox.radiantrush.logic.WeeklyRunStats
import com.thinkblox.radiantrush.logic.WalletRunPersonalStats
import com.thinkblox.radiantrush.logic.WeeklyRadiantCupRules
import com.thinkblox.radiantrush.logic.RadiantGameRules
import com.thinkblox.radiantrush.logic.RadiantRunResult
import com.thinkblox.radiantrush.logic.RewardLoopRules
import com.thinkblox.radiantrush.logic.SkrPassportRules
import com.thinkblox.radiantrush.solana.SkrBalanceSnapshot
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Firebase Auth + Firestore repository for Radiant Circle.
 *
 * Phase 5 persists read-only SKR mainnet balance snapshots after the app
 * queries Solana RPC by public wallet address. Phase 9 adds a no-loss daily
 * Radiant Chest reward loop after all proof quests are completed. Phase 10
 * adds persistent Rush Tickets, the Radiant Rush score loop, and collectible
 * rewards. These are app progression only and never move SOL/SKR/tokens.
 */
class FirebaseRadiantRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun bootstrap(onState: (RushUiState) -> Unit) {
        onState(
            PreviewContent.defaultState().copy(
                firebaseStatus = FirebaseStatus.Loading,
                lastMessage = "Loading your progress…",
            ),
        )

        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Cloud progress is unavailable.",
                ),
            )
            return
        }

        val auth = FirebaseAuth.getInstance(app)
        val existingUser = auth.currentUser
        if (existingUser != null) {
            loadOrCreateProfile(existingUser.uid, onState)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    onState(errorState("Anonymous sign-in succeeded but no user id was returned."))
                } else {
                    loadOrCreateProfile(uid, onState)
                }
            }
            .addOnFailureListener { error ->
                onState(errorState("Anonymous sign-in failed: ${safeMessage(error)}"))
            }
    }

    fun completeDailyFirebaseCheckIn(
        quest: QuestPreview,
        onState: (RushUiState) -> Unit,
    ) {
        if (quest.id != QuestIds.DAILY_CHECK_IN) {
            onState(errorState("This quest is locked until its real implementation exists."))
            return
        }

        saveQuestProof(
            questId = quest.id,
            questTitle = quest.title,
            proofType = "firestore_phase5_progress",
            xpReward = quest.xp,
            walletAddress = null,
            proofFields = mapOf(
                "cloudOnly" to true,
            ),
            loadingMessage = "Saving today’s check-in…",
            successMessage = "Checked in for today.",
            onState = onState,
        )
    }

    fun saveWalletConnection(
        publicKey: String,
        accountLabel: String?,
        onState: (RushUiState) -> Unit,
    ) {
        val cleanPublicKey = publicKey.trim()
        if (cleanPublicKey.isBlank()) {
            onState(errorState("Wallet connection returned an empty public key."))
            return
        }

        val session = currentFirebaseSession(onState) ?: return
        onState(loadingState("Saving wallet…"))

        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val walletProofRef = userRef.collection(COMPLETED_QUESTS).document(QuestIds.WALLET_CONNECT)
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val walletProofSnapshot = transaction.get(walletProofRef)
            val firstWalletReward = !walletProofSnapshot.exists()
            val oldRushTickets = userSnapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()
            val newRushTickets = oldRushTickets + if (firstWalletReward) RadiantGameRules.QUEST_TICKET_REWARD else 0
            val displayName = userSnapshot.getString("displayName")
                ?: accountLabel?.takeIf { it.isNotBlank() }
                ?: "Radiant Rookie"
            val avatarId = PublicProfileRules.normalizeAvatarId(userSnapshot.getString("avatarId"))
            val xp = userSnapshot.getLong("xp") ?: 0L
            val level = userSnapshot.getLong("level") ?: levelForXp(xp)
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val skrTier = userSnapshot.getString("skrTier") ?: "Explorer"

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to cleanPublicKey,
                    "walletAddressShort" to shortenAddress(cleanPublicKey),
                    "walletAccountLabel" to accountLabel,
                    "walletStatus" to "Wallet connected",
                    "skrTier" to skrTier,
                    "xp" to xp,
                    "level" to level,
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "rushTickets" to newRushTickets,
                    "phase" to 10,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                walletProofRef,
                mapOf(
                    "questId" to QuestIds.WALLET_CONNECT,
                    "questTitle" to "Wallet Ready",
                    "date" to todayKey(),
                    "proofType" to "mwa_authorization_public_key",
                    "walletAddress" to cleanPublicKey,
                    "walletAddressShort" to shortenAddress(cleanPublicKey),
                    "xpEarned" to 0,
                    "rushTicketsEarned" to if (firstWalletReward) RadiantGameRules.QUEST_TICKET_REWARD else 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to cleanPublicKey,
                    "walletAddressShort" to shortenAddress(cleanPublicKey),
                    "xp" to xp,
                    "level" to level,
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "skrTier" to skrTier,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
            .addOnSuccessListener {
                loadOrCreateProfile(session.uid, onState, "Wallet connected." )
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not save wallet connection: ${safeMessage(error)}")
            }
    }

    fun saveDailySignedProof(
        walletAddress: String,
        message: String,
        signature: String,
        onState: (RushUiState) -> Unit,
    ) {
        val cleanWallet = walletAddress.trim()
        val cleanSignature = signature.trim()
        if (cleanWallet.isBlank() || cleanSignature.isBlank()) {
            onState(errorState("Wallet signed the proof, but the proof payload was incomplete."))
            return
        }

        saveQuestProof(
            questId = QuestIds.SIGN_DAILY_PROOF,
            questTitle = "Sign Daily Proof",
            proofType = "mwa_daily_message_signature",
            xpReward = SIGNED_PROOF_XP,
            walletAddress = cleanWallet,
            proofFields = mapOf(
                "network" to "off-chain",
                "signedMessage" to message,
                "signedMessageSignature" to cleanSignature,
                "lastSignedMessageSignature" to cleanSignature,
            ),
            loadingMessage = "Saving signed daily proof…",
            successMessage = "Daily proof message signed and saved.",
            userExtraFields = mapOf(
                "lastSignedMessage" to message,
                "lastSignedMessageSignature" to cleanSignature,
                "lastSignedProofDate" to todayKey(),
            ),
            onState = onState,
        )
    }

    fun saveDailyMemoProof(
        walletAddress: String,
        memoText: String,
        transactionSignature: String,
        explorerUrl: String,
        onState: (RushUiState) -> Unit,
    ) {
        val cleanSignature = transactionSignature.trim()
        if (cleanSignature.isBlank()) {
            onState(errorState("Wallet submitted the memo, but no transaction signature was returned."))
            return
        }

        saveQuestProof(
            questId = QuestIds.ON_CHAIN_PROOF,
            questTitle = "On-Chain Memo Proof",
            proofType = "solana_devnet_memo_transaction",
            xpReward = ON_CHAIN_PROOF_XP,
            walletAddress = walletAddress.takeIf { it.isNotBlank() },
            proofFields = mapOf(
                "network" to "devnet",
                "memoText" to memoText,
                "txSignature" to cleanSignature,
                "explorerUrl" to explorerUrl,
            ),
            loadingMessage = "Saving devnet memo proof…",
            successMessage = "Devnet memo proof saved. Transaction: ${shortenAddress(cleanSignature)}",
            userExtraFields = mapOf(
                "lastOnChainTxSignature" to cleanSignature,
                "lastOnChainExplorerUrl" to explorerUrl,
                "lastOnChainProofDate" to todayKey(),
            ),
            onState = onState,
        )
    }

    fun saveSkrBalanceSnapshot(
        snapshot: SkrBalanceSnapshot,
        onState: (RushUiState) -> Unit,
    ) {
        val cleanWallet = snapshot.walletAddress.trim()
        if (cleanWallet.isBlank()) {
            onState(errorState("SKR check returned an empty wallet address."))
            return
        }

        val session = currentFirebaseSession(onState) ?: return
        val db = FirebaseFirestore.getInstance(session.app)
        val today = todayKey()
        val userRef = db.collection(USERS).document(session.uid)
        val completedRef = userRef.collection(COMPLETED_QUESTS).document("${QuestIds.SKR_HOLDER}_$today")
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)

        db.runTransaction { transaction ->
            val completedSnapshot = transaction.get(completedRef)
            val userSnapshot = transaction.get(userRef)
            val firstScanToday = !completedSnapshot.exists()
            val rewardForThisWrite = if (firstScanToday) SKR_SCAN_XP.toLong() else 0L
            val savedQuestXp = completedSnapshot.getLong("xpEarned") ?: rewardForThisWrite

            val oldXp = userSnapshot.getLong("xp") ?: 0L
            val oldRushTickets = userSnapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()
            val oldSkrCasualTickets = userSnapshot.getLong("skrCasualRushTickets") ?: 0L
            val oldPerkGrantDate = userSnapshot.getString("skrPerkTicketGrantDate")
            val oldPerkTicketsGranted = (userSnapshot.getLong("skrPerkTicketsGrantedToday") ?: 0L).toInt()
            val perkTicketDelta = SkrPassportRules.dailyTicketGrantDelta(
                targetDailyBonus = snapshot.dailyCasualTicketBonus,
                grantDate = oldPerkGrantDate,
                grantedToday = oldPerkTicketsGranted,
                todayKey = today,
            )
            val totalPerkTicketsGrantedToday = if (oldPerkGrantDate == today) {
                oldPerkTicketsGranted + perkTicketDelta
            } else {
                perkTicketDelta
            }
            val questTickets = if (firstScanToday) RadiantGameRules.QUEST_TICKET_REWARD else 0
            val newRushTickets = oldRushTickets + questTickets
            val newSkrCasualTickets = oldSkrCasualTickets + perkTicketDelta
            val oldStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: oldStreak
            val lastQuestDate = userSnapshot.getString("lastQuestDate")
            val newXp = oldXp + rewardForThisWrite
            val newStreak = if (firstScanToday) nextStreak(lastQuestDate, today, oldStreak) else oldStreak
            val newLongestStreak = maxOf(longestStreak, newStreak)
            val newLevel = levelForXp(newXp)
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val walletStatus = "Wallet connected"

            transaction.set(
                completedRef,
                mapOf(
                    "questId" to QuestIds.SKR_HOLDER,
                    "questTitle" to "Scan SKR Passport v2",
                    "date" to today,
                    "proofType" to "solana_mainnet_skr_balance_snapshot",
                    "walletAddress" to cleanWallet,
                    "walletAddressShort" to shortenAddress(cleanWallet),
                    "xpEarned" to savedQuestXp,
                    "rushTicketsEarned" to questTickets,
                    "skrPerkTicketsGrantedThisScan" to perkTicketDelta,
                    "skrPerkTicketsGrantedToday" to totalPerkTicketsGrantedToday,
                    "skrCasualTicketsOnly" to true,
                    "network" to snapshot.network,
                    "skrMint" to snapshot.mint,
                    "skrBalanceRaw" to snapshot.balanceRawAmount,
                    "skrBalanceUi" to snapshot.balanceUiAmount,
                    "skrBalanceDisplay" to snapshot.balanceDisplay,
                    "skrDecimals" to snapshot.decimals,
                    "skrTokenAccountCount" to snapshot.tokenAccountCount,
                    "skrTier" to snapshot.tierLabel,
                    // Kept for backward compatibility with Phase 5 profile data.
                    // Phase 11C UI no longer presents this as the primary perk.
                    "skrXpMultiplier" to snapshot.xpMultiplierLabel,
                    "skrXpMultiplierValue" to snapshot.xpMultiplierValue,
                    "hasSkr" to snapshot.hasSkr,
                    "skrPassportVersion" to snapshot.passportVersion,
                    "skrDailyBonusTickets" to snapshot.dailyCasualTicketBonus,
                    "skrChestBonusXp" to snapshot.chestBonusXp,
                    "skrChestBonusTickets" to snapshot.chestBonusTickets,
                    "skrFrameLabel" to snapshot.frameLabel,
                    "skrAuraLabel" to snapshot.auraLabel,
                    "skrHolderCollectibleLabel" to snapshot.holderCollectibleLabel,
                    "skrEligibleBalanceUi" to snapshot.eligibleSkrUiAmount,
                    "skrEligibleBalanceDisplay" to snapshot.eligibleSkrDisplay,
                    "skrStakedBalanceUi" to snapshot.stakedSkrUiAmount,
                    "skrStakedBalanceDisplay" to snapshot.stakedSkrDisplay,
                    "skrUnstakingBalanceUi" to snapshot.unstakingSkrUiAmount,
                    "skrUnstakingBalanceDisplay" to snapshot.unstakingSkrDisplay,
                    "skrStakedStatus" to snapshot.stakedSkrStatus,
                    "skrStakedVerified" to snapshot.stakedSkrVerified,
                    "skrStakeBoostActive" to snapshot.stakeBoostActive,
                    "skrStakeBoostLabel" to snapshot.stakeBoostLabel,
                    "skrUnstakingReady" to snapshot.unstakingReady,
                    "skrUnstakeReadyAtClientMs" to snapshot.unstakeReadyAtClientMs,
                    "skrStakingProgramId" to snapshot.stakingProgramId,
                    "skrStakingAccountCount" to snapshot.stakingAccountCount,
                    "skrStakingRpcSlot" to snapshot.stakingRpcSlot,
                    "perkSource" to "client-observed-mainnet-liquid-plus-official-staking-read-only",
                    "rankedScoreMultiplier" to 1.0,
                    "rankedAttemptBonus" to 0,
                    "rpcSlot" to snapshot.rpcSlot,
                    "checkedAtClientMs" to snapshot.checkedAtClientMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to displayName,
                    "walletAddress" to cleanWallet,
                    "walletAddressShort" to shortenAddress(cleanWallet),
                    "walletStatus" to walletStatus,
                    "skrTier" to snapshot.tierLabel,
                    "skrBalanceRaw" to snapshot.balanceRawAmount,
                    "skrBalanceUi" to snapshot.balanceUiAmount,
                    "skrBalanceDisplay" to snapshot.balanceDisplay,
                    "skrMint" to snapshot.mint,
                    "skrNetwork" to snapshot.network,
                    "skrDecimals" to snapshot.decimals,
                    "skrTokenAccountCount" to snapshot.tokenAccountCount,
                    "skrXpMultiplier" to snapshot.xpMultiplierLabel,
                    "skrXpMultiplierValue" to snapshot.xpMultiplierValue,
                    "hasSkr" to snapshot.hasSkr,
                    "skrPassportVersion" to snapshot.passportVersion,
                    "skrDailyBonusTickets" to snapshot.dailyCasualTicketBonus,
                    "skrPerkTicketGrantDate" to today,
                    "skrPerkTicketsGrantedToday" to totalPerkTicketsGrantedToday,
                    "skrChestBonusXp" to snapshot.chestBonusXp,
                    "skrChestBonusTickets" to snapshot.chestBonusTickets,
                    "skrFrameLabel" to snapshot.frameLabel,
                    "skrAuraLabel" to snapshot.auraLabel,
                    "skrHolderCollectibleLabel" to snapshot.holderCollectibleLabel,
                    "skrEligibleBalanceUi" to snapshot.eligibleSkrUiAmount,
                    "skrEligibleBalanceDisplay" to snapshot.eligibleSkrDisplay,
                    "skrStakedBalanceUi" to snapshot.stakedSkrUiAmount,
                    "skrStakedBalanceDisplay" to snapshot.stakedSkrDisplay,
                    "skrUnstakingBalanceUi" to snapshot.unstakingSkrUiAmount,
                    "skrUnstakingBalanceDisplay" to snapshot.unstakingSkrDisplay,
                    "skrStakedStatus" to snapshot.stakedSkrStatus,
                    "skrStakedVerified" to snapshot.stakedSkrVerified,
                    "skrStakeBoostActive" to snapshot.stakeBoostActive,
                    "skrStakeBoostLabel" to snapshot.stakeBoostLabel,
                    "skrUnstakingReady" to snapshot.unstakingReady,
                    "skrUnstakeReadyAtClientMs" to snapshot.unstakeReadyAtClientMs,
                    "skrStakingProgramId" to snapshot.stakingProgramId,
                    "skrStakingAccountCount" to snapshot.stakingAccountCount,
                    "skrStakingRpcSlot" to snapshot.stakingRpcSlot,
                    "skrPerkSource" to "client-observed-mainnet-liquid-plus-official-staking-read-only",
                    "lastSkrCheckDate" to today,
                    "lastSkrCheckedAtClientMs" to snapshot.checkedAtClientMs,
                    "lastSkrRpcSlot" to snapshot.rpcSlot,
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongestStreak,
                    "lastQuestDate" to if (firstScanToday) today else lastQuestDate,
                    "rushTickets" to newRushTickets,
                    "skrCasualRushTickets" to newSkrCasualTickets,
                    "phase" to 11,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
                    "walletAddress" to cleanWallet,
                    "walletAddressShort" to shortenAddress(cleanWallet),
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongestStreak,
                    "skrTier" to snapshot.tierLabel,
                    "skrBalanceUi" to snapshot.balanceUiAmount,
                    "skrXpMultiplier" to snapshot.xpMultiplierLabel,
                    "hasSkr" to snapshot.hasSkr,
                    "skrPassportVersion" to snapshot.passportVersion,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            perkTicketDelta
        }
            .addOnSuccessListener { grantedPerkTickets ->
                val perkLine = if (snapshot.hasSkr) {
                    val grantText = if (grantedPerkTickets > 0) {
                        "+$grantedPerkTickets holder casual ticket${if (grantedPerkTickets == 1) "" else "s"} granted today"
                    } else {
                        "today's holder tickets already granted"
                    }
                    buildString {
                        append("${snapshot.tierLabel} • $grantText • chest +${snapshot.chestBonusXp} XP")
                        if (snapshot.chestBonusTickets > 0) append(" +${snapshot.chestBonusTickets} ticket${if (snapshot.chestBonusTickets == 1) "" else "s"}")
                        if (snapshot.stakeBoostActive) append(" • ${snapshot.stakeBoostLabel}")
                    }
                } else {
                    "No SKR perks yet"
                }
                loadOrCreateProfile(
                    session.uid,
                    onState,
                    "SKR Passport refreshed • ${snapshot.balanceDisplay} liquid • ${snapshot.stakedSkrDisplay} staked • $perkLine.",
                )
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Couldn't update SKR Passport. ${safeMessage(error)}")
            }
    }


    fun claimDailyRadiantChest(onState: (RushUiState) -> Unit) {
        val session = currentFirebaseSession(onState) ?: return
        val db = FirebaseFirestore.getInstance(session.app)
        val today = todayKey()
        val utcWeekKey = Phase11CompetitionRules.utcWeekKey()
        val userRef = db.collection(USERS).document(session.uid)
        val chestRef = userRef.collection(COMPLETED_QUESTS).document("${QuestIds.DAILY_RADIANT_CHEST}_$today")
        val dailyCheckInRef = userRef.collection(COMPLETED_QUESTS).document("${QuestIds.DAILY_CHECK_IN}_$today")
        val signedProofRef = userRef.collection(COMPLETED_QUESTS).document("${QuestIds.SIGN_DAILY_PROOF}_$today")
        val memoProofRef = userRef.collection(COMPLETED_QUESTS).document("${QuestIds.ON_CHAIN_PROOF}_$today")
        val skrProofRef = userRef.collection(COMPLETED_QUESTS).document("${QuestIds.SKR_HOLDER}_$today")
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)

        db.runTransaction { transaction ->
            val chestSnapshot = transaction.get(chestRef)
            if (chestSnapshot.exists()) {
                throw DuplicateQuestException()
            }

            val userSnapshot = transaction.get(userRef)
            val walletAddress = userSnapshot.getString("walletAddress")
            val walletConnected = !walletAddress.isNullOrBlank()
            val dailyCheckInSnapshot = transaction.get(dailyCheckInRef)
            val signedProofSnapshot = transaction.get(signedProofRef)
            val memoProofSnapshot = transaction.get(memoProofRef)
            val skrProofSnapshot = transaction.get(skrProofRef)
            val requiredProofsDone = walletConnected &&
                dailyCheckInSnapshot.exists() &&
                signedProofSnapshot.exists() &&
                memoProofSnapshot.exists() &&
                skrProofSnapshot.exists()

            if (!requiredProofsDone) {
                throw IllegalStateException("Complete every daily proof before opening the Radiant Chest.")
            }

            val oldXp = userSnapshot.getLong("xp") ?: 0L
            val oldChestXp = userSnapshot.getLong("totalChestXp") ?: 0L
            val weeklyActivityMatches = userSnapshot.getString("weeklyActivityKey") == utcWeekKey
            val weeklyChestsBefore = if (weeklyActivityMatches) {
                (userSnapshot.getLong("weeklyChestsOpened") ?: 0L).toInt()
            } else {
                0
            }
            val weeklyRunsBefore = if (weeklyActivityMatches) {
                (userSnapshot.getLong("weeklyRunsCompleted") ?: 0L).toInt()
            } else {
                0
            }
            val oldRushTickets = userSnapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()
            val oldSkrCasualTickets = userSnapshot.getLong("skrCasualRushTickets") ?: 0L
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val avatarId = PublicProfileRules.normalizeAvatarId(userSnapshot.getString("avatarId"))
            // Chest perks are sourced from TODAY'S completed SKR scan rather than a
            // potentially stale profile mirror from an earlier day.
            val skrTier = skrProofSnapshot.getString("skrTier")
                ?: userSnapshot.getString("skrTier")
                ?: "Explorer"
            val reward = RewardLoopRules.pickDailyChestReward(
                todayKey = today,
                userSeed = session.uid,
                currentStreak = currentStreak.toInt(),
                skrTierLabel = skrTier,
                skrBonusXp = (skrProofSnapshot.getLong("skrChestBonusXp") ?: 0L).toInt(),
                skrBonusTickets = (skrProofSnapshot.getLong("skrChestBonusTickets") ?: 0L).toInt(),
            )
            val newRushTickets = oldRushTickets + RadiantGameRules.CHEST_TICKET_REWARD
            val newSkrCasualTickets = oldSkrCasualTickets + reward.skrBonusTickets
            val newXp = oldXp + reward.totalXp.toLong()
            val newLevel = levelForXp(newXp)
            val newChestXp = oldChestXp + reward.totalXp.toLong()

            transaction.set(
                chestRef,
                mapOf(
                    "questId" to QuestIds.DAILY_RADIANT_CHEST,
                    "questTitle" to "Daily Radiant Chest",
                    "date" to today,
                    "proofType" to "daily_reward_loop_no_stake_no_loss",
                    "walletAddress" to walletAddress,
                    "walletAddressShort" to shortenAddress(walletAddress),
                    "xpEarned" to reward.totalXp,
                    "rewardRarity" to reward.rarity,
                    "rewardTitle" to reward.title,
                    "baseXp" to reward.baseXp,
                    "streakBonusXp" to reward.streakBonusXp,
                    "skrBonusXp" to reward.skrBonusXp,
                    "skrBonusTickets" to reward.skrBonusTickets,
                    "skrBonusTicketsCasualOnly" to true,
                    "skrPerkTier" to reward.skrPerkTier,
                    "skrPerkSource" to "today-mainnet-read-only-skr-passport-liquid-plus-staking",
                    "revealLine" to reward.revealLine,
                    "rushTicketsEarned" to RadiantGameRules.CHEST_TICKET_REWARD,
                    "skrCasualRushTicketsEarned" to reward.skrBonusTickets,
                    "noStake" to true,
                    "noLoss" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to walletAddress,
                    "walletAddressShort" to shortenAddress(walletAddress),
                    "walletStatus" to "Wallet connected",
                    "skrTier" to skrTier,
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "lastChestClaimDate" to today,
                    "lastChestRewardTitle" to reward.title,
                    "lastChestRewardRarity" to reward.rarity,
                    "lastChestRewardXp" to reward.totalXp,
                    "lastChestSkrBonusXp" to reward.skrBonusXp,
                    "lastChestSkrBonusTickets" to reward.skrBonusTickets,
                    "lastChestRewardTickets" to RadiantGameRules.CHEST_TICKET_REWARD,
                    "totalChestXp" to newChestXp,
                    "rushTickets" to newRushTickets,
                    "skrCasualRushTickets" to newSkrCasualTickets,
                    "weeklyActivityKey" to utcWeekKey,
                    "weeklyRunsCompleted" to weeklyRunsBefore,
                    "weeklyChestsOpened" to weeklyChestsBefore + 1,
                    "phase" to 11,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to walletAddress,
                    "walletAddressShort" to shortenAddress(walletAddress),
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "skrTier" to skrTier,
                    "lastChestRewardRarity" to reward.rarity,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
            .addOnSuccessListener {
                loadOrCreateProfile(session.uid, onState, "Daily Radiant Chest opened!")
            }
            .addOnFailureListener { error ->
                val message = if (error is DuplicateQuestException) {
                    "Daily Radiant Chest already opened for today. Come back tomorrow."
                } else {
                    "Could not open Daily Radiant Chest: ${safeMessage(error)}"
                }
                loadOrCreateProfile(session.uid, onState, message)
            }
    }

    fun completeRadiantRun(
        result: RadiantRunResult,
        onState: (RushUiState) -> Unit,
    ) {
        val session = currentFirebaseSession(onState) ?: return
        val safeResult = result.copy(
            score = result.score.coerceAtLeast(0),
            maxCombo = result.maxCombo.coerceAtLeast(0),
            radiantHits = result.radiantHits.coerceAtLeast(0),
            corruptedHits = result.corruptedHits.coerceAtLeast(0),
            perfectHits = result.perfectHits.coerceAtLeast(0),
        )
        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)
        val completedAtMs = System.currentTimeMillis()
        val utcDayKey = Phase11CompetitionRules.utcDayKey(completedAtMs)
        val utcWeekKey = Phase11CompetitionRules.utcWeekKey(completedAtMs)
        val weeklyRef = db.collection(RUN_WEEKLY)
            .document(utcWeekKey)
            .collection(RUN_ENTRIES)
            .document(session.uid)
        val allTimeRef = db.collection(RUN_ALL_TIME).document(session.uid)

        db.runTransaction { transaction ->
            // Firestore transactions require all reads before writes.
            val userSnapshot = transaction.get(userRef)
            val leaderboardSnapshot = transaction.get(leaderboardRef)

            // Phase 11B.1: public competition identity is the connected wallet, not
            // the Firebase anonymous installation UID. Prefer the canonical user
            // profile value, but recover an older leaderboard wallet if a legacy
            // profile somehow lost its mirrored address.
            val walletAddress = userSnapshot.getString("walletAddress")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: leaderboardSnapshot.getString("walletAddress")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            val walletConnected = !walletAddress.isNullOrBlank()
            val walletDailyRef = walletAddress?.let { address ->
                db.collection(RUN_WALLET_DAILY)
                    .document(utcDayKey)
                    .collection(RUN_WALLETS)
                    .document(address)
            }
            val walletDailySnapshot = walletDailyRef?.let { transaction.get(it) }
            val weeklySnapshot = transaction.get(weeklyRef)
            val allTimeSnapshot = transaction.get(allTimeRef)

            val oldTickets = userSnapshot.getLong("rushTickets")
                ?: RadiantGameRules.STARTER_TICKETS.toLong()
            val oldSkrCasualTickets = userSnapshot.getLong("skrCasualRushTickets") ?: 0L
            if (oldTickets + oldSkrCasualTickets < RadiantGameRules.RUN_TICKET_COST) {
                throw IllegalStateException("No Rush Tickets left. Complete daily quests, scan SKR Passport, or open tomorrow's chest to earn more.")
            }

            val legacyAttemptsUsed = (userSnapshot.getLong("rankedAttemptsUsedToday") ?: 0L).toInt()
            val rankedDecision = Phase11CompetitionRules.rankedAttemptDecision(
                walletConnected = walletConnected,
                savedDayKey = walletDailySnapshot?.getString("utcDayKey")
                    ?: userSnapshot.getString("rankedRunsDayKey"),
                savedAttemptsUsed = if (walletDailySnapshot?.exists() == true) {
                    (walletDailySnapshot.getLong("attemptsUsed") ?: 0L).toInt()
                } else {
                    legacyAttemptsUsed
                },
                completedAtEpochMillis = completedAtMs,
                rankedEntryTicketAvailable = oldTickets >= RadiantGameRules.RUN_TICKET_COST,
            )
            // Phase 11C.4: gameplay XP uses the same wallet/day scope as ranked
            // attempts. Otherwise reinstalling or using a second phone would reset
            // the 300 XP cap and My Stats would show device-local values. Existing
            // per-UID fields remain a migration/fallback mirror.
            val legacyGameplayXp = Phase11CompetitionRules.gameplayXpEarnedToday(
                savedDayKey = userSnapshot.getString("gameplayXpDayKey"),
                savedEarned = (userSnapshot.getLong("gameplayXpEarnedToday") ?: 0L).toInt(),
                currentUtcDayKey = utcDayKey,
            )
            val sharedGameplayXp = if (walletDailySnapshot?.exists() == true &&
                walletDailySnapshot.getString("utcDayKey") == utcDayKey
            ) {
                (walletDailySnapshot.getLong("gameplayXpEarnedToday") ?: 0L)
                    .toInt()
                    .coerceIn(0, Phase11CompetitionRules.DAILY_GAMEPLAY_XP_CAP)
            } else {
                0
            }
            val gameplayXpBefore = if (walletConnected) {
                maxOf(sharedGameplayXp, legacyGameplayXp)
            } else {
                legacyGameplayXp
            }
            val xpAward = Phase11CompetitionRules.cappedGameplayXp(
                score = safeResult.score,
                maxCombo = safeResult.maxCombo,
                perfectHits = safeResult.perfectHits,
                savedDayKey = utcDayKey,
                savedEarnedToday = gameplayXpBefore,
                completedAtEpochMillis = completedAtMs,
            )

            val dailyActivityMatches = userSnapshot.getString("dailyActivityKey") == utcDayKey
            val dailyRunsBefore = if (dailyActivityMatches) (userSnapshot.getLong("dailyRunsToday") ?: 0L).toInt() else 0
            val dailyPerfectBefore = if (dailyActivityMatches) (userSnapshot.getLong("dailyPerfectHitsToday") ?: 0L).toInt() else 0
            val dailyBestScoreBefore = if (dailyActivityMatches) (userSnapshot.getLong("dailyBestScoreToday") ?: 0L).toInt() else 0
            val dailyBestComboBefore = if (dailyActivityMatches) (userSnapshot.getLong("dailyBestComboToday") ?: 0L).toInt() else 0
            val weeklyActivityMatches = userSnapshot.getString("weeklyActivityKey") == utcWeekKey
            val weeklyRunsBefore = if (weeklyActivityMatches) (userSnapshot.getLong("weeklyRunsCompleted") ?: 0L).toInt() else 0
            val weeklyChestsBefore = if (weeklyActivityMatches) (userSnapshot.getLong("weeklyChestsOpened") ?: 0L).toInt() else 0

            val oldRuns = (userSnapshot.getLong("totalRuns") ?: 0L).toInt()
            val oldCounts = collectionCounts(userSnapshot)
            val reward = RadiantGameRules.pickRunReward(
                score = safeResult.score,
                maxCombo = safeResult.maxCombo,
                userSeed = session.uid,
                runSerial = oldRuns + 1,
                ownedCounts = oldCounts,
            )
            val newCounts = oldCounts.toMutableMap().apply {
                this[reward.collectible.id] = (this[reward.collectible.id] ?: 0) + 1
            }
            val newCollectionForFirestore = newCounts.mapValues { it.value.toLong() }
            val oldXp = userSnapshot.getLong("xp") ?: 0L
            val newXp = oldXp + xpAward.grantedXp.toLong()
            val newLevel = levelForXp(newXp)
            val oldShards = userSnapshot.getLong("radiantShards") ?: 0L
            val newShards = oldShards + reward.duplicateShards.toLong()
            val oldBestScore = (userSnapshot.getLong("bestRunScore") ?: 0L).toInt()
            val newBestScore = maxOf(oldBestScore, safeResult.score)
            val newTotalRuns = oldRuns + 1
            // SKR holder tickets are casual-only by construction. A ranked run
            // always consumes a standard Rush Ticket. Casual runs prefer an SKR
            // casual ticket so holder perks can never buy extra ranked attempts.
            val usedSkrCasualTicket = rankedDecision.mode == RunCompetitionMode.Casual &&
                oldSkrCasualTickets >= RadiantGameRules.RUN_TICKET_COST
            val newTickets = oldTickets - if (usedSkrCasualTicket) 0 else RadiantGameRules.RUN_TICKET_COST
            val newSkrCasualTickets = oldSkrCasualTickets -
                if (usedSkrCasualTicket) RadiantGameRules.RUN_TICKET_COST else 0
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val avatarId = PublicProfileRules.normalizeAvatarId(userSnapshot.getString("avatarId"))
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val skrTier = userSnapshot.getString("skrTier") ?: "Explorer"
            val collectionOwned = RadiantGameRules.ownedUniqueCount(newCounts)

            val runRecord = Phase11CompetitionRules.createRunScoreRecord(
                runId = "${session.uid}_$completedAtMs",
                ownerUid = session.uid,
                displayName = displayName,
                walletAddress = walletAddress,
                walletAddressShort = shortenAddress(walletAddress),
                score = safeResult.score,
                maxCombo = safeResult.maxCombo,
                perfectHits = safeResult.perfectHits,
                radiantHits = safeResult.radiantHits,
                corruptedHits = safeResult.corruptedHits,
                mode = rankedDecision.mode,
                completedAtEpochMillis = completedAtMs,
            )

            val currentWeekly = weeklySnapshot.takeIf { it.exists() }?.let { snapshot ->
                WeeklyRunStats(
                    utcWeekKey = snapshot.getString("utcWeekKey") ?: utcWeekKey,
                    bestScore = (snapshot.getLong("score") ?: 0L).toInt(),
                    bestCombo = (snapshot.getLong("bestCombo") ?: 0L).toInt(),
                    perfectHitsAtBestScore = (snapshot.getLong("perfectHits") ?: 0L).toInt(),
                    bestCompletedAtEpochMillis = snapshot.getLong("bestCompletedAtEpochMillis") ?: 0L,
                    rankedRunsPlayed = (snapshot.getLong("runsPlayed") ?: 0L).toInt(),
                )
            }
            val currentAllTime = allTimeSnapshot.takeIf { it.exists() }?.let { snapshot ->
                RunPersonalBest(
                    score = (snapshot.getLong("score") ?: 0L).toInt(),
                    bestCombo = (snapshot.getLong("bestCombo") ?: 0L).toInt(),
                    perfectHits = (snapshot.getLong("perfectHits") ?: 0L).toInt(),
                    completedAtEpochMillis = snapshot.getLong("bestCompletedAtEpochMillis") ?: 0L,
                    utcWeekKey = snapshot.getString("bestWeekKey") ?: utcWeekKey,
                )
            }
            val newWeekly = Phase11CompetitionRules.updateWeeklyStats(currentWeekly, runRecord)
            val newAllTime = Phase11CompetitionRules.updatePersonalBest(currentAllTime, runRecord)
            val allTimeRuns = (allTimeSnapshot.getLong("runsPlayed") ?: 0L).toInt() +
                if (rankedDecision.mode == RunCompetitionMode.Ranked) 1 else 0

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to walletAddress,
                    "walletAddressShort" to shortenAddress(walletAddress),
                    "walletStatus" to if (walletConnected) {
                        "Wallet connected"
                    } else {
                        userSnapshot.getString("walletStatus") ?: "Wallet not connected yet"
                    },
                    "rushTickets" to newTickets,
                    "skrCasualRushTickets" to newSkrCasualTickets,
                    "lastRunTicketType" to if (usedSkrCasualTicket) "SKR_CASUAL" else "STANDARD",
                    "xp" to newXp,
                    "level" to newLevel,
                    "bestRunScore" to newBestScore,
                    "totalRuns" to newTotalRuns,
                    "lastRunScore" to safeResult.score,
                    "lastRunMaxCombo" to safeResult.maxCombo,
                    "lastRunPerfectHits" to safeResult.perfectHits,
                    "lastRunRadiantHits" to safeResult.radiantHits,
                    "lastRunCorruptedHits" to safeResult.corruptedHits,
                    "lastRunCompetitionMode" to rankedDecision.mode.name,
                    "lastRunPerformanceXp" to xpAward.grantedXp,
                    "gameplayXpDayKey" to xpAward.utcDayKey,
                    "gameplayXpEarnedToday" to xpAward.earnedAfter,
                    "rankedRunsDayKey" to rankedDecision.utcDayKey,
                    "rankedAttemptsUsedToday" to rankedDecision.rankedAttemptsUsedAfter,
                    "runWeeklyKey" to (newWeekly?.utcWeekKey ?: utcWeekKey),
                    "runWeeklyBestScore" to (newWeekly?.bestScore ?: 0),
                    "runWeeklyBestCombo" to (newWeekly?.bestCombo ?: 0),
                    "runWeeklyPerfectHits" to (newWeekly?.perfectHitsAtBestScore ?: 0),
                    "runWeeklyRunsPlayed" to (newWeekly?.rankedRunsPlayed ?: 0),
                    "runAllTimeBestScore" to (newAllTime?.score ?: 0),
                    "runAllTimeBestCombo" to (newAllTime?.bestCombo ?: 0),
                    "runAllTimePerfectHits" to (newAllTime?.perfectHits ?: 0),
                    "lastRunCapsuleTier" to reward.capsuleTier,
                    "lastRunRewardId" to reward.collectible.id,
                    "lastRunRewardTitle" to reward.collectible.title,
                    "lastRunRewardRarity" to reward.collectible.rarity,
                    // Phase 11 progression XP is controlled by the daily gameplay cap.
                    "lastRunRewardXp" to xpAward.grantedXp,
                    "lastRunRewardShards" to reward.duplicateShards,
                    "dailyActivityKey" to utcDayKey,
                    "dailyRunsToday" to dailyRunsBefore + 1,
                    "dailyPerfectHitsToday" to dailyPerfectBefore + safeResult.perfectHits,
                    "dailyBestScoreToday" to maxOf(dailyBestScoreBefore, safeResult.score),
                    "dailyBestComboToday" to maxOf(dailyBestComboBefore, safeResult.maxCombo),
                    "weeklyActivityKey" to utcWeekKey,
                    "weeklyRunsCompleted" to weeklyRunsBefore + 1,
                    "weeklyChestsOpened" to weeklyChestsBefore,
                    "radiantShards" to newShards,
                    "radiantCollection" to newCollectionForFirestore,
                    "collectionOwned" to collectionOwned,
                    "phase" to 11,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to walletAddress,
                    "walletAddressShort" to shortenAddress(walletAddress),
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "skrTier" to skrTier,
                    "bestRunScore" to newBestScore,
                    "collectionOwned" to collectionOwned,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            // Phase 11B.1: ranked-attempt usage is shared by wallet + UTC day,
            // so using the same wallet on another phone/reinstall cannot create a
            // second set of ranked attempts. Existing Phase 11B per-UID counters
            // are migrated into this shared document the next time a run is saved.
            val walletXpNeedsSync = xpAward.earnedAfter > sharedGameplayXp
            if (walletDailyRef != null && (
                    rankedDecision.mode == RunCompetitionMode.Ranked ||
                        walletDailySnapshot?.exists() != true ||
                        walletXpNeedsSync
                    )
            ) {
                transaction.set(
                    walletDailyRef,
                    mapOf(
                        "walletAddress" to walletAddress,
                        "walletAddressShort" to shortenAddress(walletAddress),
                        "utcDayKey" to utcDayKey,
                        "attemptsUsed" to rankedDecision.rankedAttemptsUsedAfter,
                        "gameplayXpEarnedToday" to xpAward.earnedAfter,
                        "lastWriterUid" to session.uid,
                        "scoreAuthority" to "client-reported-prototype-not-payout-authority",
                        "payoutEligible" to false,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }

            if (rankedDecision.mode == RunCompetitionMode.Ranked && newWeekly != null && newAllTime != null) {
                transaction.set(
                    weeklyRef,
                    mapOf(
                        "ownerUid" to session.uid,
                        "displayName" to displayName,
                        "avatarId" to avatarId,
                        "walletAddress" to walletAddress,
                        "walletAddressShort" to shortenAddress(walletAddress),
                        "utcWeekKey" to newWeekly.utcWeekKey,
                        "score" to newWeekly.bestScore,
                        "bestCombo" to newWeekly.bestCombo,
                        "perfectHits" to newWeekly.perfectHitsAtBestScore,
                        "runsPlayed" to newWeekly.rankedRunsPlayed,
                        "bestCompletedAtEpochMillis" to newWeekly.bestCompletedAtEpochMillis,
                        "scoreAuthority" to "client-reported-prototype-not-payout-authority",
                        "payoutEligible" to false,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                transaction.set(
                    allTimeRef,
                    mapOf(
                        "ownerUid" to session.uid,
                        "displayName" to displayName,
                        "avatarId" to avatarId,
                        "walletAddress" to walletAddress,
                        "walletAddressShort" to shortenAddress(walletAddress),
                        "score" to newAllTime.score,
                        "bestCombo" to newAllTime.bestCombo,
                        "perfectHits" to newAllTime.perfectHits,
                        "runsPlayed" to allTimeRuns,
                        "bestCompletedAtEpochMillis" to newAllTime.completedAtEpochMillis,
                        "bestWeekKey" to newAllTime.utcWeekKey,
                        "scoreAuthority" to "client-reported-prototype-not-payout-authority",
                        "payoutEligible" to false,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }

            Triple(reward, rankedDecision.mode, xpAward)
        }
            .addOnSuccessListener { (reward, mode, xpAward) ->
                val duplicateText = if (reward.duplicate) {
                    " Duplicate converted to +${reward.duplicateShards} Radiant Shards."
                } else {
                    " New collectible discovered!"
                }
                val modeText = if (mode == RunCompetitionMode.Ranked) {
                    "Ranked run saved to Weekly + All-Time competition"
                } else {
                    "Casual run saved; ranked boards unchanged"
                }
                val capText = if (xpAward.wasCapped) " Daily gameplay XP cap reached." else ""
                loadOrCreateProfile(
                    session.uid,
                    onState,
                    "$modeText • ${reward.capsuleTier}: ${reward.collectible.rarity} ${reward.collectible.title}. +${xpAward.grantedXp} performance XP.$duplicateText$capText",
                )
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not save Radiant Rush: ${safeMessage(error)}")
            }
    }

    fun updatePublicProfile(
        displayName: String,
        avatarId: String,
        onState: (RushUiState) -> Unit,
    ) {
        val session = currentFirebaseSession(onState) ?: return
        val cleanName = PublicProfileRules.sanitizeDisplayName(displayName)
        val cleanAvatar = PublicProfileRules.normalizeAvatarId(avatarId)
        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)
        val currentWeek = Phase11CompetitionRules.utcWeekKey()
        val weeklyRef = db.collection(RUN_WEEKLY)
            .document(currentWeek)
            .collection(RUN_ENTRIES)
            .document(session.uid)
        val allTimeRef = db.collection(RUN_ALL_TIME).document(session.uid)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val weeklySnapshot = transaction.get(weeklyRef)
            val allTimeSnapshot = transaction.get(allTimeRef)
            val walletAddress = userSnapshot.getString("walletAddress")
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to cleanName,
                    "avatarId" to cleanAvatar,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to cleanName,
                    "avatarId" to cleanAvatar,
                    "walletAddress" to walletAddress,
                    "walletAddressShort" to shortenAddress(walletAddress),
                    "xp" to (userSnapshot.getLong("xp") ?: 0L),
                    "level" to (userSnapshot.getLong("level") ?: 1L),
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "skrTier" to (userSnapshot.getString("skrTier") ?: "Explorer"),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            if (weeklySnapshot.exists()) {
                transaction.set(
                    weeklyRef,
                    mapOf(
                        "displayName" to cleanName,
                        "avatarId" to cleanAvatar,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }
            if (allTimeSnapshot.exists()) {
                transaction.set(
                    allTimeRef,
                    mapOf(
                        "displayName" to cleanName,
                        "avatarId" to cleanAvatar,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }
        }
            .addOnSuccessListener {
                loadOrCreateProfile(session.uid, onState, "Public profile updated.")
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not update profile: ${safeMessage(error)}")
            }
    }

    fun clearWalletConnection(onState: (RushUiState) -> Unit) {
        val session = currentFirebaseSession(onState) ?: return
        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)

        db.runBatch { batch ->
            batch.set(
                userRef,
                mapOf(
                    "walletAddress" to null,
                    "walletAddressShort" to null,
                    "walletAccountLabel" to null,
                    "walletStatus" to "Wallet not connected yet",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            batch.set(
                leaderboardRef,
                mapOf(
                    "walletAddress" to null,
                    "walletAddressShort" to "No wallet",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
            .addOnSuccessListener {
                loadOrCreateProfile(session.uid, onState, "Wallet disconnected.")
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not clear wallet connection: ${safeMessage(error)}")
            }
    }

    private fun saveQuestProof(
        questId: String,
        questTitle: String,
        proofType: String,
        xpReward: Int,
        walletAddress: String?,
        proofFields: Map<String, Any?>,
        loadingMessage: String,
        successMessage: String,
        userExtraFields: Map<String, Any?> = emptyMap(),
        onState: (RushUiState) -> Unit,
    ) {
        val session = currentFirebaseSession(onState) ?: return
        // Do not emit a full default loading state here. Wallet flows switch apps,
        // then return to Compose; replacing the whole screen with preview/default
        // data during the Firebase write can make a successfully submitted proof
        // look unfinished and can re-enable the action button too early.

        val db = FirebaseFirestore.getInstance(session.app)
        val today = todayKey()
        val userRef = db.collection(USERS).document(session.uid)
        val completedRef = userRef.collection(COMPLETED_QUESTS).document("${questId}_$today")
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)

        db.runTransaction { transaction ->
            val completedSnapshot = transaction.get(completedRef)
            if (completedSnapshot.exists()) {
                throw DuplicateQuestException()
            }

            val userSnapshot = transaction.get(userRef)
            val oldXp = userSnapshot.getLong("xp") ?: 0L
            val oldRushTickets = userSnapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()
            val newRushTickets = oldRushTickets + RadiantGameRules.QUEST_TICKET_REWARD
            val oldStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: oldStreak
            val lastQuestDate = userSnapshot.getString("lastQuestDate")
            val newXp = oldXp + xpReward
            val newStreak = nextStreak(lastQuestDate, today, oldStreak)
            val newLongestStreak = maxOf(longestStreak, newStreak)
            val newLevel = levelForXp(newXp)
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val avatarId = PublicProfileRules.normalizeAvatarId(userSnapshot.getString("avatarId"))
            val savedWalletAddress = walletAddress ?: userSnapshot.getString("walletAddress")
            val walletStatus = if (!savedWalletAddress.isNullOrBlank()) "Wallet connected" else "Wallet not connected yet"
            val skrTier = userSnapshot.getString("skrTier") ?: "Explorer"

            val proofDocument = mutableMapOf<String, Any?>(
                "questId" to questId,
                "questTitle" to questTitle,
                "date" to today,
                "proofType" to proofType,
                "walletAddress" to savedWalletAddress,
                "walletAddressShort" to shortenAddress(savedWalletAddress),
                "xpEarned" to xpReward,
                "rushTicketsEarned" to RadiantGameRules.QUEST_TICKET_REWARD,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            proofDocument.putAll(proofFields)

            val userUpdate = mutableMapOf<String, Any?>(
                "displayName" to displayName,
                "avatarId" to avatarId,
                "walletAddress" to savedWalletAddress,
                "walletAddressShort" to shortenAddress(savedWalletAddress),
                "walletStatus" to walletStatus,
                "skrTier" to skrTier,
                "xp" to newXp,
                "level" to newLevel,
                "currentStreak" to newStreak,
                "longestStreak" to newLongestStreak,
                "lastQuestDate" to today,
                "rushTickets" to newRushTickets,
                "phase" to 10,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            userUpdate.putAll(userExtraFields)

            transaction.set(completedRef, proofDocument, SetOptions.merge())
            transaction.set(userRef, userUpdate, SetOptions.merge())
            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "walletAddress" to savedWalletAddress,
                    "walletAddressShort" to shortenAddress(savedWalletAddress),
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongestStreak,
                    "skrTier" to skrTier,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
            .addOnSuccessListener {
                loadOrCreateProfile(session.uid, onState, successMessage)
            }
            .addOnFailureListener { error ->
                val message = if (error is DuplicateQuestException) {
                    "$questTitle already completed for today."
                } else {
                    "Could not save $questTitle: ${safeMessage(error)}"
                }
                loadOrCreateProfile(session.uid, onState, message)
            }
    }

    private fun loadOrCreateProfile(
        uid: String,
        onState: (RushUiState) -> Unit,
        message: String? = "Profile ready.",
    ) {
        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Cloud progress is unavailable.",
                ),
            )
            return
        }

        val db = FirebaseFirestore.getInstance(app)
        val userRef = db.collection(USERS).document(uid)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    loadCompletedAndLeaderboard(db, uid, snapshot, onState, message)
                } else {
                    val baseProfile = mapOf(
                        "displayName" to "Radiant Rookie",
                        "avatarId" to PublicProfileRules.DEFAULT_AVATAR_ID,
                        "walletAddress" to null,
                        "walletAddressShort" to null,
                        "walletStatus" to "Wallet not connected yet",
                        "skrTier" to "Explorer",
                        "skrPassportVersion" to 2L,
                        "skrDailyBonusTickets" to 0L,
                        "skrPerkTicketsGrantedToday" to 0L,
                        "skrChestBonusXp" to 0L,
                        "skrChestBonusTickets" to 0L,
                        "skrFrameLabel" to "Explorer Frame",
                        "skrAuraLabel" to "No holder aura",
                        "skrHolderCollectibleLabel" to "No holder collectible",
                        "skrEligibleBalanceDisplay" to "Not checked",
                        "skrStakedBalanceDisplay" to "Not checked",
                        "skrUnstakingBalanceDisplay" to "0 SKR",
                        "skrStakedStatus" to SkrPassportRules.STAKED_STATUS_NOT_VERIFIED,
                        "skrStakedVerified" to false,
                        "skrStakeBoostActive" to false,
                        "skrStakeBoostLabel" to "Stake Boost inactive",
                        "skrUnstakingReady" to false,
                        "xp" to 0L,
                        "level" to 1L,
                        "currentStreak" to 0L,
                        "longestStreak" to 0L,
                        "totalChestXp" to 0L,
                        "rushTickets" to RadiantGameRules.STARTER_TICKETS.toLong(),
                        "skrCasualRushTickets" to 0L,
                        "bestRunScore" to 0L,
                        "totalRuns" to 0L,
                        "radiantShards" to 0L,
                        "radiantCollection" to emptyMap<String, Long>(),
                        "collectionOwned" to 0L,
                        "dailyActivityKey" to null,
                        "dailyRunsToday" to 0L,
                        "dailyPerfectHitsToday" to 0L,
                        "dailyBestScoreToday" to 0L,
                        "dailyBestComboToday" to 0L,
                        "weeklyActivityKey" to null,
                        "weeklyRunsCompleted" to 0L,
                        "weeklyChestsOpened" to 0L,
                        "phase" to 11,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )

                    userRef.set(baseProfile)
                        .addOnSuccessListener {
                            userRef.get()
                                .addOnSuccessListener { createdSnapshot ->
                                    loadCompletedAndLeaderboard(db, uid, createdSnapshot, onState, "Profile ready.")
                                }
                                .addOnFailureListener { error ->
                                    onState(errorState("Could not load your profile."))
                                }
                        }
                        .addOnFailureListener { error ->
                            onState(errorState("Could not create your profile."))
                        }
                }
            }
            .addOnFailureListener { error ->
                onState(errorState("Could not load your profile."))
            }
    }

    private fun loadCompletedAndLeaderboard(
        db: FirebaseFirestore,
        uid: String,
        userSnapshot: DocumentSnapshot,
        onState: (RushUiState) -> Unit,
        message: String?,
    ) {
        val today = todayKey()
        db.collection(USERS)
            .document(uid)
            .collection(COMPLETED_QUESTS)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { completedQuery ->
                val completedIds = completedQuery.documents
                    .mapNotNull { it.getString("questId") }
                    .toMutableSet()

                // Defensive fallback: use profile-level proof dates as a second
                // source of truth. If the app was backgrounded during the wallet
                // handoff or the subcollection query is delayed, the UI should
                // still show today's signed/memo proof as completed once the
                // profile fields were saved.
                if (userSnapshot.getString("lastSignedProofDate") == today) {
                    completedIds.add(QuestIds.SIGN_DAILY_PROOF)
                }
                if (userSnapshot.getString("lastOnChainProofDate") == today) {
                    completedIds.add(QuestIds.ON_CHAIN_PROOF)
                }
                if (userSnapshot.getString("lastSkrCheckDate") == today) {
                    completedIds.add(QuestIds.SKR_HOLDER)
                }

                val currentWalletAddress = userSnapshot.getString("walletAddress")?.trim()?.takeIf { it.isNotBlank() }
                val walletConnected = currentWalletAddress != null
                if (userSnapshot.getString("lastChestClaimDate") == today) {
                    completedIds.add(QuestIds.DAILY_RADIANT_CHEST)
                }

                db.collection(LEADERBOARD)
                    .orderBy("xp", Query.Direction.DESCENDING)
                    // Read more than the visible Top 20 so legacy duplicate anonymous
                    // UIDs cannot crowd unique wallets out of the ranking.
                    .limit(100)
                    .get()
                    .addOnSuccessListener { leaderboardQuery ->
                        val uniqueWalletRows = LeaderboardRules.collapseByWallet(
                            candidates = leaderboardQuery.documents.map { document ->
                                LeaderboardCandidate(
                                    sourceId = document.id,
                                    displayName = document.getString("displayName") ?: "Radiant Rookie",
                                    walletAddress = document.getString("walletAddress"),
                                    walletAddressShort = document.getString("walletAddressShort"),
                                    xp = (document.getLong("xp") ?: 0L).toInt(),
                                    streak = (document.getLong("currentStreak") ?: 0L).toInt(),
                                    tier = document.getString("skrTier") ?: "Explorer",
                                    avatarId = PublicProfileRules.normalizeAvatarId(document.getString("avatarId")),
                                    updatedAtMs = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                                )
                            },
                            limit = 20,
                        )
                        val currentDisplayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
                        val currentAvatarId = PublicProfileRules.normalizeAvatarId(userSnapshot.getString("avatarId"))
                        val leaderboard = uniqueWalletRows.mapIndexed { index, row ->
                            val isCurrentUser = currentWalletAddress != null &&
                                row.walletAddress?.trim() == currentWalletAddress
                            LeaderboardPreview(
                                rank = index + 1,
                                name = if (isCurrentUser) currentDisplayName else row.displayName,
                                xp = row.xp,
                                streak = row.streak,
                                tier = row.tier,
                                walletLabel = LeaderboardRules.walletLabel(row),
                                avatarId = if (isCurrentUser) {
                                    currentAvatarId
                                } else {
                                    PublicProfileRules.normalizeAvatarId(row.avatarId)
                                },
                                isCurrentUser = isCurrentUser,
                            )
                        }.ifEmpty {
                            if (walletConnected) listOf(profileToLeaderboardRow(userSnapshot)) else emptyList()
                        }

                        val user = profileToUser(userSnapshot)
                        val collection = RadiantGameRules.collectionPreview(collectionCounts(userSnapshot))
                        val radiantRun = RadiantRunPreview(
                            rushTickets = user.rushTickets,
                            skrCasualRushTickets = user.skrCasualRushTickets,
                            bestScore = user.bestRunScore,
                            totalRuns = user.totalRuns,
                            lastScore = user.lastRunScore,
                            lastMaxCombo = user.lastRunMaxCombo,
                            lastRewardTitle = user.lastRunRewardTitle,
                            lastRewardRarity = user.lastRunRewardRarity,
                            lastRewardXp = user.lastRunRewardXp,
                            lastRewardShards = user.lastRunRewardShards,
                            radiantShards = user.radiantShards,
                            collectionOwned = collection.count { it.discovered },
                            collectionTotal = collection.size,
                        )
                        val quests = PreviewContent.quests.map { quest ->
                            when {
                                completedIds.contains(quest.id) -> quest.copy(status = QuestStatus.Completed)
                                quest.id == QuestIds.WALLET_CONNECT && walletConnected -> quest.copy(status = QuestStatus.Completed)
                                quest.id == QuestIds.DAILY_CHECK_IN -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.WALLET_CONNECT -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.SIGN_DAILY_PROOF && walletConnected -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.ON_CHAIN_PROOF && walletConnected -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.SKR_HOLDER && walletConnected -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.SIGN_DAILY_PROOF -> quest.copy(status = QuestStatus.Blocked)
                                quest.id == QuestIds.ON_CHAIN_PROOF -> quest.copy(status = QuestStatus.Blocked)
                                quest.id == QuestIds.SKR_HOLDER -> quest.copy(status = QuestStatus.Blocked)
                                else -> quest.copy(status = QuestStatus.Locked)
                            }
                        }
                        val chestClaimedToday = completedIds.contains(QuestIds.DAILY_RADIANT_CHEST)
                        val questStatuses = quests.associate { it.id to it.status }
                        val chestReady = RewardLoopRules.canClaimDailyChest(
                            questStatuses = questStatuses,
                            alreadyClaimedToday = chestClaimedToday,
                        )
                        val radiantChest = radiantChestState(
                            user = user,
                            completedQuestCount = quests.count { it.status == QuestStatus.Completed },
                            totalQuestCount = quests.size,
                            chestReady = chestReady,
                            chestClaimedToday = chestClaimedToday,
                        )
                        val badges = badgeState(user, completedIds)

                        loadRunCompetition(
                            db = db,
                            userSnapshot = userSnapshot,
                            walletConnected = walletConnected,
                        ) { runCompetition, competitionWarning ->
                            val retention = retentionState(
                                user = user,
                                radiantRun = radiantRun,
                                competition = runCompetition,
                            )
                            val mergedMessage = listOfNotNull(message, competitionWarning)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                                .takeIf { it.isNotBlank() }
                            onState(
                                RushUiState(
                                    firebaseStatus = FirebaseStatus.Ready,
                                    user = user,
                                    quests = quests,
                                    radiantChest = radiantChest,
                                    radiantRun = radiantRun,
                                    collection = collection,
                                    badges = badges,
                                    leaderboard = leaderboard,
                                    runCompetition = runCompetition,
                                    retention = retention,
                                    todayKey = today,
                                    lastMessage = mergedMessage,
                                ),
                            )
                        }
                    }
                    .addOnFailureListener { error ->
                        onState(errorState("Could not read leaderboard: ${safeMessage(error)}"))
                    }
            }
            .addOnFailureListener { error ->
                onState(errorState("Could not read completed quests: ${safeMessage(error)}"))
            }
    }

    private fun loadRunCompetition(
        db: FirebaseFirestore,
        userSnapshot: DocumentSnapshot,
        walletConnected: Boolean,
        onLoaded: (RunCompetitionPreview, String?) -> Unit,
    ) {
        val now = System.currentTimeMillis()
        val currentDay = Phase11CompetitionRules.utcDayKey(now)
        val currentWeek = Phase11CompetitionRules.utcWeekKey(now)
        val previousWeek = WeeklyRadiantCupRules.previousWeekKey(now)
        val seasonEndsAt = WeeklyRadiantCupRules.seasonEndsAtEpochMillis(now)
        val walletAddress = userSnapshot.getString("walletAddress")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val currentDisplayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
        val currentAvatarId = PublicProfileRules.normalizeAvatarId(userSnapshot.getString("avatarId"))
        val legacyAttemptsUsed = Phase11CompetitionRules.rankedAttemptsUsedToday(
            savedDayKey = userSnapshot.getString("rankedRunsDayKey"),
            savedAttemptsUsed = (userSnapshot.getLong("rankedAttemptsUsedToday") ?: 0L).toInt(),
            currentUtcDayKey = currentDay,
        )
        val legacyGameplayXp = Phase11CompetitionRules.gameplayXpEarnedToday(
            savedDayKey = userSnapshot.getString("gameplayXpDayKey"),
            savedEarned = (userSnapshot.getLong("gameplayXpEarnedToday") ?: 0L).toInt(),
            currentUtcDayKey = currentDay,
        )
        val profileWeek = userSnapshot.getString("runWeeklyKey")
        val fallbackWeeklyScore = if (profileWeek == currentWeek) {
            (userSnapshot.getLong("runWeeklyBestScore") ?: 0L).toInt()
        } else 0
        val fallbackWeeklyRuns = if (profileWeek == currentWeek) {
            (userSnapshot.getLong("runWeeklyRunsPlayed") ?: 0L).toInt()
        } else 0
        val fallbackPersonal = WalletRunPersonalStats(
            weeklyBestScore = fallbackWeeklyScore,
            allTimeBestScore = (userSnapshot.getLong("runAllTimeBestScore") ?: 0L).toInt(),
            bestCombo = (userSnapshot.getLong("runAllTimeBestCombo") ?: 0L).toInt(),
            perfectHitsAtBest = (userSnapshot.getLong("runAllTimePerfectHits") ?: 0L).toInt(),
            weeklyRankedRuns = fallbackWeeklyRuns,
        )
        val fallbackReward = WeeklyRadiantCupRules.rewardForPlacement(null)
        val fallbackCup = WeeklyCupPreview(
            seasonKey = currentWeek,
            seasonEndsAtEpochMillis = seasonEndsAt,
            personalBestScore = fallbackWeeklyScore,
            projectedRewardTitle = fallbackReward.title,
            projectedRewardDetail = fallbackReward.detail,
            previousSeasonKey = previousWeek,
        )

        fun build(
            attemptsUsed: Int,
            gameplayXpEarned: Int,
            personal: WalletRunPersonalStats = fallbackPersonal,
            weeklyRows: List<RunLeaderboardPreview> = emptyList(),
            allTimeRows: List<RunLeaderboardPreview> = emptyList(),
            weeklyCup: WeeklyCupPreview = fallbackCup.copy(personalBestScore = personal.weeklyBestScore),
        ) = RunCompetitionPreview(
            weeklyLeaderboard = weeklyRows,
            allTimeLeaderboard = allTimeRows,
            weeklyCup = weeklyCup,
            weekKey = currentWeek,
            rankedAttemptsUsedToday = attemptsUsed,
            rankedAttemptsRemaining = if (walletConnected) {
                (Phase11CompetitionRules.DAILY_RANKED_ATTEMPTS - attemptsUsed).coerceAtLeast(0)
            } else 0,
            dailyGameplayXpEarned = gameplayXpEarned,
            dailyGameplayXpCap = Phase11CompetitionRules.DAILY_GAMEPLAY_XP_CAP,
            personalWeeklyBestScore = personal.weeklyBestScore,
            personalAllTimeBestScore = personal.allTimeBestScore,
            personalBestCombo = personal.bestCombo,
            personalPerfectHits = personal.perfectHitsAtBest,
            personalWeeklyRuns = personal.weeklyRankedRuns,
            lastRunMode = userSnapshot.getString("lastRunCompetitionMode"),
            lastRunPerformanceXp = (userSnapshot.getLong("lastRunPerformanceXp") ?: 0L).toInt(),
        )

        fun mergeWarnings(vararg warnings: String?): String? =
            warnings.filterNotNull()
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .takeIf { it.isNotBlank() }

        fun loadPersonalWalletStats(
            onDone: (WalletRunPersonalStats, String?) -> Unit,
        ) {
            if (!walletConnected || walletAddress == null) {
                onDone(fallbackPersonal, null)
                return
            }

            db.collection(RUN_WEEKLY)
                .document(currentWeek)
                .collection(RUN_ENTRIES)
                .whereEqualTo("walletAddress", walletAddress)
                .get()
                .addOnSuccessListener { weeklyPersonalQuery ->
                    val weeklyCandidates = weeklyPersonalQuery.documents.map(::runCandidateFromDocument)
                    db.collection(RUN_ALL_TIME)
                        .whereEqualTo("walletAddress", walletAddress)
                        .get()
                        .addOnSuccessListener { allTimePersonalQuery ->
                            val walletStats = Phase11CompetitionRules.walletPersonalStats(
                                walletAddress = walletAddress,
                                weeklyCandidates = weeklyCandidates,
                                allTimeCandidates = allTimePersonalQuery.documents.map(::runCandidateFromDocument),
                            )
                            // Prefer wallet-scoped rows when present, but retain the
                            // per-UID mirror as migration fallback for older installs.
                            val allTimeSource = if (
                                walletStats.allTimeBestScore >= fallbackPersonal.allTimeBestScore
                            ) {
                                walletStats
                            } else {
                                fallbackPersonal
                            }
                            onDone(
                                WalletRunPersonalStats(
                                    weeklyBestScore = maxOf(walletStats.weeklyBestScore, fallbackPersonal.weeklyBestScore),
                                    allTimeBestScore = allTimeSource.allTimeBestScore,
                                    bestCombo = allTimeSource.bestCombo,
                                    perfectHitsAtBest = allTimeSource.perfectHitsAtBest,
                                    weeklyRankedRuns = maxOf(walletStats.weeklyRankedRuns, fallbackPersonal.weeklyRankedRuns),
                                ),
                                null,
                            )
                        }
                        .addOnFailureListener {
                            onDone(fallbackPersonal, "Personal Run stats are temporarily unavailable.")
                        }
                }
                .addOnFailureListener {
                    onDone(fallbackPersonal, "Personal Run stats are temporarily unavailable.")
                }
        }

        fun loadWeeklyCup(
            weeklyCandidates: List<RunLeaderboardCandidate>,
            personal: WalletRunPersonalStats,
            onDone: (WeeklyCupPreview) -> Unit,
        ) {
            val participantCount = Phase11CompetitionRules
                .collapseRunLeaderboardByWallet(weeklyCandidates, limit = 100)
                .size
            val personalRank = if (walletConnected) {
                Phase11CompetitionRules.walletRank(walletAddress, weeklyCandidates, limit = 100)
            } else {
                null
            }
            val projectedReward = WeeklyRadiantCupRules.rewardForPlacement(personalRank)

            fun loadPreviousSeason(
                sponsorName: String?,
                prizeLabel: String?,
                sponsorStatus: String,
                sponsorNote: String?,
                sponsorActive: Boolean,
            ) {
                if (!walletConnected || walletAddress == null) {
                    onDone(
                        WeeklyCupPreview(
                            seasonKey = currentWeek,
                            seasonEndsAtEpochMillis = seasonEndsAt,
                            participantCount = participantCount,
                            personalRank = personalRank,
                            personalBestScore = personal.weeklyBestScore,
                            projectedRewardTitle = projectedReward.title,
                            projectedRewardDetail = projectedReward.detail,
                            previousSeasonKey = previousWeek,
                            sponsorName = sponsorName,
                            sponsoredPrizeLabel = prizeLabel,
                            sponsoredPrizeStatus = sponsorStatus,
                            sponsorNote = sponsorNote,
                            sponsoredPrizeActive = sponsorActive,
                            payoutEnabled = false,
                        ),
                    )
                    return
                }

                db.collection(RUN_WEEKLY)
                    .document(previousWeek)
                    .collection(RUN_ENTRIES)
                    .orderBy("score", Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .addOnSuccessListener { previousQuery ->
                        val previousCandidates = previousQuery.documents.map(::runCandidateFromDocument)
                        val previousRank = Phase11CompetitionRules.walletRank(
                            walletAddress = walletAddress,
                            candidates = previousCandidates,
                            limit = 100,
                        )
                        val previousReward = previousRank?.let(WeeklyRadiantCupRules::rewardForPlacement)
                        onDone(
                            WeeklyCupPreview(
                                seasonKey = currentWeek,
                                seasonEndsAtEpochMillis = seasonEndsAt,
                                participantCount = participantCount,
                                personalRank = personalRank,
                                personalBestScore = personal.weeklyBestScore,
                                projectedRewardTitle = projectedReward.title,
                                projectedRewardDetail = projectedReward.detail,
                                previousSeasonKey = previousWeek,
                                previousSeasonRank = previousRank,
                                previousRewardTitle = previousReward?.title,
                                sponsorName = sponsorName,
                                sponsoredPrizeLabel = prizeLabel,
                                sponsoredPrizeStatus = sponsorStatus,
                                sponsorNote = sponsorNote,
                                sponsoredPrizeActive = sponsorActive,
                                payoutEnabled = false,
                            ),
                        )
                    }
                    .addOnFailureListener {
                        onDone(
                            WeeklyCupPreview(
                                seasonKey = currentWeek,
                                seasonEndsAtEpochMillis = seasonEndsAt,
                                participantCount = participantCount,
                                personalRank = personalRank,
                                personalBestScore = personal.weeklyBestScore,
                                projectedRewardTitle = projectedReward.title,
                                projectedRewardDetail = projectedReward.detail,
                                previousSeasonKey = previousWeek,
                                sponsorName = sponsorName,
                                sponsoredPrizeLabel = prizeLabel,
                                sponsoredPrizeStatus = sponsorStatus,
                                sponsorNote = sponsorNote,
                                sponsoredPrizeActive = sponsorActive,
                                payoutEnabled = false,
                            ),
                        )
                    }
            }

            db.collection(WEEKLY_CUP_CONFIGS)
                .document(currentWeek)
                .get()
                .addOnSuccessListener { sponsorDocument ->
                    val sponsorState = WeeklyRadiantCupRules.sponsorState(
                        status = sponsorDocument.getString("status"),
                        sponsorName = sponsorDocument.getString("sponsorName"),
                        prizeLabel = sponsorDocument.getString("prizeLabel"),
                        note = sponsorDocument.getString("note"),
                    )
                    loadPreviousSeason(
                        sponsorName = sponsorState.sponsorName,
                        prizeLabel = sponsorState.prizeLabel,
                        sponsorStatus = sponsorState.statusLabel,
                        sponsorNote = sponsorState.note,
                        sponsorActive = sponsorState.active,
                    )
                }
                .addOnFailureListener {
                    loadPreviousSeason(
                        sponsorName = null,
                        prizeLabel = null,
                        sponsorStatus = "No sponsored prize this week",
                        sponsorNote = null,
                        sponsorActive = false,
                    )
                }
        }

        fun loadBoards(
            attemptsUsed: Int,
            gameplayXpEarned: Int,
            attemptWarning: String? = null,
        ) {
            db.collection(RUN_WEEKLY)
                .document(currentWeek)
                .collection(RUN_ENTRIES)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener { weeklyQuery ->
                    val weeklyCandidates = weeklyQuery.documents.map(::runCandidateFromDocument)
                    val weeklyRows = runRows(weeklyCandidates, walletAddress, currentDisplayName, currentAvatarId)

                    fun finish(
                        allTimeRows: List<RunLeaderboardPreview>,
                        boardWarning: String? = null,
                    ) {
                        loadPersonalWalletStats { personal, personalWarning ->
                            loadWeeklyCup(
                                weeklyCandidates = weeklyCandidates,
                                personal = personal,
                            ) { weeklyCup ->
                                onLoaded(
                                    build(
                                        attemptsUsed = attemptsUsed,
                                        gameplayXpEarned = gameplayXpEarned,
                                        personal = personal,
                                        weeklyRows = weeklyRows,
                                        allTimeRows = allTimeRows,
                                        weeklyCup = weeklyCup,
                                    ),
                                    mergeWarnings(attemptWarning, boardWarning, personalWarning),
                                )
                            }
                        }
                    }

                    db.collection(RUN_ALL_TIME)
                        .orderBy("score", Query.Direction.DESCENDING)
                        .limit(100)
                        .get()
                        .addOnSuccessListener { allTimeQuery ->
                            finish(runRows(allTimeQuery.documents.map(::runCandidateFromDocument), walletAddress, currentDisplayName, currentAvatarId))
                        }
                        .addOnFailureListener {
                            finish(
                                allTimeRows = emptyList(),
                                boardWarning = "All-Time Run ranks are temporarily unavailable.",
                            )
                        }
                }
                .addOnFailureListener {
                    loadPersonalWalletStats { personal, personalWarning ->
                        onLoaded(
                            build(
                                attemptsUsed = attemptsUsed,
                                gameplayXpEarned = gameplayXpEarned,
                                personal = personal,
                            ),
                            mergeWarnings(
                                attemptWarning,
                                personalWarning,
                                "Radiant Rush ranks are temporarily unavailable.",
                            ),
                        )
                    }
                }
        }

        // Wallet/day state is shared across installs so My Stats does not reset
        // when the same Solana wallet is used on another phone or after a reinstall.
        if (walletConnected && walletAddress != null) {
            db.collection(RUN_WALLET_DAILY)
                .document(currentDay)
                .collection(RUN_WALLETS)
                .document(walletAddress)
                .get()
                .addOnSuccessListener { walletDaily ->
                    val attemptsUsed = if (walletDaily.exists()) {
                        (walletDaily.getLong("attemptsUsed") ?: 0L)
                            .toInt()
                            .coerceIn(0, Phase11CompetitionRules.DAILY_RANKED_ATTEMPTS)
                    } else {
                        legacyAttemptsUsed
                    }
                    val sharedGameplayXp = if (walletDaily.exists()) {
                        (walletDaily.getLong("gameplayXpEarnedToday") ?: 0L)
                            .toInt()
                            .coerceIn(0, Phase11CompetitionRules.DAILY_GAMEPLAY_XP_CAP)
                    } else {
                        0
                    }
                    loadBoards(
                        attemptsUsed = attemptsUsed,
                        gameplayXpEarned = maxOf(sharedGameplayXp, legacyGameplayXp),
                    )
                }
                .addOnFailureListener { error ->
                    loadBoards(
                        attemptsUsed = legacyAttemptsUsed,
                        gameplayXpEarned = legacyGameplayXp,
                        attemptWarning = "Ranked-run status could not be refreshed: ${safeMessage(error)}",
                    )
                }
        } else {
            loadBoards(
                attemptsUsed = 0,
                gameplayXpEarned = legacyGameplayXp,
            )
        }
    }

    private fun runCandidateFromDocument(document: DocumentSnapshot): RunLeaderboardCandidate =
        RunLeaderboardCandidate(
            sourceId = document.id,
            displayName = document.getString("displayName") ?: "Radiant Rookie",
            walletAddress = document.getString("walletAddress"),
            walletAddressShort = document.getString("walletAddressShort"),
            score = (document.getLong("score") ?: 0L).toInt(),
            bestCombo = (document.getLong("bestCombo") ?: 0L).toInt(),
            perfectHits = (document.getLong("perfectHits") ?: 0L).toInt(),
            runsPlayed = (document.getLong("runsPlayed") ?: 0L).toInt(),
            avatarId = PublicProfileRules.normalizeAvatarId(document.getString("avatarId")),
            bestCompletedAtEpochMillis = document.getLong("bestCompletedAtEpochMillis") ?: 0L,
            updatedAtMs = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
        )

    private fun runRows(
        candidates: List<RunLeaderboardCandidate>,
        currentWalletAddress: String?,
        currentDisplayName: String,
        currentAvatarId: String,
    ): List<RunLeaderboardPreview> =
        Phase11CompetitionRules.collapseRunLeaderboardByWallet(candidates, limit = 20)
            .mapIndexed { index, row ->
                val isCurrentUser = currentWalletAddress != null &&
                    row.walletAddress?.trim() == currentWalletAddress
                RunLeaderboardPreview(
                    rank = index + 1,
                    name = if (isCurrentUser) currentDisplayName else row.displayName,
                    walletLabel = Phase11CompetitionRules.walletLabel(row),
                    score = row.score,
                    bestCombo = row.bestCombo,
                    perfectHits = row.perfectHits,
                    runsPlayed = row.runsPlayed,
                    avatarId = if (isCurrentUser) {
                        currentAvatarId
                    } else {
                        PublicProfileRules.normalizeAvatarId(row.avatarId)
                    },
                    isCurrentUser = isCurrentUser,
                )
            }

    private fun currentFirebaseSession(onState: (RushUiState) -> Unit): FirebaseSession? {
        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Cloud progress is unavailable.",
                ),
            )
            return null
        }

        val uid = FirebaseAuth.getInstance(app).currentUser?.uid
        if (uid.isNullOrBlank()) {
            bootstrap(onState)
            return null
        }

        return FirebaseSession(app, uid)
    }

    private fun ensureFirebaseApp(): FirebaseApp? = try {
        FirebaseApp.getInstance()
    } catch (_: IllegalStateException) {
        try {
            FirebaseApp.initializeApp(appContext)
        } catch (_: IllegalStateException) {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun collectionCounts(snapshot: DocumentSnapshot): Map<String, Int> {
        val raw = snapshot.get("radiantCollection") as? Map<*, *> ?: return emptyMap()
        return raw.entries.mapNotNull { (key, value) ->
            val id = key as? String ?: return@mapNotNull null
            val count = when (value) {
                is Number -> value.toInt()
                else -> 0
            }
            id to count.coerceAtLeast(0)
        }.toMap()
    }

    private fun profileToUser(snapshot: DocumentSnapshot): UserPreview {
        val uidShort = snapshot.id.take(6).uppercase(Locale.US)
        val walletAddress = snapshot.getString("walletAddress")
        val walletStatus = if (!walletAddress.isNullOrBlank()) {
            "Wallet connected"
        } else {
            snapshot.getString("walletStatus") ?: "Wallet not connected yet"
        }

        return UserPreview(
            displayName = snapshot.getString("displayName") ?: "Radiant Rookie",
            avatarId = PublicProfileRules.normalizeAvatarId(snapshot.getString("avatarId")),
            walletStatus = walletStatus,
            walletAddress = walletAddress ?: "Connect your Solana wallet",
            skrTier = snapshot.getString("skrTier") ?: "Explorer",
            xp = (snapshot.getLong("xp") ?: 0L).toInt(),
            level = (snapshot.getLong("level") ?: 1L).toInt(),
            currentStreak = (snapshot.getLong("currentStreak") ?: 0L).toInt(),
            lastSignedMessageSignature = snapshot.getString("lastSignedMessageSignature"),
            lastOnChainTxSignature = snapshot.getString("lastOnChainTxSignature"),
            lastOnChainExplorerUrl = snapshot.getString("lastOnChainExplorerUrl"),
            skrBalance = snapshot.getString("skrBalanceDisplay") ?: "Not checked",
            skrMultiplier = snapshot.getString("skrXpMultiplier") ?: "1.00x",
            skrNetwork = snapshot.getString("skrNetwork") ?: "mainnet-beta",
            skrMint = snapshot.getString("skrMint") ?: "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
            lastSkrChecked = snapshot.getString("lastSkrCheckDate"),
            hasSkr = snapshot.getBoolean("hasSkr") ?: false,
            skrPassportVersion = (snapshot.getLong("skrPassportVersion") ?: 2L).toInt(),
            skrDailyBonusTickets = (snapshot.getLong("skrDailyBonusTickets") ?: 0L).toInt(),
            skrDailyBonusTicketsGrantedToday = if (snapshot.getString("skrPerkTicketGrantDate") == todayKey()) {
                (snapshot.getLong("skrPerkTicketsGrantedToday") ?: 0L).toInt()
            } else {
                0
            },
            skrChestBonusXp = (snapshot.getLong("skrChestBonusXp") ?: 0L).toInt(),
            skrChestBonusTickets = (snapshot.getLong("skrChestBonusTickets") ?: 0L).toInt(),
            skrFrameLabel = snapshot.getString("skrFrameLabel") ?: "Explorer Frame",
            skrAuraLabel = snapshot.getString("skrAuraLabel") ?: "No holder aura",
            skrHolderCollectibleLabel = snapshot.getString("skrHolderCollectibleLabel") ?: "No holder collectible",
            skrEligibleBalance = snapshot.getString("skrEligibleBalanceDisplay") ?: snapshot.getString("skrBalanceDisplay") ?: "Not checked",
            skrStakedBalance = snapshot.getString("skrStakedBalanceDisplay") ?: "Not checked",
            skrUnstakingBalance = snapshot.getString("skrUnstakingBalanceDisplay") ?: "0 SKR",
            skrStakedStatus = snapshot.getString("skrStakedStatus") ?: SkrPassportRules.STAKED_STATUS_NOT_VERIFIED,
            skrStakedVerified = snapshot.getBoolean("skrStakedVerified") ?: false,
            skrStakeBoostActive = snapshot.getBoolean("skrStakeBoostActive") ?: false,
            skrStakeBoostLabel = snapshot.getString("skrStakeBoostLabel") ?: "Stake Boost inactive",
            skrUnstakingReady = snapshot.getBoolean("skrUnstakingReady") ?: false,
            skrStakingProgramId = snapshot.getString("skrStakingProgramId") ?: "SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ",
            lastChestClaimDate = snapshot.getString("lastChestClaimDate"),
            lastChestRewardTitle = snapshot.getString("lastChestRewardTitle"),
            lastChestRewardRarity = snapshot.getString("lastChestRewardRarity"),
            lastChestRewardXp = (snapshot.getLong("lastChestRewardXp") ?: 0L).toInt(),
            lastChestRewardTickets = (snapshot.getLong("lastChestRewardTickets") ?: 0L).toInt(),
            lastChestSkrBonusXp = (snapshot.getLong("lastChestSkrBonusXp") ?: 0L).toInt(),
            lastChestSkrBonusTickets = (snapshot.getLong("lastChestSkrBonusTickets") ?: 0L).toInt(),
            totalChestXp = (snapshot.getLong("totalChestXp") ?: 0L).toInt(),
            rushTickets = (snapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()).toInt(),
            skrCasualRushTickets = (snapshot.getLong("skrCasualRushTickets") ?: 0L).toInt(),
            bestRunScore = (snapshot.getLong("bestRunScore") ?: 0L).toInt(),
            totalRuns = (snapshot.getLong("totalRuns") ?: 0L).toInt(),
            lastRunScore = (snapshot.getLong("lastRunScore") ?: 0L).toInt(),
            lastRunMaxCombo = (snapshot.getLong("lastRunMaxCombo") ?: 0L).toInt(),
            lastRunRewardTitle = snapshot.getString("lastRunRewardTitle"),
            lastRunRewardRarity = snapshot.getString("lastRunRewardRarity"),
            lastRunRewardXp = (snapshot.getLong("lastRunRewardXp") ?: 0L).toInt(),
            lastRunRewardShards = (snapshot.getLong("lastRunRewardShards") ?: 0L).toInt(),
            radiantShards = (snapshot.getLong("radiantShards") ?: 0L).toInt(),
            collectionOwned = (snapshot.getLong("collectionOwned") ?: 0L).toInt(),
            dailyActivityKey = snapshot.getString("dailyActivityKey"),
            dailyRunsToday = (snapshot.getLong("dailyRunsToday") ?: 0L).toInt(),
            dailyPerfectHitsToday = (snapshot.getLong("dailyPerfectHitsToday") ?: 0L).toInt(),
            dailyBestScoreToday = (snapshot.getLong("dailyBestScoreToday") ?: 0L).toInt(),
            dailyBestComboToday = (snapshot.getLong("dailyBestComboToday") ?: 0L).toInt(),
            weeklyActivityKey = snapshot.getString("weeklyActivityKey"),
            weeklyRunsCompleted = (snapshot.getLong("weeklyRunsCompleted") ?: 0L).toInt(),
            weeklyChestsOpened = (snapshot.getLong("weeklyChestsOpened") ?: 0L).toInt(),
        )
    }

    private fun profileToLeaderboardRow(snapshot: DocumentSnapshot): LeaderboardPreview = LeaderboardPreview(
        rank = 1,
        name = snapshot.getString("displayName") ?: "Radiant Rookie",
        xp = (snapshot.getLong("xp") ?: 0L).toInt(),
        streak = (snapshot.getLong("currentStreak") ?: 0L).toInt(),
        tier = snapshot.getString("skrTier") ?: "Explorer",
        walletLabel = shortenAddress(snapshot.getString("walletAddress")),
        avatarId = PublicProfileRules.normalizeAvatarId(snapshot.getString("avatarId")),
        isCurrentUser = true,
    )

    private fun retentionState(
        user: UserPreview,
        radiantRun: RadiantRunPreview,
        competition: RunCompetitionPreview,
    ): RetentionPreview {
        val utcDay = Phase11CompetitionRules.utcDayKey()
        val utcWeek = Phase11CompetitionRules.utcWeekKey()
        val dailyMatches = user.dailyActivityKey == utcDay
        val weeklyMatches = user.weeklyActivityKey == utcWeek
        val input = RetentionRules.RetentionInput(
            utcDayKey = utcDay,
            currentStreak = user.currentStreak,
            collectionOwned = radiantRun.collectionOwned,
            collectionTotal = radiantRun.collectionTotal,
            dailyRuns = if (dailyMatches) user.dailyRunsToday else 0,
            dailyPerfectHits = if (dailyMatches) user.dailyPerfectHitsToday else 0,
            dailyBestScore = if (dailyMatches) user.dailyBestScoreToday else 0,
            dailyBestCombo = if (dailyMatches) user.dailyBestComboToday else 0,
            dailyGameplayXp = competition.dailyGameplayXpEarned,
            weeklyRuns = if (weeklyMatches) user.weeklyRunsCompleted else 0,
            weeklyRankedRuns = competition.personalWeeklyRuns,
            weeklyChests = if (weeklyMatches) user.weeklyChestsOpened else 0,
        )
        val daily = RetentionRules.dailyGoals(input)
        val weekly = RetentionRules.weeklyGoals(input)
        val next = RetentionRules.nextAction(daily, weekly)
        val streakMilestone = RetentionRules.nextStreakMilestone(user.currentStreak)
        val collectionMilestone = RetentionRules.nextCollectionMilestone(
            collectionOwned = radiantRun.collectionOwned,
            collectionTotal = radiantRun.collectionTotal,
        )
        fun RetentionRules.ProgressGoal.toPreview() = RetentionGoalPreview(
            id = id,
            title = title,
            detail = detail,
            progress = progress,
            target = target,
        )
        return RetentionPreview(
            dailyGoals = daily.map { it.toPreview() },
            weeklyGoals = weekly.map { it.toPreview() },
            nextActionTitle = next?.title ?: "Daily goals complete",
            nextActionDetail = next?.detail ?: "Keep your streak alive and climb the Weekly Radiant Cup.",
            dailyCompleted = daily.count { it.completed },
            weeklyCompleted = weekly.count { it.completed },
            streakNextTarget = streakMilestone.target,
            streakRewardTitle = streakMilestone.title,
            collectionNextTarget = collectionMilestone.target,
            collectionRewardTitle = collectionMilestone.title,
        )
    }

    private fun badgeState(user: UserPreview, completedIds: Set<String>): List<BadgePreview> = listOf(
        BadgePreview("First Launch", "Start Radiant Circle.", unlocked = true),
        BadgePreview("Daily Ready", "Your daily progress is ready.", unlocked = true),
        BadgePreview("Daily Saver", "Finish today’s check-in.", unlocked = completedIds.contains(QuestIds.DAILY_CHECK_IN)),
        BadgePreview("Wallet Ready", "Connect your Solana wallet.", unlocked = user.walletStatus == "Wallet connected"),
        BadgePreview("Daily Proof", "Sign the daily proof message.", unlocked = completedIds.contains(QuestIds.SIGN_DAILY_PROOF)),
        BadgePreview("On-Chain Spark", "Submit your first memo proof.", unlocked = completedIds.contains(QuestIds.ON_CHAIN_PROOF)),
        BadgePreview("SKR Radiant", "Unlock SKR Passport perks.", unlocked = user.hasSkr),
        BadgePreview("Radiant Chest", "Open your Daily Radiant Chest.", unlocked = completedIds.contains(QuestIds.DAILY_RADIANT_CHEST)),
        BadgePreview("First Run", "Finish your first Radiant Rush.", unlocked = user.totalRuns > 0),
        BadgePreview("Combo Pilot", "Reach a 10-hit combo in Radiant Rush.", unlocked = user.lastRunMaxCombo >= 10),
        BadgePreview("Collector", "Discover three Radiant collectibles.", unlocked = user.collectionOwned >= 3),
        BadgePreview("3-Day Streak Spark", "Keep a three-day streak alive.", unlocked = user.currentStreak >= 3),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = user.currentStreak >= 7),
        BadgePreview("14-Day Streak Aurora", "Keep a fourteen-day streak alive.", unlocked = user.currentStreak >= 14),
        BadgePreview("Vault Complete", "Discover the full Radiant collection.", unlocked = user.collectionOwned >= RadiantGameRules.collectibles.size),
    )

    private fun radiantChestState(
        user: UserPreview,
        completedQuestCount: Int,
        totalQuestCount: Int,
        chestReady: Boolean,
        chestClaimedToday: Boolean,
    ): RadiantChestPreview {
        val progressText = "$completedQuestCount/$totalQuestCount daily proofs ready"
        return when {
            chestClaimedToday -> RadiantChestPreview(
                status = RadiantChestStatus.Claimed,
                subtitle = "Today’s reward is secured.",
                progressText = progressText,
                rewardText = "Daily reward secured",
                buttonLabel = "Come Back Tomorrow",
                lastRewardRarity = user.lastChestRewardRarity,
                lastRewardTitle = user.lastChestRewardTitle,
                lastRewardXp = user.lastChestRewardXp,
                lastRewardStandardTickets = user.lastChestRewardTickets,
                lastRewardSkrCasualTickets = user.lastChestSkrBonusTickets,
                lastRewardSkrBonusXp = user.lastChestSkrBonusXp,
            )
            chestReady -> RadiantChestPreview(
                status = RadiantChestStatus.Ready,
                subtitle = "Ready! Open your chest for today’s reward.",
                progressText = progressText,
                rewardText = if (user.hasSkr) {
                    "${user.skrTier} chest: +${user.skrChestBonusXp} XP" +
                        if (user.skrChestBonusTickets > 0) " • +${user.skrChestBonusTickets} SKR casual ticket${if (user.skrChestBonusTickets == 1) "" else "s"}" else ""
                } else {
                    "Standard chest ready"
                },
                buttonLabel = "Open Chest",
            )
            else -> RadiantChestPreview(
                status = RadiantChestStatus.Locked,
                subtitle = "Complete today’s quests to unlock your chest.",
                progressText = progressText,
                rewardText = "Finish all daily quests to unlock",
                buttonLabel = "Locked",
            )
        }
    }

    private fun nextStreak(lastDate: String?, today: String, currentStreak: Long): Long {
        if (lastDate == today) return currentStreak
        return try {
            val last = lastDate?.let { LocalDate.parse(it, dateFormatter) }
            val current = LocalDate.parse(today, dateFormatter)
            if (last != null && last.plusDays(1) == current) currentStreak + 1L else 1L
        } catch (_: Exception) {
            1L
        }
    }

    private fun levelForXp(xp: Long): Long = (xp / 500L) + 1L

    private fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).format(dateFormatter)

    private fun shortenAddress(address: String?): String {
        val clean = address?.trim().orEmpty()
        if (clean.length <= 12) return clean.ifBlank { "No wallet" }
        return "${clean.take(4)}…${clean.takeLast(4)}"
    }

    private fun loadingState(message: String): RushUiState = PreviewContent.defaultState().copy(
        firebaseStatus = FirebaseStatus.Loading,
        todayKey = todayKey(),
        lastMessage = message,
    )

    private fun errorState(message: String): RushUiState = PreviewContent.defaultState().copy(
        firebaseStatus = FirebaseStatus.Error,
        todayKey = todayKey(),
        lastMessage = message,
    )

    private fun safeMessage(error: Throwable): String = when (error) {
        is FirebaseFirestoreException -> "${error.code}: ${error.message ?: "Firestore error"}"
        else -> error.message ?: error::class.java.simpleName
    }

    private data class FirebaseSession(
        val app: FirebaseApp,
        val uid: String,
    )

    private class DuplicateQuestException : RuntimeException("Quest already completed today.")

    private companion object {
        const val USERS = "users"
        const val COMPLETED_QUESTS = "completedQuests"
        const val LEADERBOARD = "leaderboard"
        const val RUN_WEEKLY = "runWeekly"
        const val RUN_ENTRIES = "entries"
        const val RUN_ALL_TIME = "runAllTime"
        const val RUN_WALLET_DAILY = "runWalletDaily"
        const val RUN_WALLETS = "wallets"
        const val WEEKLY_CUP_CONFIGS = "weeklyCupConfigs"
        const val SIGNED_PROOF_XP = 75
        const val ON_CHAIN_PROOF_XP = 100
        const val SKR_SCAN_XP = 50
    }
}
