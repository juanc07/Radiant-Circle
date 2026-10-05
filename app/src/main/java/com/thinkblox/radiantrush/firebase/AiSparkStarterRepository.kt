package com.thinkblox.radiantrush.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * Thin authenticated client for the server-side Radiant AI Spark Starter.
 *
 * The Android app sends only the accepted Circle peer UID. The Cloud Function
 * verifies the relationship, derives Shared Sparks from Firestore server-side,
 * applies rate limiting, and calls OpenAI. No OpenAI credential is shipped in
 * the APK.
 */
class AiSparkStarterRepository(context: Context) {
    private val appContext = context.applicationContext

    data class Result(
        val starter: String? = null,
        val error: String? = null,
    )

    fun generate(peerUid: String, onResult: (Result) -> Unit) {
        val cleanPeerUid = peerUid.trim()
        if (cleanPeerUid.isBlank()) {
            onResult(Result(error = "Choose a Circle connection first."))
            return
        }

        val app = ensureFirebaseApp()
        if (app == null) {
            onResult(Result(error = "Radiant AI needs the configured Firebase build."))
            return
        }

        val currentUid = FirebaseAuth.getInstance(app).currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            onResult(Result(error = "Sign in before using Radiant AI."))
            return
        }
        if (cleanPeerUid == currentUid) {
            onResult(Result(error = "Choose another member in your Circle."))
            return
        }

        FirebaseFunctions.getInstance(app)
            .getHttpsCallable("generateSparkStarter")
            .call(mapOf("peerUid" to cleanPeerUid))
            .addOnSuccessListener { callableResult ->
                val payload = callableResult.data as? Map<*, *>
                val starter = (payload?.get("starter") as? String)
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    .orEmpty()
                if (starter.isBlank()) {
                    onResult(Result(error = "Radiant AI didn't return a starter. Try again."))
                } else {
                    onResult(Result(starter = starter))
                }
            }
            .addOnFailureListener { error ->
                val message = error.message.orEmpty().lowercase()
                val friendly = when {
                    "no shared sparks" in message || "failed-precondition" in message ->
                        "Add matching interests to your Circle profiles first, then try again."
                    "resource-exhausted" in message || "too many" in message ->
                        "You've reached today's AI Spark Starter limit."
                    "unauthenticated" in message ->
                        "Sign in again before using Radiant AI."
                    "permission-denied" in message ->
                        "AI Spark Starter is available only for accepted Circle connections."
                    else -> "Radiant AI couldn't generate a starter right now. Try again."
                }
                onResult(Result(error = friendly))
            }
    }

    private fun ensureFirebaseApp(): FirebaseApp? = try {
        FirebaseApp.getInstance()
    } catch (_: IllegalStateException) {
        try {
            FirebaseApp.initializeApp(appContext)
        } catch (_: Exception) {
            null
        }
    } catch (_: Exception) {
        null
    }
}
