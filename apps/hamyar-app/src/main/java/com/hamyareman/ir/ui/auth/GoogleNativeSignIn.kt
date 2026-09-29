package com.hamyareman.ir.ui.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Google account picker → OIDC ID token. The token is immediately exchanged with Appwrite. */
object GoogleNativeSignIn {
    suspend fun idToken(activity: ComponentActivity, webClientId: String): String {
        require(webClientId.isNotBlank()) { "شناسهٔ Web Client گوگل در این نسخه تنظیم نشده است." }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val credential = CredentialManager.create(activity)
            .getCredential(context = activity, request = request)
            .credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) { "پاسخ حساب گوگل معتبر نبود." }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
