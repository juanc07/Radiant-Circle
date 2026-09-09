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
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.data.UserPreview
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Firebase Auth + Firestore repository for Radiant Rush.
 *
 * Phase 4 persists real MWA signed-message proofs and devnet memo transaction
 * signatures after wallet approval. It stores public proof data only.
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
            proofType = "firestore_phase4_progress",
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
            val displayName = userSnapshot.getString("displayName")
                ?: accountLabel?.takeIf { it.isNotBlank() }
                ?: "Radiant Rookie"
            val xp = userSnapshot.getLong("xp") ?: 0L
            val level = userSnapshot.getLong("level") ?: levelForXp(xp)
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val longestStreak = userSnapshot.getLong("longestStreak") ?: currentStreak
            val skrTier = userSnapshot.getString("skrTier") ?: "Visitor"

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
                    "phase" to 4,
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
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
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
            val skrTier = userSnapshot.getString("skrTier") ?: "Visitor"

            val proofDocument = mutableMapOf<String, Any?>(
                "questId" to questId,
                "questTitle" to questTitle,
                "date" to today,
                "proofType" to proofType,
                "walletAddress" to savedWalletAddress,
                "walletAddressShort" to shortenAddress(savedWalletAddress),
                "xpEarned" to xpReward,
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
                "phase" to 4,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            userUpdate.putAll(userExtraFields)

            transaction.set(completedRef, proofDocument, SetOptions.merge())
            transaction.set(userRef, userUpdate, SetOptions.merge())
            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
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
                        "skrTier" to "Visitor",
                        "xp" to 0L,
                        "level" to 1L,
                        "currentStreak" to 0L,
                        "longestStreak" to 0L,
                        "phase" to 4,
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

                val walletConnected = !userSnapshot.getString("walletAddress").isNullOrBlank()

                db.collection(LEADERBOARD)
                    .orderBy("xp", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .addOnSuccessListener { leaderboardQuery ->
                        val leaderboard = leaderboardQuery.documents.mapIndexed { index, document ->
                            LeaderboardPreview(
                                rank = index + 1,
                                name = document.getString("displayName") ?: "Radiant Rookie",
                                xp = (document.getLong("xp") ?: 0L).toInt(),
                                streak = (document.getLong("currentStreak") ?: 0L).toInt(),
                                tier = document.getString("skrTier") ?: "Visitor",
                            )
                        }.ifEmpty {
                            listOf(profileToLeaderboardRow(userSnapshot))
                        }

                        val user = profileToUser(userSnapshot)
                        val quests = PreviewContent.quests.map { quest ->
                            when {
                                completedIds.contains(quest.id) -> quest.copy(status = QuestStatus.Completed)
                                quest.id == QuestIds.WALLET_CONNECT && walletConnected -> quest.copy(status = QuestStatus.Completed)
                                quest.id == QuestIds.DAILY_CHECK_IN -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.WALLET_CONNECT -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.SIGN_DAILY_PROOF && walletConnected -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.ON_CHAIN_PROOF && walletConnected -> quest.copy(status = QuestStatus.Ready)
                                quest.id == QuestIds.SIGN_DAILY_PROOF -> quest.copy(status = QuestStatus.Blocked)
                                quest.id == QuestIds.ON_CHAIN_PROOF -> quest.copy(status = QuestStatus.Blocked)
                                else -> quest.copy(status = QuestStatus.Locked)
                            }
                        }
                        val badges = badgeState(user, completedIds)

                        onState(
                            RushUiState(
                                firebaseStatus = FirebaseStatus.Ready,
                                user = user,
                                quests = quests,
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
            skrTier = snapshot.getString("skrTier") ?: "Visitor",
            xp = (snapshot.getLong("xp") ?: 0L).toInt(),
            level = (snapshot.getLong("level") ?: 1L).toInt(),
            currentStreak = (snapshot.getLong("currentStreak") ?: 0L).toInt(),
            lastSignedMessageSignature = snapshot.getString("lastSignedMessageSignature"),
            lastOnChainTxSignature = snapshot.getString("lastOnChainTxSignature"),
            lastOnChainExplorerUrl = snapshot.getString("lastOnChainExplorerUrl"),
        )
    }

    private fun profileToLeaderboardRow(snapshot: DocumentSnapshot): LeaderboardPreview = LeaderboardPreview(
        rank = 1,
        name = snapshot.getString("displayName") ?: "Radiant Rookie",
        xp = (snapshot.getLong("xp") ?: 0L).toInt(),
        streak = (snapshot.getLong("currentStreak") ?: 0L).toInt(),
        tier = snapshot.getString("skrTier") ?: "Visitor",
    )

    private fun badgeState(user: UserPreview, completedIds: Set<String>): List<BadgePreview> = listOf(
        BadgePreview("First Launch", "Open the native Android app shell.", unlocked = true),
        BadgePreview("Cloud Synced", "Create a Firebase profile and save progress.", unlocked = true),
        BadgePreview("Daily Saver", "Save the daily Firebase check-in.", unlocked = completedIds.contains(QuestIds.DAILY_CHECK_IN)),
        BadgePreview("Wallet Ready", "Connect with Mobile Wallet Adapter.", unlocked = user.walletStatus == "Wallet connected"),
        BadgePreview("Daily Proof", "Sign the daily proof message.", unlocked = completedIds.contains(QuestIds.SIGN_DAILY_PROOF)),
        BadgePreview("On-Chain Spark", "Submit the first memo proof transaction.", unlocked = completedIds.contains(QuestIds.ON_CHAIN_PROOF)),
        BadgePreview("SKR Radiant", "Hold SKR and unlock boosted status.", unlocked = user.skrTier != "Visitor"),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = user.currentStreak >= 7),
    )

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
    }
}
