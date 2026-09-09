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
 * Phase 3 adds public wallet identity persistence after a real Mobile Wallet
 * Adapter authorization. This class does not sign messages, send transactions,
 * verify SKR, or simulate any on-chain success.
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
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.Error,
                    todayKey = todayKey(),
                    lastMessage = "This quest is locked until its real implementation exists.",
                ),
            )
            return
        }

        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Add app/google-services.json before saving Firebase quest progress.",
                ),
            )
            return
        }

        val auth = FirebaseAuth.getInstance(app)
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            bootstrap(onState)
            return
        }

        onState(
            PreviewContent.defaultState().copy(
                firebaseStatus = FirebaseStatus.Loading,
                todayKey = todayKey(),
                lastMessage = "Saving daily Firebase check-in…",
            ),
        )

        val db = FirebaseFirestore.getInstance(app)
        val today = todayKey()
        val userRef = db.collection(USERS).document(uid)
        val completedRef = userRef.collection(COMPLETED_QUESTS).document("${quest.id}_$today")
        val leaderboardRef = db.collection(LEADERBOARD).document(uid)

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
            val newXp = oldXp + quest.xp
            val newStreak = nextStreak(lastQuestDate, today, oldStreak)
            val newLongestStreak = maxOf(longestStreak, newStreak)
            val newLevel = levelForXp(newXp)
            val displayName = userSnapshot.getString("displayName") ?: "Radiant Rookie"
            val walletAddress = userSnapshot.getString("walletAddress")
            val walletStatus = if (!walletAddress.isNullOrBlank()) "Wallet connected" else "Wallet not connected yet"
            val skrTier = userSnapshot.getString("skrTier") ?: "Visitor"

            transaction.set(
                completedRef,
                mapOf(
                    "questId" to quest.id,
                    "questTitle" to quest.title,
                    "date" to today,
                    "proofType" to "firestore_phase3_progress",
                    "xpEarned" to quest.xp,
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
                    "walletStatus" to walletStatus,
                    "skrTier" to skrTier,
                    "xp" to newXp,
                    "level" to newLevel,
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongestStreak,
                    "lastQuestDate" to today,
                    "phase" to 3,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                leaderboardRef,
                mapOf(
                    "displayName" to displayName,
                    "walletAddressShort" to shortenAddress(walletAddress),
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
                loadOrCreateProfile(uid, onState, "Daily Firebase check-in saved. Wallet connection is available through Mobile Wallet Adapter.")
            }
            .addOnFailureListener { error ->
                val message = if (error is DuplicateQuestException) {
                    "Daily Firebase check-in already completed for today."
                } else {
                    "Could not save check-in: ${safeMessage(error)}"
                }
                loadOrCreateProfile(uid, onState, message)
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

        val app = ensureFirebaseApp()
        if (app == null) {
            onState(
                PreviewContent.defaultState().copy(
                    firebaseStatus = FirebaseStatus.NotConfigured,
                    todayKey = todayKey(),
                    lastMessage = "Wallet authorized, but Firebase is not configured so the public wallet address was not saved.",
                ),
            )
            return
        }

        val auth = FirebaseAuth.getInstance(app)
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            bootstrap(onState)
            return
        }

        onState(
            PreviewContent.defaultState().copy(
                firebaseStatus = FirebaseStatus.Loading,
                todayKey = todayKey(),
                lastMessage = "Saving wallet identity to Firebase…",
            ),
        )

        val db = FirebaseFirestore.getInstance(app)
        val userRef = db.collection(USERS).document(uid)
        val walletProofRef = userRef.collection(COMPLETED_QUESTS).document(QuestIds.WALLET_CONNECT)
        val leaderboardRef = db.collection(LEADERBOARD).document(uid)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val displayName = userSnapshot.getString("displayName") ?: accountLabel?.takeIf { it.isNotBlank() } ?: "Radiant Rookie"
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
                    "phase" to 3,
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
                loadOrCreateProfile(uid, onState, "Wallet connected and public address saved to Firebase.")
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(uid, onState, "Could not save wallet connection: ${safeMessage(error)}")
            }
    }

    fun clearWalletConnection(onState: (RushUiState) -> Unit) {
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

        val auth = FirebaseAuth.getInstance(app)
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            bootstrap(onState)
            return
        }

        val db = FirebaseFirestore.getInstance(app)
        val userRef = db.collection(USERS).document(uid)
        val leaderboardRef = db.collection(LEADERBOARD).document(uid)

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
                loadOrCreateProfile(uid, onState, "Wallet disconnected locally and Firebase profile updated.")
            }
            .addOnFailureListener { error ->
                loadOrCreateProfile(uid, onState, "Could not clear wallet connection: ${safeMessage(error)}")
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
                        "phase" to 3,
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
                    .toSet()

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
        BadgePreview("On-Chain Spark", "Submit the first memo proof transaction.", unlocked = false),
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

    private fun errorState(message: String): RushUiState = PreviewContent.defaultState().copy(
        firebaseStatus = FirebaseStatus.Error,
        todayKey = todayKey(),
        lastMessage = message,
    )

    private fun safeMessage(error: Throwable): String = when (error) {
        is FirebaseFirestoreException -> "${error.code}: ${error.message ?: "Firestore error"}"
        else -> error.message ?: error::class.java.simpleName
    }

    private class DuplicateQuestException : RuntimeException("Quest already completed today.")

    private companion object {
        const val USERS = "users"
        const val COMPLETED_QUESTS = "completedQuests"
        const val LEADERBOARD = "leaderboard"
    }
}
