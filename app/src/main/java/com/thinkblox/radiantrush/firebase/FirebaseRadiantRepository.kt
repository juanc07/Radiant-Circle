package com.thinkblox.radiantrush.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.thinkblox.radiantrush.data.AccountIdentityPreview
import com.thinkblox.radiantrush.data.AccountLinkResult
import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import com.thinkblox.radiantrush.data.CircleActionResult
import com.thinkblox.radiantrush.data.CircleDiscoveryResult
import com.thinkblox.radiantrush.data.CircleMemberPreview
import com.thinkblox.radiantrush.data.CircleMemberProfilePreview
import com.thinkblox.radiantrush.data.CircleProfilePreview
import com.thinkblox.radiantrush.data.CircleSocialSnapshot
import com.thinkblox.radiantrush.data.CircleSparkPreview
import com.thinkblox.radiantrush.data.BadgePreview
import com.thinkblox.radiantrush.data.DailyRadiancePreview
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
import com.thinkblox.radiantrush.data.WeeklyCupWinnerPreview
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.data.UserPreview
import com.thinkblox.radiantrush.logic.CircleDiscoveryRules
import com.thinkblox.radiantrush.logic.DailyRadianceRules
import com.thinkblox.radiantrush.logic.LeaderboardCandidate
import com.thinkblox.radiantrush.logic.LeaderboardRules
import com.thinkblox.radiantrush.logic.GameplayXpAward
import com.thinkblox.radiantrush.logic.Phase11CompetitionRules
import com.thinkblox.radiantrush.logic.Phase12CompetitionVerificationRules
import com.thinkblox.radiantrush.logic.Phase12WeeklyCupConfigRules
import com.thinkblox.radiantrush.logic.Phase12WeeklyCupResultRules
import com.thinkblox.radiantrush.logic.TrustedWeeklyCupResultState
import com.thinkblox.radiantrush.logic.TrustedWeeklyCupWinnerInput
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.logic.SharedSparkRules
import com.thinkblox.radiantrush.logic.RetentionRules
import com.thinkblox.radiantrush.logic.RunCompetitionMode
import com.thinkblox.radiantrush.logic.RunLeaderboardCandidate
import com.thinkblox.radiantrush.logic.RunPersonalBest
import com.thinkblox.radiantrush.logic.RunScoreRecord
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
import java.util.Date
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

    fun currentAccountIdentity(): AccountIdentityPreview {
        val app = ensureFirebaseApp() ?: return AccountIdentityPreview()
        val user = FirebaseAuth.getInstance(app).currentUser ?: return AccountIdentityPreview()
        val googleLinked = user.providerData.any { provider -> provider.providerId == GoogleAuthProvider.PROVIDER_ID }
        return AccountIdentityPreview(
            isTemporary = user.isAnonymous || !googleLinked,
            providerLabel = if (googleLinked) "Google" else "This device",
            email = user.email?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Upgrades the current anonymous Firebase account in-place when possible.
     * If this Google account already owns a Radiant Circle UID (for example after reinstall),
     * switch back to that existing UID so its profile/Circle/Radiance state is restored.
     */
    fun linkOrRestoreGoogleAccount(
        idToken: String,
        onResult: (AccountLinkResult) -> Unit,
    ) {
        val app = ensureFirebaseApp()
        if (app == null) {
            onResult(AccountLinkResult(false, message = "Account recovery is unavailable right now."))
            return
        }
        val auth = FirebaseAuth.getInstance(app)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(AccountLinkResult(false, message = "Your Circle account is still getting ready."))
            return
        }
        if (idToken.isBlank()) {
            onResult(AccountLinkResult(false, message = "Google sign-in did not finish. Please try again."))
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val alreadyLinked = currentUser.providerData.any { provider ->
            provider.providerId == GoogleAuthProvider.PROVIDER_ID
        }
        if (!currentUser.isAnonymous && alreadyLinked) {
            onResult(AccountLinkResult(true, message = "Your Circle is already protected with Google."))
            return
        }

        currentUser.linkWithCredential(credential)
            .addOnSuccessListener {
                onResult(
                    AccountLinkResult(
                        success = true,
                        restoredExistingAccount = false,
                        message = "Your Circle is protected. Your profile can now return after reinstall.",
                    ),
                )
            }
            .addOnFailureListener { error ->
                if (error is FirebaseAuthUserCollisionException) {
                    // The Google account is already linked to the user's older durable UID.
                    // This is the expected recovery path after uninstall/reinstall.
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            onResult(
                                AccountLinkResult(
                                    success = true,
                                    restoredExistingAccount = true,
                                    message = "Welcome back. Your saved Circle profile was restored.",
                                ),
                            )
                        }
                        .addOnFailureListener {
                            onResult(
                                AccountLinkResult(
                                    false,
                                    message = "We couldn't restore your saved Circle right now. Please try again.",
                                ),
                            )
                        }
                } else {
                    onResult(
                        AccountLinkResult(
                            false,
                            message = "We couldn't protect your Circle right now. Please try again.",
                        ),
                    )
                }
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
            userExtraFields = mapOf(
                "lastDailyCheckInDate" to todayKey(),
            ),
            onState = onState,
        )
    }

    fun openDailyRadiance(onState: (RushUiState) -> Unit) {
        val session = currentFirebaseSession(onState) ?: return
        val db = FirebaseFirestore.getInstance(session.app)
        val today = todayKey()
        val userRef = db.collection(USERS).document(session.uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val lastOpenedDay = snapshot.getString("dailyRadianceLastOpenedDate")
            if (lastOpenedDay != today) {
                val savedStreak = (snapshot.getLong("dailyRadianceCurrentStreak") ?: 0L).toInt()
                val savedLongest = (snapshot.getLong("dailyRadianceLongestStreak") ?: 0L).toInt()
                val nextStreak = DailyRadianceRules.nextStreak(
                    lastOpenedDay = lastOpenedDay,
                    todayKey = today,
                    currentStreak = savedStreak,
                )
                val content = DailyRadianceRules.contentFor(
                    accountId = session.uid,
                    dayKey = today,
                )

                transaction.set(
                    userRef,
                    mapOf(
                        "dailyRadianceLastOpenedDate" to today,
                        "dailyRadianceCurrentStreak" to nextStreak.toLong(),
                        "dailyRadianceLongestStreak" to maxOf(savedLongest, nextStreak).toLong(),
                        "dailyRadianceMessageId" to content.id,
                        "dailyRadianceCategory" to content.category,
                        "dailyRadianceOpenedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }
            Unit
        }
            .addOnSuccessListener {
                loadOrCreateProfile(session.uid, onState, "Today's Radiance is ready.")
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Daily Radiance open failed: ${safeMessage(error)}")
                // Keep the last known profile/Daily Plan usable instead of replacing the shell
                // with a default error snapshot after a lightweight social action fails.
                loadOrCreateProfile(
                    session.uid,
                    onState,
                    "Couldn't open today's Radiance. Please try again.",
                )
            }
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
        val receiptId = safeResult.receiptId.trim()
        if (!Phase12CompetitionVerificationRules.isValidReceiptId(receiptId)) {
            onState(errorState("Could not save Radiant Rush run: invalid competition receipt id."))
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val leaderboardRef = db.collection(LEADERBOARD).document(session.uid)
        val completedAtMs = safeResult.completedAtEpochMillis
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
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
                runId = receiptId,
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
                        "scoreAuthority" to Phase12CompetitionVerificationRules.CLIENT_REPORTED_SCORE_AUTHORITY,
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
                        "scoreAuthority" to Phase12CompetitionVerificationRules.CLIENT_REPORTED_SCORE_AUTHORITY,
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
                        "scoreAuthority" to Phase12CompetitionVerificationRules.CLIENT_REPORTED_SCORE_AUTHORITY,
                        "payoutEligible" to false,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }

            RadiantRunCommitOutcome(
                reward = reward,
                mode = rankedDecision.mode,
                xpAward = xpAward,
                runRecord = runRecord,
            )
        }
            .addOnSuccessListener { outcome ->
                val reward = outcome.reward
                val mode = outcome.mode
                val xpAward = outcome.xpAward
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
                val savedMessage =
                    "$modeText • ${reward.capsuleTier}: ${reward.collectible.rarity} ${reward.collectible.title}. " +
                        "+${xpAward.grantedXp} performance XP.$duplicateText$capText"

                // Phase 12A.1: do not silently hide receipt failures. The normal
                // run/reward transaction above is already committed, so a receipt
                // failure can never take away the player's score, XP, ticket use,
                // collectible, Weekly PB, or All-Time PB. For Ranked runs we wait
                // only for the separate receipt attempt before refreshing the
                // profile so the result screen reports CREATED / ALREADY EXISTS /
                // FAILED explicitly.
                if (mode == RunCompetitionMode.Ranked) {
                    Log.d(
                        TAG,
                        "Phase12A Ranked run committed; submitting receipt ${outcome.runRecord.runId}",
                    )
                    submitUnverifiedCompetitionReceipt(db, outcome.runRecord) { receiptResult ->
                        val receiptMessage = when (receiptResult.status) {
                            CompetitionReceiptWriteStatus.CREATED ->
                                " Competition receipt ${outcome.runRecord.runId.take(8)} created as UNVERIFIED."

                            CompetitionReceiptWriteStatus.ALREADY_EXISTS ->
                                " Competition receipt ${outcome.runRecord.runId.take(8)} already exists; no duplicate created."

                            CompetitionReceiptWriteStatus.FAILED ->
                                " Competition receipt FAILED; score/reward are still saved. ${receiptResult.detail}"
                        }
                        loadOrCreateProfile(
                            session.uid,
                            onState,
                            savedMessage + receiptMessage,
                        )
                    }
                } else {
                    Log.d(TAG, "Phase12A receipt skipped because run mode is Casual.")
                    loadOrCreateProfile(
                        session.uid,
                        onState,
                        "$savedMessage No trusted-Cup receipt is created for Casual runs.",
                    )
                }
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(session.uid, onState, "Could not save Radiant Rush: ${safeMessage(error)}")
            }
    }

    private enum class CompetitionReceiptWriteStatus {
        CREATED,
        ALREADY_EXISTS,
        FAILED,
    }

    private data class CompetitionReceiptWriteResult(
        val status: CompetitionReceiptWriteStatus,
        val detail: String = "",
    )

    /**
     * Creates the Phase 12A client receipt. This write can only create an
     * UNVERIFIED/non-payout-eligible document; Firestore rules deny all client
     * updates and deletes. A future trusted Admin/server verifier may transition
     * the receipt to VERIFIED or REJECTED.
     *
     * The callback is diagnostic/UX only. The existing run/reward/leaderboard
     * transaction is already committed before this method runs, so receipt
     * persistence never becomes payout authority and never rolls gameplay back.
     */
    private fun submitUnverifiedCompetitionReceipt(
        db: FirebaseFirestore,
        run: RunScoreRecord,
        onComplete: (CompetitionReceiptWriteResult) -> Unit,
    ) {
        if (run.mode != RunCompetitionMode.Ranked) {
            onComplete(
                CompetitionReceiptWriteResult(
                    status = CompetitionReceiptWriteStatus.FAILED,
                    detail = "Run was not Ranked.",
                ),
            )
            return
        }

        val walletAddress = run.walletAddress?.trim()?.takeIf { it.isNotBlank() }
        if (walletAddress == null) {
            val detail = "No connected wallet was attached to the Ranked run."
            Log.w(TAG, "Competition receipt ${run.runId} not submitted: $detail")
            onComplete(
                CompetitionReceiptWriteResult(
                    status = CompetitionReceiptWriteStatus.FAILED,
                    detail = detail,
                ),
            )
            return
        }

        val trust = Phase12CompetitionVerificationRules.clientInitialTrustState()
        val receiptRef = db.collection(COMPETITION_RUN_SUBMISSIONS).document(run.runId)
        val payload = mapOf(
            "schemaVersion" to Phase12CompetitionVerificationRules.RECEIPT_SCHEMA_VERSION,
            "receiptId" to run.runId,
            "ownerUid" to run.ownerUid,
            "walletAddress" to walletAddress,
            "utcDayKey" to run.utcDayKey,
            "utcWeekKey" to run.utcWeekKey,
            "mode" to run.mode.name,
            "score" to run.score,
            "maxCombo" to run.maxCombo,
            "perfectHits" to run.perfectHits,
            "radiantHits" to run.radiantHits,
            "corruptedHits" to run.corruptedHits,
            "clientCompletedAtEpochMillis" to run.completedAtEpochMillis,
            "scoreAuthority" to Phase12CompetitionVerificationRules.CLIENT_REPORTED_SCORE_AUTHORITY,
            "verificationStatus" to trust.verificationStatus.name,
            "trustedPlacementEligible" to trust.trustedPlacementEligible,
            "payoutEligible" to trust.payoutEligible,
            "payoutStatus" to trust.payoutStatus,
            "submittedAt" to FieldValue.serverTimestamp(),
        )

        receiptRef.set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "Created UNVERIFIED competition receipt ${run.runId}")
                onComplete(
                    CompetitionReceiptWriteResult(
                        status = CompetitionReceiptWriteStatus.CREATED,
                    ),
                )
            }
            .addOnFailureListener { writeError ->
                // A retry of the exact same completed run intentionally reuses the
                // same receipt id. Because client updates are denied, .set() on an
                // already-created document is rejected as an update. Read it back
                // and treat an exact same-owner receipt as an idempotent success.
                receiptRef.get()
                    .addOnSuccessListener { existing ->
                        val sameReceiptExists = existing.exists() &&
                            existing.getString("receiptId") == run.runId &&
                            existing.getString("ownerUid") == run.ownerUid

                        if (sameReceiptExists) {
                            Log.d(TAG, "Competition receipt ${run.runId} already exists; retry is idempotent.")
                            onComplete(
                                CompetitionReceiptWriteResult(
                                    status = CompetitionReceiptWriteStatus.ALREADY_EXISTS,
                                ),
                            )
                        } else {
                            val detail = safeMessage(writeError).take(180)
                            Log.w(TAG, "Competition receipt ${run.runId} was not persisted: $detail")
                            onComplete(
                                CompetitionReceiptWriteResult(
                                    status = CompetitionReceiptWriteStatus.FAILED,
                                    detail = detail,
                                ),
                            )
                        }
                    }
                    .addOnFailureListener { readError ->
                        val detail = safeMessage(writeError).take(140)
                        val readDetail = safeMessage(readError).take(100)
                        Log.w(
                            TAG,
                            "Competition receipt ${run.runId} failed and could not be checked: $detail / $readDetail",
                        )
                        onComplete(
                            CompetitionReceiptWriteResult(
                                status = CompetitionReceiptWriteStatus.FAILED,
                                detail = "$detail (verification read also failed: $readDetail)",
                            ),
                        )
                    }
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
                // Circle identity sync is best-effort. The core public profile save must
                // never fail just because the optional social-profile collection is not
                // available yet.
                db.collection(CIRCLE_PROFILES).document(session.uid)
                    .set(
                        mapOf(
                            "ownerUid" to session.uid,
                            "displayName" to cleanName,
                            "avatarId" to cleanAvatar,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    .addOnCompleteListener {
                        loadOrCreateProfile(session.uid, onState, "Public profile updated.")
                    }
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
                        "dailyRadianceLastOpenedDate" to null,
                        "dailyRadianceCurrentStreak" to 0L,
                        "dailyRadianceLongestStreak" to 0L,
                        "dailyRadianceMessageId" to null,
                        "dailyRadianceCategory" to null,
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
        val currentWalletAddress = userSnapshot.getString("walletAddress")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val walletConnected = currentWalletAddress != null
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
        val dailyRadiance = dailyRadianceState(
            uid = uid,
            userSnapshot = userSnapshot,
            today = today,
        )
        val accountIdentity = currentAccountIdentity()

        fun profileFallbackCompletedIds(): MutableSet<String> = mutableSetOf<String>().apply {
            if (userSnapshot.getString("lastDailyCheckInDate") == today) {
                add(QuestIds.DAILY_CHECK_IN)
            }
            if (userSnapshot.getString("lastSignedProofDate") == today) {
                add(QuestIds.SIGN_DAILY_PROOF)
            }
            if (userSnapshot.getString("lastOnChainProofDate") == today) {
                add(QuestIds.ON_CHAIN_PROOF)
            }
            if (userSnapshot.getString("lastSkrCheckDate") == today) {
                add(QuestIds.SKR_HOLDER)
            }
            if (userSnapshot.getString("lastChestClaimDate") == today) {
                add(QuestIds.DAILY_RADIANT_CHEST)
            }
        }

        fun questState(
            completedIds: Set<String>,
            completionHistoryLoaded: Boolean,
        ): List<QuestPreview> = PreviewContent.quests.map { quest ->
            when {
                completedIds.contains(quest.id) -> quest.copy(status = QuestStatus.Completed)
                quest.id == QuestIds.WALLET_CONNECT && walletConnected -> quest.copy(status = QuestStatus.Completed)
                quest.id == QuestIds.DAILY_CHECK_IN && !completionHistoryLoaded -> quest.copy(status = QuestStatus.Syncing)
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

        fun chestState(completedIds: Set<String>, quests: List<QuestPreview>): RadiantChestPreview {
            val chestClaimedToday = completedIds.contains(QuestIds.DAILY_RADIANT_CHEST)
            val questStatuses = quests.associate { it.id to it.status }
            val chestReady = RewardLoopRules.canClaimDailyChest(
                questStatuses = questStatuses,
                alreadyClaimedToday = chestClaimedToday,
            )
            return radiantChestState(
                user = user,
                completedQuestCount = quests.count { it.status == QuestStatus.Completed },
                totalQuestCount = quests.size,
                chestReady = chestReady,
                chestClaimedToday = chestClaimedToday,
            )
        }

        // Phase 12D UX rule: the profile document is the critical bootstrap boundary.
        // Once it is available the app, Home and MWA are usable. Daily quest history,
        // ranks and Cup data are enrichment and may hydrate afterward without blocking
        // the whole product shell.
        val fallbackCompletedIds = profileFallbackCompletedIds()
        val fallbackQuests = questState(
            completedIds = fallbackCompletedIds,
            completionHistoryLoaded = false,
        )
        onState(
            RushUiState(
                firebaseStatus = FirebaseStatus.Ready,
                accountIdentity = accountIdentity,
                user = user,
                quests = fallbackQuests,
                dailyRadiance = dailyRadiance,
                radiantChest = chestState(fallbackCompletedIds, fallbackQuests),
                radiantRun = radiantRun,
                collection = collection,
                badges = badgeState(user, fallbackCompletedIds),
                leaderboard = emptyList(),
                runCompetition = RunCompetitionPreview(),
                retention = retentionState(
                    user = user,
                    radiantRun = radiantRun,
                    competition = RunCompetitionPreview(),
                ),
                todayKey = today,
                lastMessage = message,
                backgroundSyncInProgress = true,
            ),
        )

        fun continueBackgroundHydration(
            completedIds: Set<String>,
            questWarning: String? = null,
        ) {
            val quests = questState(
                completedIds = completedIds,
                completionHistoryLoaded = true,
            )
            val radiantChest = chestState(completedIds, quests)
            val badges = badgeState(user, completedIds)

            fun finishBackgroundHydration(
                leaderboard: List<LeaderboardPreview>,
                leaderboardWarning: String? = null,
            ) {
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
                    val mergedMessage = listOfNotNull(
                        message,
                        questWarning,
                        leaderboardWarning,
                        competitionWarning,
                    )
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                    onState(
                        RushUiState(
                            firebaseStatus = FirebaseStatus.Ready,
                            accountIdentity = accountIdentity,
                            user = user,
                            quests = quests,
                            dailyRadiance = dailyRadiance,
                            radiantChest = radiantChest,
                            radiantRun = radiantRun,
                            collection = collection,
                            badges = badges,
                            leaderboard = leaderboard,
                            runCompetition = runCompetition,
                            retention = retention,
                            todayKey = today,
                            lastMessage = mergedMessage,
                            backgroundSyncInProgress = false,
                        ),
                    )
                }
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
                    finishBackgroundHydration(leaderboard)
                }
                .addOnFailureListener { error ->
                    // Ranking is enrichment, not an app availability gate.
                    finishBackgroundHydration(
                        leaderboard = if (walletConnected) {
                            listOf(profileToLeaderboardRow(userSnapshot))
                        } else {
                            emptyList()
                        },
                        leaderboardWarning = "Ranks are temporarily unavailable: ${safeMessage(error)}",
                    )
                }
        }

        db.collection(USERS)
            .document(uid)
            .collection(COMPLETED_QUESTS)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { completedQuery ->
                val completedIds = profileFallbackCompletedIds().apply {
                    addAll(completedQuery.documents.mapNotNull { it.getString("questId") })
                }

                // Older profiles created before the stable-refresh fix may have a valid daily
                // check-in receipt but no profile-level fallback marker. Backfill the marker once
                // so later profile-first refreshes can render Done immediately without a CTA flash.
                if (
                    completedIds.contains(QuestIds.DAILY_CHECK_IN) &&
                    userSnapshot.getString("lastDailyCheckInDate") != today
                ) {
                    db.collection(USERS)
                        .document(uid)
                        .set(
                            mapOf(
                                "lastDailyCheckInDate" to today,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                            SetOptions.merge(),
                        )
                }

                continueBackgroundHydration(completedIds)
            }
            .addOnFailureListener { error ->
                // Quest-history availability must not turn a valid profile/session into
                // a global sync failure. Profile proof-date fallbacks keep wallet-bound
                // tasks honest until the next successful refresh.
                continueBackgroundHydration(
                    completedIds = fallbackCompletedIds,
                    questWarning = "Daily progress is still catching up: ${safeMessage(error)}",
                )
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

            fun buildCup(
                sponsor: CupSponsorPresentation,
                previousRank: Int? = null,
                previousRewardTitle: String? = null,
                finalResult: TrustedWeeklyCupResultState? = null,
            ): WeeklyCupPreview {
                val trustedWinnerRank = finalResult?.winners
                    ?.firstOrNull { winner -> walletAddress != null && winner.walletAddress == walletAddress }
                    ?.placement
                val trustedWinnerReward = trustedWinnerRank?.let(WeeklyRadiantCupRules::rewardForPlacement)
                return WeeklyCupPreview(
                    seasonKey = currentWeek,
                    seasonStartsAtEpochMillis = sponsor.startsAtEpochMillis ?: 0L,
                    seasonEndsAtEpochMillis = sponsor.endsAtEpochMillis ?: seasonEndsAt,
                    participantCount = participantCount,
                    personalRank = personalRank,
                    personalBestScore = personal.weeklyBestScore,
                    projectedRewardTitle = projectedReward.title,
                    projectedRewardDetail = projectedReward.detail,
                    previousSeasonKey = previousWeek,
                    previousSeasonRank = if (finalResult != null) trustedWinnerRank else previousRank,
                    previousRewardTitle = if (finalResult != null) trustedWinnerReward?.title else previousRewardTitle,
                    sponsorName = sponsor.sponsorName,
                    sponsoredPrizeLabel = sponsor.prizeLabel,
                    sponsoredPrizeStatus = sponsor.sponsorStatus,
                    sponsorNote = sponsor.sponsorNote,
                    sponsoredPrizeActive = sponsor.sponsorActive,
                    trustedSponsorConfig = sponsor.trustedConfig,
                    cupStatusCode = sponsor.cupStatusCode,
                    cupStatusLabel = sponsor.cupStatusLabel,
                    fundingVerificationStatus = sponsor.fundingStatus,
                    fundingVerificationLabel = sponsor.fundingLabel,
                    placementAllocationLabel = sponsor.placementAllocationLabel,
                    trustedResultsRequired = sponsor.trustedResultsRequired,
                    payoutEnabled = false,
                    finalResultWeekKey = finalResult?.weekKey,
                    finalResultFundingStatus = finalResult?.fundingStatusCode,
                    finalResultFundingLabel = finalResult?.fundingStatusLabel,
                    finalWinners = finalResult?.winners.orEmpty().map { winner ->
                        WeeklyCupWinnerPreview(
                            placement = winner.placement,
                            walletLabel = winner.walletLabel,
                            score = winner.score,
                            prizeLabel = winner.prizeLabel,
                        )
                    },
                )
            }

            fun loadTrustedFinalResult(
                resultWeekKey: String,
                onResult: (TrustedWeeklyCupResultState?) -> Unit,
            ) {
                val resultRef = db.collection(WEEKLY_CUP_RESULTS).document(resultWeekKey)
                resultRef.get()
                    .addOnSuccessListener { resultDocument ->
                        if (!resultDocument.exists()) {
                            onResult(null)
                            return@addOnSuccessListener
                        }

                        resultRef.collection(WEEKLY_CUP_WINNERS)
                            .orderBy("placement", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener { winnerQuery ->
                                fun stringField(name: String): String? = resultDocument.get(name) as? String
                                fun intField(name: String): Int? = (resultDocument.get(name) as? Number)?.toInt()
                                fun booleanField(name: String): Boolean? = resultDocument.get(name) as? Boolean
                                fun timestampMillis(name: String): Long? =
                                    (resultDocument.get(name) as? Timestamp)?.toDate()?.time

                                val allocations = (resultDocument.get("placementAllocationsBps") as? Map<*, *>)
                                    .orEmpty()
                                    .mapNotNull { (rawRank, rawBps) ->
                                        val rank = rawRank?.toString()?.toIntOrNull()
                                        val bps = (rawBps as? Number)?.toInt()
                                        if (rank != null && bps != null) rank to bps else null
                                    }
                                    .toMap()
                                val winnerInputs = winnerQuery.documents.map { winnerDocument ->
                                    TrustedWeeklyCupWinnerInput(
                                        schemaVersion = (winnerDocument.get("schemaVersion") as? Number)?.toInt(),
                                        resultVersion = (winnerDocument.get("resultVersion") as? Number)?.toInt(),
                                        weekKey = winnerDocument.getString("weekKey"),
                                        placement = (winnerDocument.getLong("placement") ?: 0L).toInt(),
                                        walletAddress = winnerDocument.getString("walletAddress"),
                                        receiptId = winnerDocument.getString("receiptId"),
                                        score = (winnerDocument.get("score") as? Number)?.toInt(),
                                        maxCombo = (winnerDocument.get("maxCombo") as? Number)?.toInt(),
                                        perfectHits = (winnerDocument.get("perfectHits") as? Number)?.toInt(),
                                        prizeAmountAtomic = winnerDocument.getString("prizeAmountAtomic"),
                                        prizeAssetSymbol = winnerDocument.getString("prizeAssetSymbol"),
                                        payoutStatus = winnerDocument.getString("payoutStatus"),
                                        fundingVerificationStatusAtClose = winnerDocument.getString("fundingVerificationStatusAtClose"),
                                        resultAuthority = winnerDocument.getString("resultAuthority"),
                                        payoutEnabled = winnerDocument.get("payoutEnabled") as? Boolean,
                                        payoutReady = winnerDocument.get("payoutReady") as? Boolean,
                                    )
                                }

                                val result = Phase12WeeklyCupResultRules.presentation(
                                    expectedWeekKey = resultWeekKey,
                                    schemaVersion = intField("schemaVersion"),
                                    resultVersion = intField("resultVersion"),
                                    weekKey = stringField("weekKey"),
                                    finalizationStatus = stringField("finalizationStatus"),
                                    finalizationAuthority = stringField("finalizationAuthority"),
                                    finalizedAtEpochMillis = timestampMillis("finalizedAt"),
                                    prizeAssetSymbol = stringField("prizeAssetSymbol"),
                                    prizeMint = stringField("prizeMint"),
                                    prizeDecimals = intField("prizeDecimals"),
                                    prizeAmountAtomic = stringField("prizeAmountAtomic"),
                                    placementAllocationsBps = allocations,
                                    fundingVerificationStatusAtClose = stringField("fundingVerificationStatusAtClose"),
                                    payoutEnabled = booleanField("payoutEnabled"),
                                    payoutReady = booleanField("payoutReady"),
                                    winnerCount = intField("winnerCount"),
                                    winners = winnerInputs,
                                )
                                onResult(result.takeIf { it.recognized })
                            }
                            .addOnFailureListener { onResult(null) }
                    }
                    .addOnFailureListener { onResult(null) }
            }

            fun loadPreviousSeason(sponsor: CupSponsorPresentation) {
                loadTrustedFinalResult(previousWeek) { finalResult ->
                    if (finalResult != null) {
                        onDone(buildCup(sponsor = sponsor, finalResult = finalResult))
                        return@loadTrustedFinalResult
                    }
                    if (!walletConnected || walletAddress == null) {
                        onDone(buildCup(sponsor))
                        return@loadTrustedFinalResult
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
                                buildCup(
                                    sponsor = sponsor,
                                    previousRank = previousRank,
                                    previousRewardTitle = previousReward?.title,
                                ),
                            )
                        }
                        .addOnFailureListener {
                            onDone(buildCup(sponsor))
                        }
                }
            }

            db.collection(WEEKLY_CUP_CONFIGS)
                .document(currentWeek)
                .get()
                .addOnSuccessListener { sponsorDocument ->
                    fun stringField(name: String): String? = sponsorDocument.get(name) as? String
                    fun intField(name: String): Int? = (sponsorDocument.get(name) as? Number)?.toInt()
                    fun longField(name: String): Long? = (sponsorDocument.get(name) as? Number)?.toLong()
                    fun booleanField(name: String): Boolean? = sponsorDocument.get(name) as? Boolean
                    fun timestampMillis(name: String): Long? =
                        (sponsorDocument.get(name) as? Timestamp)?.toDate()?.time

                    val schemaVersion = intField("schemaVersion")
                    val trustedPresentation = if (schemaVersion == Phase12WeeklyCupConfigRules.SCHEMA_VERSION) {
                        val allocationMap = (sponsorDocument.get("placementAllocationsBps") as? Map<*, *>)
                            .orEmpty()
                            .mapNotNull { (rawRank, rawBps) ->
                                val rank = rawRank?.toString()?.toIntOrNull()
                                val bps = (rawBps as? Number)?.toInt()
                                if (rank != null && bps != null) rank to bps else null
                            }
                            .toMap()
                        Phase12WeeklyCupConfigRules.presentation(
                            expectedWeekKey = currentWeek,
                            schemaVersion = schemaVersion,
                            weekKey = stringField("weekKey"),
                            status = stringField("status"),
                            sponsorName = stringField("sponsorName"),
                            sponsorNote = stringField("sponsorNote"),
                            prizeAssetSymbol = stringField("prizeAssetSymbol"),
                            prizeMint = stringField("prizeMint"),
                            prizeDecimals = intField("prizeDecimals"),
                            prizeAmountAtomic = stringField("prizeAmountAtomic"),
                            placementAllocationsBps = allocationMap,
                            startsAtEpochMillis = timestampMillis("startsAt"),
                            endsAtEpochMillis = timestampMillis("endsAt"),
                            fundingWalletAddress = stringField("fundingWalletAddress"),
                            fundingVerificationStatus = stringField("fundingVerificationStatus"),
                            fundingRequiredAmountAtomic = stringField("fundingRequiredAmountAtomic"),
                            fundingObservedAmountAtomic = stringField("fundingObservedAmountAtomic"),
                            fundingVerificationSlot = longField("fundingVerificationSlot"),
                            fundingVerificationNetwork = stringField("fundingVerificationNetwork"),
                            fundingVerificationMint = stringField("fundingVerificationMint"),
                            fundingVerificationCommitment = stringField("fundingVerificationCommitment"),
                            fundingVerificationAuthority = stringField("fundingVerificationAuthority"),
                            fundingVerificationSchemaVersion = intField("fundingVerificationSchemaVersion"),
                            fundingCheckedAtEpochMillis = timestampMillis("fundingCheckedAt"),
                            fundingVerifiedAtEpochMillis = timestampMillis("fundingVerifiedAt"),
                            configurationAuthority = stringField("configurationAuthority"),
                            trustedResultsRequired = booleanField("trustedResultsRequired"),
                        )
                    } else {
                        null
                    }

                    if (trustedPresentation?.recognized == true) {
                        loadPreviousSeason(
                            CupSponsorPresentation(
                                sponsorName = trustedPresentation.sponsorName,
                                prizeLabel = trustedPresentation.prizeLabel,
                                sponsorStatus = trustedPresentation.statusLabel,
                                sponsorNote = trustedPresentation.sponsorNote,
                                sponsorActive = trustedPresentation.published,
                                trustedConfig = true,
                                cupStatusCode = trustedPresentation.statusCode,
                                cupStatusLabel = trustedPresentation.statusLabel,
                                startsAtEpochMillis = trustedPresentation.startsAtEpochMillis,
                                endsAtEpochMillis = trustedPresentation.endsAtEpochMillis,
                                fundingStatus = trustedPresentation.fundingStatusCode,
                                fundingLabel = trustedPresentation.fundingStatusLabel,
                                placementAllocationLabel = trustedPresentation.placementAllocationLabel,
                                trustedResultsRequired = trustedPresentation.trustedResultsRequired,
                            ),
                        )
                    } else {
                        // Backward-compatible display for the Phase 11 sponsor announcement shape.
                        // It is deliberately not labeled as a trusted Phase 12 config.
                        val legacy = WeeklyRadiantCupRules.sponsorState(
                            status = stringField("status"),
                            sponsorName = stringField("sponsorName"),
                            prizeLabel = stringField("prizeLabel"),
                            note = stringField("note"),
                        )
                        loadPreviousSeason(
                            CupSponsorPresentation(
                                sponsorName = legacy.sponsorName,
                                prizeLabel = legacy.prizeLabel,
                                sponsorStatus = if (legacy.active) {
                                    "Legacy sponsor announcement"
                                } else {
                                    "No sponsored prize this week"
                                },
                                sponsorNote = legacy.note,
                                sponsorActive = legacy.active,
                                trustedConfig = false,
                                cupStatusCode = if (legacy.active) "LEGACY_ANNOUNCED" else "UNCONFIGURED",
                                cupStatusLabel = if (legacy.active) "Legacy announcement" else "No trusted Cup config",
                                fundingStatus = Phase12WeeklyCupConfigRules.FUNDING_NOT_VERIFIED,
                                fundingLabel = if (legacy.active) "Funding not verified" else "Funding wallet not configured",
                                trustedResultsRequired = true,
                            ),
                        )
                    }
                }
                .addOnFailureListener {
                    loadPreviousSeason(CupSponsorPresentation())
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


    fun loadMyCircleProfile(onResult: (CircleProfilePreview?, String?) -> Unit) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(null, "Your Circle profile is still getting ready.")
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        db.collection(USERS).document(session.uid).get()
            .addOnSuccessListener { userSnapshot ->
                val user = profileToUser(userSnapshot)
                db.collection(CIRCLE_PROFILES).document(session.uid).get()
                    .addOnSuccessListener { profileSnapshot ->
                        onResult(
                            circleProfileFromDocument(
                                document = profileSnapshot,
                                uid = session.uid,
                                fallbackName = user.displayName,
                                fallbackAvatarId = user.avatarId,
                            ),
                            null,
                        )
                    }
                    .addOnFailureListener { error ->
                        onResult(null, circleFriendlyFailure("load your social profile", error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(null, circleFriendlyFailure("load your social profile", error))
            }
    }

    fun saveCircleProfile(
        profile: CircleProfilePreview,
        onResult: (CircleActionResult) -> Unit,
    ) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(CircleActionResult(false, "Your Circle account is still getting ready."))
            return
        }

        val clean = SharedSparkRules.sanitizeProfile(profile)
        val db = FirebaseFirestore.getInstance(session.app)
        db.collection(USERS).document(session.uid).get()
            .addOnSuccessListener { userSnapshot ->
                val user = profileToUser(userSnapshot)
                val payload = mapOf(
                    "ownerUid" to session.uid,
                    "displayName" to PublicProfileRules.sanitizeDisplayName(user.displayName),
                    "avatarId" to PublicProfileRules.normalizeAvatarId(user.avatarId),
                    "motto" to clean.motto,
                    "favoriteFood" to clean.favoriteFood,
                    "music" to clean.music,
                    "games" to clean.games,
                    "hobbies" to clean.hobbies,
                    "books" to clean.books,
                    "pets" to clean.pets,
                    "currentlyInto" to clean.currentlyInto,
                    "weekendVibe" to clean.weekendVibe,
                    "talkAbout" to clean.talkAbout,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                db.collection(CIRCLE_PROFILES).document(session.uid)
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener {
                        onResult(CircleActionResult(true, "Your Circle profile is updated."))
                    }
                    .addOnFailureListener { error ->
                        onResult(CircleActionResult(false, circleFriendlyFailure("save your social profile", error)))
                    }
            }
            .addOnFailureListener { error ->
                onResult(CircleActionResult(false, circleFriendlyFailure("save your social profile", error)))
            }
    }

    fun loadCircleMemberProfile(
        member: CircleMemberPreview,
        onResult: (CircleMemberProfilePreview?, String?) -> Unit,
    ) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(null, "Circle needs cloud sign-in.")
            return
        }
        if (member.uid.isBlank() || member.uid == session.uid) {
            onResult(null, "Choose another member in your Circle.")
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        val edgeId = CircleDiscoveryRules.pairId(session.uid, member.uid)
        db.collection(CIRCLE_EDGES).document(edgeId).get()
            .addOnSuccessListener { edgeSnapshot ->
                if (!edgeSnapshot.exists() || edgeSnapshot.getString("status") != CIRCLE_STATUS_ACCEPTED) {
                    onResult(null, "This profile is available after a Spark is accepted.")
                    return@addOnSuccessListener
                }

                db.collection(CIRCLE_PROFILES).document(member.uid).get()
                    .addOnSuccessListener { memberProfileSnapshot ->
                        val memberProfile = circleProfileFromDocument(
                            document = memberProfileSnapshot,
                            uid = member.uid,
                            fallbackName = member.displayName,
                            fallbackAvatarId = member.avatarId,
                        )
                        db.collection(CIRCLE_PROFILES).document(session.uid).get()
                            .addOnSuccessListener { myProfileSnapshot ->
                                val myProfile = circleProfileFromDocument(
                                    document = myProfileSnapshot,
                                    uid = session.uid,
                                    fallbackName = "",
                                    fallbackAvatarId = "fox",
                                )
                                onResult(
                                    CircleMemberProfilePreview(
                                        member = member.copy(
                                            displayName = memberProfile.displayName,
                                            avatarId = memberProfile.avatarId,
                                        ),
                                        profile = memberProfile,
                                        sharedSparks = SharedSparkRules.sharedSparks(myProfile, memberProfile, limit = 5),
                                    ),
                                    null,
                                )
                            }
                            .addOnFailureListener {
                                onResult(
                                    CircleMemberProfilePreview(
                                        member = member.copy(
                                            displayName = memberProfile.displayName,
                                            avatarId = memberProfile.avatarId,
                                        ),
                                        profile = memberProfile,
                                    ),
                                    null,
                                )
                            }
                    }
                    .addOnFailureListener { error ->
                        onResult(null, circleFriendlyFailure("open this Circle profile", error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(null, circleFriendlyFailure("open this Circle profile", error))
            }
    }


    fun loadCircleSocial(onResult: (CircleSocialSnapshot?, String?) -> Unit) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(null, "Circle needs cloud sign-in. Try again in a moment.")
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        db.collection(CIRCLE_EDGES)
            .whereArrayContains("memberUids", session.uid)
            .limit(60)
            .get()
            .addOnSuccessListener { snapshot ->
                val incoming = mutableListOf<CircleSparkPreview>()
                val connections = mutableListOf<CircleSparkPreview>()
                snapshot.documents.forEach { document ->
                    val edge = circleEdgePreview(document, session.uid) ?: return@forEach
                    when (edge.status) {
                        CIRCLE_STATUS_PENDING -> if (edge.incoming) incoming += edge
                        CIRCLE_STATUS_ACCEPTED -> connections += edge
                    }
                }
                onResult(
                    CircleSocialSnapshot(
                        incomingRequests = incoming.sortedBy { it.member.displayName.lowercase(Locale.US) },
                        connections = connections.sortedBy { it.member.displayName.lowercase(Locale.US) },
                    ),
                    null,
                )
            }
            .addOnFailureListener { error ->
                onResult(null, circleFriendlyFailure("refresh your Circle", error))
            }
    }

    fun startCircleDiscovery(
        location: ApproximateCircleLocation,
        onResult: (CircleDiscoveryResult) -> Unit,
    ) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(CircleDiscoveryResult(message = "Circle needs cloud sign-in. Try again in a moment."))
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        val now = System.currentTimeMillis()
        val presenceKeys = CircleDiscoveryRules.presenceKeys(location, now)
        val userRef = db.collection(USERS).document(session.uid)

        userRef.get()
            .addOnSuccessListener { userSnapshot ->
                if (!userSnapshot.exists()) {
                    onResult(CircleDiscoveryResult(message = "Your Radiant Circle profile is still loading."))
                    return@addOnSuccessListener
                }

                val user = profileToUser(userSnapshot)
                val presenceRef = db.collection(CIRCLE_DISCOVERY).document(session.uid)
                val presence = mapOf(
                    "ownerUid" to session.uid,
                    "displayName" to PublicProfileRules.sanitizeDisplayName(user.displayName),
                    "avatarId" to PublicProfileRules.normalizeAvatarId(user.avatarId),
                    "radianceStreak" to (userSnapshot.getLong("dailyRadianceCurrentStreak") ?: 0L).coerceAtLeast(0L),
                    "level" to user.level.coerceAtLeast(1).toLong(),
                    "localWindowKeys" to presenceKeys.localWindowKeys,
                    "regionalWindowKeys" to presenceKeys.regionalWindowKeys,
                    "broadWindowKeys" to presenceKeys.broadWindowKeys,
                    "countryWindowKeys" to presenceKeys.countryWindowKeys,
                    "globalWindowKeys" to presenceKeys.globalWindowKeys,
                    "expiresAtEpochMillis" to (now + CircleDiscoveryRules.DISCOVERY_WINDOW_MILLIS),
                    "expiresAt" to Timestamp(Date(now + CircleDiscoveryRules.DISCOVERY_WINDOW_MILLIS)),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                presenceRef.set(presence)
                    .addOnSuccessListener {
                        db.collection(CIRCLE_EDGES)
                            .whereArrayContains("memberUids", session.uid)
                            .limit(80)
                            .get()
                            .addOnSuccessListener { edges ->
                                val excludedUids = buildSet {
                                    add(session.uid)
                                    edges.documents.forEach { edge ->
                                        (edge.get("memberUids") as? List<*>)
                                            ?.filterIsInstance<String>()
                                            ?.forEach(::add)
                                    }
                                }
                                searchCircleTier(
                                    db = db,
                                    session = session,
                                    location = location,
                                    nowMillis = now,
                                    excludedUids = excludedUids,
                                    tierIndex = 0,
                                    onResult = onResult,
                                )
                            }
                            .addOnFailureListener {
                                searchCircleTier(
                                    db = db,
                                    session = session,
                                    location = location,
                                    nowMillis = now,
                                    excludedUids = setOf(session.uid),
                                    tierIndex = 0,
                                    onResult = onResult,
                                )
                            }
                    }
                    .addOnFailureListener { error ->
                        onResult(CircleDiscoveryResult(message = circleFriendlyFailure("start discovery", error)))
                    }
            }
            .addOnFailureListener { error ->
                onResult(CircleDiscoveryResult(message = circleFriendlyFailure("load your Circle profile", error)))
            }
    }

    fun sendCircleSpark(
        member: CircleMemberPreview,
        onResult: (CircleActionResult) -> Unit,
    ) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(CircleActionResult(false, "Your Circle account is still getting ready."))
            return
        }
        if (member.uid.isBlank() || member.uid == session.uid) {
            onResult(CircleActionResult(false, "Choose another Radiant Circle member."))
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        val userRef = db.collection(USERS).document(session.uid)
        val edgeId = CircleDiscoveryRules.pairId(session.uid, member.uid)
        val edgeRef = db.collection(CIRCLE_EDGES).document(edgeId)

        userRef.get()
            .addOnSuccessListener { userSnapshot ->
                val user = profileToUser(userSnapshot)
                val payload = mapOf(
                    "memberUids" to listOf(session.uid, member.uid).sorted(),
                    "initiatorUid" to session.uid,
                    "recipientUid" to member.uid,
                    "status" to CIRCLE_STATUS_PENDING,
                    "initiatorDisplayName" to PublicProfileRules.sanitizeDisplayName(user.displayName),
                    "initiatorAvatarId" to PublicProfileRules.normalizeAvatarId(user.avatarId),
                    "recipientDisplayName" to PublicProfileRules.sanitizeDisplayName(member.displayName),
                    "recipientAvatarId" to PublicProfileRules.normalizeAvatarId(member.avatarId),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                // Do not transaction-get a deterministic edge before creation. Security rules
                // intentionally hide non-existent relationship documents, so that pre-read can
                // fail with PERMISSION_DENIED. A direct create succeeds for a new Spark. If the
                // relationship already exists the write is denied by the update rule, after which
                // a member-authorized read tells us the existing status without weakening rules.
                edgeRef.set(payload)
                    .addOnSuccessListener {
                        onResult(CircleActionResult(true, "Spark sent to ${member.displayName}."))
                    }
                    .addOnFailureListener { writeError ->
                        edgeRef.get()
                            .addOnSuccessListener { existing ->
                                if (existing.exists()) {
                                    val message = when (existing.getString("status")) {
                                        CIRCLE_STATUS_ACCEPTED -> "${member.displayName} is already in your Circle."
                                        CIRCLE_STATUS_IGNORED -> "That Spark was previously passed on."
                                        else -> "A Spark is already waiting for ${member.displayName}."
                                    }
                                    onResult(CircleActionResult(true, message))
                                } else {
                                    onResult(CircleActionResult(false, circleFriendlyFailure("send this Spark", writeError)))
                                }
                            }
                            .addOnFailureListener {
                                onResult(CircleActionResult(false, circleFriendlyFailure("send this Spark", writeError)))
                            }
                    }
            }
            .addOnFailureListener { error ->
                onResult(CircleActionResult(false, circleFriendlyFailure("load your Circle profile", error)))
            }
    }

    fun respondToCircleSpark(
        edgeId: String,
        accept: Boolean,
        onResult: (CircleActionResult) -> Unit,
    ) {
        val session = currentCircleSession()
        if (session == null) {
            onResult(CircleActionResult(false, "Circle needs cloud sign-in."))
            return
        }

        val db = FirebaseFirestore.getInstance(session.app)
        val edgeRef = db.collection(CIRCLE_EDGES).document(edgeId)
        val nextStatus = if (accept) CIRCLE_STATUS_ACCEPTED else CIRCLE_STATUS_IGNORED
        edgeRef.update(
            mapOf(
                "status" to nextStatus,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener {
                onResult(
                    CircleActionResult(
                        true,
                        if (accept) "Spark accepted. Your Circle just grew." else "Spark passed on.",
                    ),
                )
            }
            .addOnFailureListener { error ->
                onResult(CircleActionResult(false, circleFriendlyFailure("update this Spark", error)))
            }
    }

    private fun searchCircleTier(
        db: FirebaseFirestore,
        session: FirebaseSession,
        location: ApproximateCircleLocation,
        nowMillis: Long,
        excludedUids: Set<String>,
        tierIndex: Int,
        onResult: (CircleDiscoveryResult) -> Unit,
    ) {
        val tiers = CircleDiscoveryRules.SearchTier.entries
        if (tierIndex >= tiers.size) {
            onResult(
                CircleDiscoveryResult(
                    message = "No new Spark is active right now. Try another shake soon.",
                ),
            )
            return
        }

        val tier = tiers[tierIndex]
        val queryKeys = CircleDiscoveryRules.queryKeys(tier, location, nowMillis)
        db.collection(CIRCLE_DISCOVERY)
            .whereArrayContainsAny(tier.fieldName, queryKeys)
            .limit(40)
            .get()
            .addOnSuccessListener { snapshot ->
                val candidates = snapshot.documents
                    .filter { document ->
                        val uid = document.getString("ownerUid").orEmpty()
                        uid.isNotBlank() &&
                            uid !in excludedUids &&
                            (document.getLong("expiresAtEpochMillis") ?: 0L) > System.currentTimeMillis()
                    }

                if (candidates.isEmpty()) {
                    searchCircleTier(
                        db = db,
                        session = session,
                        location = location,
                        nowMillis = nowMillis,
                        excludedUids = excludedUids,
                        tierIndex = tierIndex + 1,
                        onResult = onResult,
                    )
                    return@addOnSuccessListener
                }

                val document = candidates.shuffled().first()
                val member = CircleMemberPreview(
                    uid = document.getString("ownerUid").orEmpty(),
                    displayName = PublicProfileRules.sanitizeDisplayName(
                        document.getString("displayName") ?: "Radiant Rookie",
                    ),
                    avatarId = PublicProfileRules.normalizeAvatarId(document.getString("avatarId")),
                    radianceStreak = (document.getLong("radianceStreak") ?: 0L).toInt().coerceAtLeast(0),
                    level = (document.getLong("level") ?: 1L).toInt().coerceAtLeast(1),
                    distanceLabel = tier.distanceLabel,
                )
                enrichDiscoveryWithSharedSparks(
                    db = db,
                    session = session,
                    member = member,
                    onResult = onResult,
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Circle discovery ${tier.name} failed: ${safeMessage(error)}")
                searchCircleTier(
                    db = db,
                    session = session,
                    location = location,
                    nowMillis = nowMillis,
                    excludedUids = excludedUids,
                    tierIndex = tierIndex + 1,
                    onResult = onResult,
                )
            }
    }


    private fun enrichDiscoveryWithSharedSparks(
        db: FirebaseFirestore,
        session: FirebaseSession,
        member: CircleMemberPreview,
        onResult: (CircleDiscoveryResult) -> Unit,
    ) {
        db.collection(CIRCLE_PROFILES).document(member.uid).get()
            .addOnSuccessListener { memberSnapshot ->
                val memberProfile = circleProfileFromDocument(
                    document = memberSnapshot,
                    uid = member.uid,
                    fallbackName = member.displayName,
                    fallbackAvatarId = member.avatarId,
                )
                db.collection(CIRCLE_PROFILES).document(session.uid).get()
                    .addOnSuccessListener { mySnapshot ->
                        val mine = circleProfileFromDocument(
                            document = mySnapshot,
                            uid = session.uid,
                            fallbackName = "",
                            fallbackAvatarId = "fox",
                        )
                        onResult(
                            CircleDiscoveryResult(
                                member = member.copy(
                                    displayName = memberProfile.displayName,
                                    avatarId = memberProfile.avatarId,
                                    sharedSparks = SharedSparkRules.sharedSparks(mine, memberProfile),
                                ),
                                message = "You found a new Spark.",
                            ),
                        )
                    }
                    .addOnFailureListener {
                        onResult(CircleDiscoveryResult(member = member, message = "You found a new Spark."))
                    }
            }
            .addOnFailureListener {
                onResult(CircleDiscoveryResult(member = member, message = "You found a new Spark."))
            }
    }

    private fun circleProfileFromDocument(
        document: DocumentSnapshot,
        uid: String,
        fallbackName: String,
        fallbackAvatarId: String,
    ): CircleProfilePreview = SharedSparkRules.sanitizeProfile(
        CircleProfilePreview(
            uid = uid,
            displayName = PublicProfileRules.sanitizeDisplayName(
                document.getString("displayName") ?: fallbackName.ifBlank { "Radiant Rookie" },
            ),
            avatarId = PublicProfileRules.normalizeAvatarId(
                document.getString("avatarId") ?: fallbackAvatarId,
            ),
            motto = document.getString("motto").orEmpty(),
            favoriteFood = document.getString("favoriteFood").orEmpty(),
            music = document.getString("music").orEmpty(),
            games = document.getString("games").orEmpty(),
            hobbies = document.getString("hobbies").orEmpty(),
            books = document.getString("books").orEmpty(),
            pets = document.getString("pets").orEmpty(),
            currentlyInto = document.getString("currentlyInto").orEmpty(),
            weekendVibe = document.getString("weekendVibe").orEmpty(),
            talkAbout = document.getString("talkAbout").orEmpty(),
        ),
    )

    private fun circleEdgePreview(
        document: DocumentSnapshot,
        currentUid: String,
    ): CircleSparkPreview? {
        val initiatorUid = document.getString("initiatorUid") ?: return null
        val recipientUid = document.getString("recipientUid") ?: return null
        val incoming = recipientUid == currentUid
        val otherUid = if (initiatorUid == currentUid) recipientUid else initiatorUid
        if (otherUid == currentUid) return null

        val displayNameField = if (incoming) "initiatorDisplayName" else "recipientDisplayName"
        val avatarField = if (incoming) "initiatorAvatarId" else "recipientAvatarId"
        return CircleSparkPreview(
            edgeId = document.id,
            member = CircleMemberPreview(
                uid = otherUid,
                displayName = PublicProfileRules.sanitizeDisplayName(
                    document.getString(displayNameField) ?: "Radiant Rookie",
                ),
                avatarId = PublicProfileRules.normalizeAvatarId(document.getString(avatarField)),
            ),
            incoming = incoming,
            status = document.getString("status") ?: CIRCLE_STATUS_PENDING,
        )
    }

    private fun currentCircleSession(): FirebaseSession? {
        val app = ensureFirebaseApp() ?: return null
        val uid = FirebaseAuth.getInstance(app).currentUser?.uid ?: return null
        return FirebaseSession(app, uid)
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

    private fun dailyRadianceState(
        uid: String,
        userSnapshot: DocumentSnapshot,
        today: String,
    ): DailyRadiancePreview {
        val lastOpenedDay = userSnapshot.getString("dailyRadianceLastOpenedDate")
        val revealedToday = lastOpenedDay == today
        val savedMessage = if (revealedToday) {
            DailyRadianceRules.contentById(userSnapshot.getString("dailyRadianceMessageId"))
        } else {
            null
        }
        val content = savedMessage ?: DailyRadianceRules.contentFor(
            accountId = uid,
            dayKey = today,
        )
        val savedStreak = (userSnapshot.getLong("dailyRadianceCurrentStreak") ?: 0L).toInt()
        val visibleStreak = if (revealedToday) {
            savedStreak.coerceAtLeast(1)
        } else {
            DailyRadianceRules.visibleStreak(
                lastOpenedDay = lastOpenedDay,
                todayKey = today,
                savedStreak = savedStreak,
            )
        }

        return DailyRadiancePreview(
            dayKey = today,
            messageId = content.id,
            category = content.category,
            message = content.message,
            revealedToday = revealedToday,
            currentStreak = visibleStreak,
            longestStreak = (userSnapshot.getLong("dailyRadianceLongestStreak") ?: 0L).toInt(),
            opening = false,
        )
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

    private fun circleFriendlyFailure(action: String, error: Throwable): String = when (error) {
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Circle is having trouble connecting. Check your connection and try again."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Circle couldn't $action right now. Refresh and try again."
            else -> "Circle couldn't $action right now. Please try again."
        }
        else -> "Circle couldn't $action right now. Please try again."
    }

    private fun safeMessage(error: Throwable): String = when (error) {
        is FirebaseFirestoreException -> "${error.code}: ${error.message ?: "Firestore error"}"
        else -> error.message ?: error::class.java.simpleName
    }

    private data class CupSponsorPresentation(
        val sponsorName: String? = null,
        val prizeLabel: String? = null,
        val sponsorStatus: String = "No sponsored prize this week",
        val sponsorNote: String? = null,
        val sponsorActive: Boolean = false,
        val trustedConfig: Boolean = false,
        val cupStatusCode: String = "UNCONFIGURED",
        val cupStatusLabel: String = "No trusted Cup config",
        val startsAtEpochMillis: Long? = null,
        val endsAtEpochMillis: Long? = null,
        val fundingStatus: String = Phase12WeeklyCupConfigRules.FUNDING_NOT_CONFIGURED,
        val fundingLabel: String = "Funding wallet not configured",
        val placementAllocationLabel: String? = null,
        val trustedResultsRequired: Boolean = true,
    )

    private data class RadiantRunCommitOutcome(
        val reward: RadiantGameRules.RunReward,
        val mode: RunCompetitionMode,
        val xpAward: GameplayXpAward,
        val runRecord: RunScoreRecord,
    )

    private data class FirebaseSession(
        val app: FirebaseApp,
        val uid: String,
    )

    private class DuplicateQuestException : RuntimeException("Quest already completed today.")

    private companion object {
        const val TAG = "FirebaseRadiantRepo"
        const val USERS = "users"
        const val COMPLETED_QUESTS = "completedQuests"
        const val LEADERBOARD = "leaderboard"
        const val RUN_WEEKLY = "runWeekly"
        const val RUN_ENTRIES = "entries"
        const val RUN_ALL_TIME = "runAllTime"
        const val RUN_WALLET_DAILY = "runWalletDaily"
        const val RUN_WALLETS = "wallets"
        const val COMPETITION_RUN_SUBMISSIONS = "competitionRunSubmissions"
        const val WEEKLY_CUP_CONFIGS = "weeklyCupConfigs"
        const val WEEKLY_CUP_RESULTS = "weeklyCupResults"
        const val WEEKLY_CUP_WINNERS = "winners"
        const val CIRCLE_DISCOVERY = "circleDiscovery"
        const val CIRCLE_PROFILES = "circleProfiles"
        const val CIRCLE_EDGES = "circleEdges"
        const val CIRCLE_STATUS_PENDING = "PENDING"
        const val CIRCLE_STATUS_ACCEPTED = "ACCEPTED"
        const val CIRCLE_STATUS_IGNORED = "IGNORED"
        const val SIGNED_PROOF_XP = 75
        const val ON_CHAIN_PROOF_XP = 100
        const val SKR_SCAN_XP = 50
    }
}
