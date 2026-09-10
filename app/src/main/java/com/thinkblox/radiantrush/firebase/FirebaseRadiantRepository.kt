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
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.data.UserPreview
import com.thinkblox.radiantrush.logic.LeaderboardCandidate
import com.thinkblox.radiantrush.logic.LeaderboardRules
import com.thinkblox.radiantrush.logic.RadiantGameRules
import com.thinkblox.radiantrush.logic.RadiantRunResult
import com.thinkblox.radiantrush.logic.RewardLoopRules
import com.thinkblox.radiantrush.solana.SkrBalanceSnapshot
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Firebase Auth + Firestore repository for Radiant Rush.
 *
 * Phase 5 persists read-only SKR mainnet balance snapshots after the app
 * queries Solana RPC by public wallet address. Phase 9 adds a no-loss daily
 * Radiant Chest reward loop after all proof quests are completed. Phase 10
 * adds persistent Rush Tickets, the Radiant Run score loop, and collectible
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
                lastMessage = "Starting Firebase anonymous sign-in…",
            ),
        )

        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Firebase is not configured yet. Add app/google-services.json to enable cloud sync.",
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
            loadingMessage = "Saving daily Firebase check-in…",
            successMessage = "Daily Firebase check-in saved.",
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
        onState(loadingState("Saving wallet identity to Firebase…"))

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
            val xp = userSnapshot.getLong("xp") ?: 0L
            val level = userSnapshot.getLong("level") ?: levelForXp(xp)
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val skrTier = userSnapshot.getString("skrTier") ?: "Explorer"

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to displayName,
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
                loadOrCreateProfile(session.uid, onState, "Wallet connected and public address saved to Firebase.")
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
            val newRushTickets = oldRushTickets + if (firstScanToday) RadiantGameRules.QUEST_TICKET_REWARD else 0
            val oldStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: oldStreak
            val lastQuestDate = userSnapshot.getString("lastQuestDate")
            val newXp = oldXp + rewardForThisWrite
            val newStreak = if (firstScanToday) nextStreak(lastQuestDate, today, oldStreak) else oldStreak
            val newLongestStreak = maxOf(longestStreak, newStreak)
            val newLevel = levelForXp(newXp)
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val walletStatus = if (cleanWallet.isNotBlank()) "Wallet connected" else "Wallet not connected yet"

            transaction.set(
                completedRef,
                mapOf(
                    "questId" to QuestIds.SKR_HOLDER,
                    "questTitle" to "Scan SKR Passport",
                    "date" to today,
                    "proofType" to "solana_mainnet_skr_balance_snapshot",
                    "walletAddress" to cleanWallet,
                    "walletAddressShort" to shortenAddress(cleanWallet),
                    "xpEarned" to savedQuestXp,
                    "rushTicketsEarned" to if (firstScanToday) RadiantGameRules.QUEST_TICKET_REWARD else 0,
                    "network" to snapshot.network,
                    "skrMint" to snapshot.mint,
                    "skrBalanceRaw" to snapshot.balanceRawAmount,
                    "skrBalanceUi" to snapshot.balanceUiAmount,
                    "skrBalanceDisplay" to snapshot.balanceDisplay,
                    "skrDecimals" to snapshot.decimals,
                    "skrTokenAccountCount" to snapshot.tokenAccountCount,
                    "skrTier" to snapshot.tierLabel,
                    "skrXpMultiplier" to snapshot.xpMultiplierLabel,
                    "skrXpMultiplierValue" to snapshot.xpMultiplierValue,
                    "hasSkr" to snapshot.hasSkr,
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
                    "lastSkrCheckDate" to today,
                    "lastSkrCheckedAtClientMs" to snapshot.checkedAtClientMs,
                    "lastSkrRpcSlot" to snapshot.rpcSlot,
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongestStreak,
                    "lastQuestDate" to if (firstScanToday) today else lastQuestDate,
                    "rushTickets" to newRushTickets,
                    "phase" to 10,
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
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
            .addOnSuccessListener {
                val message = if (snapshot.hasSkr) {
                    "SKR Passport scanned: ${snapshot.balanceDisplay} • ${snapshot.tierLabel} • ${snapshot.xpMultiplierLabel} boost."
                } else {
                    "SKR Passport scanned: 0 SKR on mainnet. Explorer tier saved without faking a balance."
                }
                loadOrCreateProfile(session.uid, onState, message)
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not save SKR Passport scan: ${safeMessage(error)}")
            }
    }


    fun claimDailyRadiantChest(onState: (RushUiState) -> Unit) {
        val session = currentFirebaseSession(onState) ?: return
        val db = FirebaseFirestore.getInstance(session.app)
        val today = todayKey()
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
            val requiredProofsDone = walletConnected &&
                transaction.get(dailyCheckInRef).exists() &&
                transaction.get(signedProofRef).exists() &&
                transaction.get(memoProofRef).exists() &&
                transaction.get(skrProofRef).exists()

            if (!requiredProofsDone) {
                throw IllegalStateException("Complete every daily proof before opening the Radiant Chest.")
            }

            val oldXp = userSnapshot.getLong("xp") ?: 0L
            val oldChestXp = userSnapshot.getLong("totalChestXp") ?: 0L
            val oldRushTickets = userSnapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()
            val newRushTickets = oldRushTickets + RadiantGameRules.CHEST_TICKET_REWARD
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val skrTier = userSnapshot.getString("skrTier") ?: "Explorer"
            val hasSkr = userSnapshot.getBoolean("hasSkr") ?: false
            val reward = RewardLoopRules.pickDailyChestReward(
                todayKey = today,
                userSeed = session.uid,
                currentStreak = currentStreak.toInt(),
                hasSkr = hasSkr,
            )
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
                    "revealLine" to reward.revealLine,
                    "rushTicketsEarned" to RadiantGameRules.CHEST_TICKET_REWARD,
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
                    "totalChestXp" to newChestXp,
                    "rushTickets" to newRushTickets,
                    "phase" to 10,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
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
                loadOrCreateProfile(session.uid, onState, "Daily Radiant Chest opened. Bonus XP saved to Firebase.")
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
        )
        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val oldTickets = userSnapshot.getLong("rushTickets")
                ?: RadiantGameRules.STARTER_TICKETS.toLong()
            if (oldTickets < RadiantGameRules.RUN_TICKET_COST) {
                throw IllegalStateException("No Rush Tickets left. Complete daily quests or tomorrow's chest to earn more.")
            }

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
            val newXp = oldXp + reward.xpReward.toLong()
            val newLevel = levelForXp(newXp)
            val oldShards = userSnapshot.getLong("radiantShards") ?: 0L
            val newShards = oldShards + reward.duplicateShards.toLong()
            val oldBestScore = (userSnapshot.getLong("bestRunScore") ?: 0L).toInt()
            val newBestScore = maxOf(oldBestScore, safeResult.score)
            val newTotalRuns = oldRuns + 1
            val newTickets = oldTickets - RadiantGameRules.RUN_TICKET_COST
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val walletAddress = userSnapshot.getString("walletAddress")
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val skrTier = userSnapshot.getString("skrTier") ?: "Explorer"
            val collectionOwned = RadiantGameRules.ownedUniqueCount(newCounts)

            transaction.set(
                userRef,
                mapOf(
                    "displayName" to displayName,
                    "rushTickets" to newTickets,
                    "xp" to newXp,
                    "level" to newLevel,
                    "bestRunScore" to newBestScore,
                    "totalRuns" to newTotalRuns,
                    "lastRunScore" to safeResult.score,
                    "lastRunMaxCombo" to safeResult.maxCombo,
                    "lastRunRadiantHits" to safeResult.radiantHits,
                    "lastRunCorruptedHits" to safeResult.corruptedHits,
                    "lastRunCapsuleTier" to reward.capsuleTier,
                    "lastRunRewardId" to reward.collectible.id,
                    "lastRunRewardTitle" to reward.collectible.title,
                    "lastRunRewardRarity" to reward.collectible.rarity,
                    "lastRunRewardXp" to reward.xpReward,
                    "lastRunRewardShards" to reward.duplicateShards,
                    "radiantShards" to newShards,
                    "radiantCollection" to newCollectionForFirestore,
                    "collectionOwned" to collectionOwned,
                    "phase" to 10,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            // Keep leaderboard ownership keyed by Firebase UID, while wallet-based
            // collapse still prevents reinstall-created anonymous duplicates.
            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
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

            reward
        }
            .addOnSuccessListener { reward ->
                val duplicateText = if (reward.duplicate) {
                    " Duplicate converted to +${reward.duplicateShards} Radiant Shards."
                } else {
                    " New collectible discovered!"
                }
                loadOrCreateProfile(
                    session.uid,
                    onState,
                    "${reward.capsuleTier} opened: ${reward.collectible.rarity} ${reward.collectible.title}. +${reward.xpReward} XP.$duplicateText",
                )
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not save Radiant Run: ${safeMessage(error)}")
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
                loadOrCreateProfile(session.uid, onState, "Wallet disconnected locally and Firebase profile updated.")
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
        message: String? = "Firebase profile loaded.",
    ) {
        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Firebase is not configured yet.",
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
                        "walletAddress" to null,
                        "walletAddressShort" to null,
                        "walletStatus" to "Wallet not connected yet",
                        "skrTier" to "Explorer",
                        "xp" to 0L,
                        "level" to 1L,
                        "currentStreak" to 0L,
                        "longestStreak" to 0L,
                        "totalChestXp" to 0L,
                        "rushTickets" to RadiantGameRules.STARTER_TICKETS.toLong(),
                        "bestRunScore" to 0L,
                        "totalRuns" to 0L,
                        "radiantShards" to 0L,
                        "radiantCollection" to emptyMap<String, Long>(),
                        "collectionOwned" to 0L,
                        "phase" to 10,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )

                    userRef.set(baseProfile)
                        .addOnSuccessListener {
                            userRef.get()
                                .addOnSuccessListener { createdSnapshot ->
                                    loadCompletedAndLeaderboard(db, uid, createdSnapshot, onState, "Firebase profile created.")
                                }
                                .addOnFailureListener { error ->
                                    onState(errorState("Could not read created Firebase profile: ${safeMessage(error)}"))
                                }
                        }
                        .addOnFailureListener { error ->
                            onState(errorState("Could not create Firebase profile: ${safeMessage(error)}"))
                        }
                }
            }
            .addOnFailureListener { error ->
                onState(errorState("Could not read Firebase profile: ${safeMessage(error)}"))
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

                val walletConnected = !userSnapshot.getString("walletAddress").isNullOrBlank()
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
                                    updatedAtMs = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                                )
                            },
                            limit = 20,
                        )
                        val leaderboard = uniqueWalletRows.mapIndexed { index, row ->
                            LeaderboardPreview(
                                rank = index + 1,
                                name = row.displayName,
                                xp = row.xp,
                                streak = row.streak,
                                tier = row.tier,
                                walletLabel = LeaderboardRules.walletLabel(row),
                            )
                        }.ifEmpty {
                            if (walletConnected) listOf(profileToLeaderboardRow(userSnapshot)) else emptyList()
                        }

                        val user = profileToUser(userSnapshot)
                        val collection = RadiantGameRules.collectionPreview(collectionCounts(userSnapshot))
                        val radiantRun = RadiantRunPreview(
                            rushTickets = user.rushTickets,
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
                                todayKey = today,
                                lastMessage = message,
                            ),
                        )
                    }
                    .addOnFailureListener { error ->
                        onState(errorState("Could not read leaderboard: ${safeMessage(error)}"))
                    }
            }
            .addOnFailureListener { error ->
                onState(errorState("Could not read completed quests: ${safeMessage(error)}"))
            }
    }

    private fun currentFirebaseSession(onState: (RushUiState) -> Unit): FirebaseSession? {
        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Firebase is not configured yet. Add app/google-services.json before saving progress.",
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
            walletStatus = walletStatus,
            walletAddress = walletAddress ?: "Firebase uid: $uidShort • tap Connect Wallet to authorize with MWA",
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
            lastChestClaimDate = snapshot.getString("lastChestClaimDate"),
            lastChestRewardTitle = snapshot.getString("lastChestRewardTitle"),
            lastChestRewardRarity = snapshot.getString("lastChestRewardRarity"),
            lastChestRewardXp = (snapshot.getLong("lastChestRewardXp") ?: 0L).toInt(),
            totalChestXp = (snapshot.getLong("totalChestXp") ?: 0L).toInt(),
            rushTickets = (snapshot.getLong("rushTickets") ?: RadiantGameRules.STARTER_TICKETS.toLong()).toInt(),
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
        )
    }

    private fun profileToLeaderboardRow(snapshot: DocumentSnapshot): LeaderboardPreview = LeaderboardPreview(
        rank = 1,
        name = snapshot.getString("displayName") ?: "Radiant Rookie",
        xp = (snapshot.getLong("xp") ?: 0L).toInt(),
        streak = (snapshot.getLong("currentStreak") ?: 0L).toInt(),
        tier = snapshot.getString("skrTier") ?: "Explorer",
        walletLabel = shortenAddress(snapshot.getString("walletAddress")),
    )

    private fun badgeState(user: UserPreview, completedIds: Set<String>): List<BadgePreview> = listOf(
        BadgePreview("First Launch", "Open the native Android app shell.", unlocked = true),
        BadgePreview("Cloud Synced", "Create a Firebase profile and save progress.", unlocked = true),
        BadgePreview("Daily Saver", "Save the daily Firebase check-in.", unlocked = completedIds.contains(QuestIds.DAILY_CHECK_IN)),
        BadgePreview("Wallet Ready", "Connect with Mobile Wallet Adapter.", unlocked = user.walletStatus == "Wallet connected"),
        BadgePreview("Daily Proof", "Sign the daily proof message.", unlocked = completedIds.contains(QuestIds.SIGN_DAILY_PROOF)),
        BadgePreview("On-Chain Spark", "Submit the first memo proof transaction.", unlocked = completedIds.contains(QuestIds.ON_CHAIN_PROOF)),
        BadgePreview("SKR Radiant", "Hold real mainnet SKR and unlock boosted status.", unlocked = user.hasSkr),
        BadgePreview("Radiant Chest", "Open the daily no-loss chest after completing all proof quests.", unlocked = completedIds.contains(QuestIds.DAILY_RADIANT_CHEST)),
        BadgePreview("First Run", "Finish your first Radiant Run.", unlocked = user.totalRuns > 0),
        BadgePreview("Combo Pilot", "Reach a 10-hit combo in Radiant Run.", unlocked = user.lastRunMaxCombo >= 10),
        BadgePreview("Collector", "Discover three Radiant collectibles.", unlocked = user.collectionOwned >= 3),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = user.currentStreak >= 7),
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
                subtitle = "Reward claimed today. Come back tomorrow for another proof run.",
                progressText = progressText,
                rewardText = "${user.lastChestRewardRarity ?: "Reward"}: ${user.lastChestRewardTitle ?: "Daily bonus"} • +${user.lastChestRewardXp} XP",
                buttonLabel = "Claimed Today",
                lastRewardRarity = user.lastChestRewardRarity,
                lastRewardTitle = user.lastChestRewardTitle,
                lastRewardXp = user.lastChestRewardXp,
            )
            chestReady -> RadiantChestPreview(
                status = RadiantChestStatus.Ready,
                subtitle = "All proofs are done. Open a no-loss chest reveal for bonus XP.",
                progressText = progressText,
                rewardText = if (user.hasSkr) "SKR holder boost included in chest reward." else "No stake. No XP loss. Just a daily reward reveal.",
                buttonLabel = "Open Chest",
            )
            else -> RadiantChestPreview(
                status = RadiantChestStatus.Locked,
                subtitle = "Complete all daily proof quests to unlock today’s chest.",
                progressText = progressText,
                rewardText = "Needs every proof: cloud, wallet, signature, memo, and SKR scan.",
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
        const val SIGNED_PROOF_XP = 75
        const val ON_CHAIN_PROOF_XP = 100
        const val SKR_SCAN_XP = 50
    }
}
