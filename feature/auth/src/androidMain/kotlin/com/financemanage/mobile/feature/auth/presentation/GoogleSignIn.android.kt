package com.financemanage.mobile.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

@Composable
actual fun rememberGoogleIdTokenRequest(webClientId: String?): (suspend () -> String?)? {
    val context = LocalContext.current
    if (webClientId.isNullOrBlank()) return null
    return remember(context, webClientId) {
        suspend {
            val option =
                GetGoogleIdOption
                    .Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            try {
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } else {
                    null
                }
            } catch (e: GetCredentialException) {
                null
            } catch (e: GoogleIdTokenParsingException) {
                null
            }
        }
    }
}
