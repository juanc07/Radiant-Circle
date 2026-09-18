package com.thinkblox.radiantrush.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface GoogleAccountCredentialResult {
    data class Success(val idToken: String) : GoogleAccountCredentialResult
    data object Cancelled : GoogleAccountCredentialResult
    data class SetupRequired(val message: String) : GoogleAccountCredentialResult
    data class Failure(val message: String) : GoogleAccountCredentialResult
}

/**
 * Explicit Sign in with Google flow for protecting/restoring a Radiant Circle account.
 *
 * The OAuth client id is supplied by the local google-services.json generated resource.
 * No client id, token, password, private key or wallet secret is hardcoded here.
 */
class GoogleAccountCredentialProvider(
    private val context: Context,
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun requestIdToken(): GoogleAccountCredentialResult {
        val webClientId = resolveWebClientId()
            ?: return GoogleAccountCredentialResult.SetupRequired(
                "Google account recovery needs one Firebase setup step before it can be used.",
            )

        val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request,
            )
            val credential = result.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                try {
                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleAccountCredentialResult.Success(google.idToken)
                } catch (_: GoogleIdTokenParsingException) {
                    GoogleAccountCredentialResult.Failure(
                        "Google sign-in could not be read. Please try again.",
                    )
                }
            } else {
                GoogleAccountCredentialResult.Failure(
                    "Google sign-in did not return an account. Please try again.",
                )
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleAccountCredentialResult.Cancelled
        } catch (_: GetCredentialException) {
            GoogleAccountCredentialResult.Failure(
                "Google sign-in could not open. Please try again.",
            )
        } catch (_: Exception) {
            GoogleAccountCredentialResult.Failure(
                "Google sign-in is unavailable right now. Please try again.",
            )
        }
    }

    private fun resolveWebClientId(): String? {
        val id = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (id == 0) return null
        return context.getString(id).trim().takeIf { it.isNotBlank() }
    }
}
