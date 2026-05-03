package com.alvaronolasco.creditcardtracker.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.alvaronolasco.creditcardtracker.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(
        val uid: String,
        val email: String?,
        val isAnonymous: Boolean
    ) : AuthState()
    data object Unauthenticated : AuthState()
}

sealed class AuthError {
    data object InvalidEmail : AuthError()
    data object WeakPassword : AuthError()
    data object EmailInUse : AuthError()
    data object UserNotFound : AuthError()
    data object WrongPassword : AuthError()
    data object NetworkError : AuthError()
    data object TooManyRequests : AuthError()
    data object UserDisabled : AuthError()
    data object EmailAlreadyLinked : AuthError()
    data object GoogleSignInCancelled : AuthError()
    data class Unknown(val message: String?) : AuthError()
}

class AuthException(val error: AuthError) : Exception()

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val credentialManager: CredentialManager
) {
    val authState: Flow<AuthState> = callbackFlow {
        trySend(AuthState.Loading)
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(AuthState.Unauthenticated)
            } else {
                trySend(AuthState.Authenticated(user.uid, user.email, user.isAnonymous))
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUid(): String? = auth.currentUser?.uid
    fun currentUser() = auth.currentUser
    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: true

    suspend fun ensureSignedIn(): Result<Unit> {
        if (auth.currentUser != null) return Result.success(Unit)
        return runCatching {
            auth.signInAnonymously().await()
            Unit
        }.onFailure { Log.e("AuthRepository", "Anonymous sign-in failed", it) }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val user = auth.currentUser
        if (user?.isAnonymous == true) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(credential).await()
        } else {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
        Unit
    }.mapError()

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }.mapError()

    suspend fun signInWithGoogle(context: Context): Result<Unit> = runCatching {
        val serverClientId = context.getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleIdTokenCredential.idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        val user = auth.currentUser
        if (user?.isAnonymous == true) {
            user.linkWithCredential(firebaseCredential).await()
        } else {
            auth.signInWithCredential(firebaseCredential).await()
        }
        Unit
    }.mapError()

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }.mapError()

    suspend fun signOut() {
        auth.signOut()
        runCatching { auth.signInAnonymously().await() }
    }

    private fun <T> Result<T>.mapError(): Result<T> = this.recoverCatching { e ->
        when (e) {
            is GetCredentialException -> throw AuthException(AuthError.GoogleSignInCancelled)
            is FirebaseAuthException -> throw AuthException(mapFirebaseError(e.errorCode))
            else -> throw AuthException(AuthError.Unknown(e.message))
        }
    }

    private fun mapFirebaseError(errorCode: String): AuthError = when (errorCode) {
        "ERROR_INVALID_EMAIL" -> AuthError.InvalidEmail
        "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword
        "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailInUse
        "ERROR_USER_NOT_FOUND" -> AuthError.UserNotFound
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> AuthError.WrongPassword
        "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NetworkError
        "ERROR_TOO_MANY_REQUESTS" -> AuthError.TooManyRequests
        "ERROR_USER_DISABLED" -> AuthError.UserDisabled
        "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_PROVIDER_ALREADY_LINKED" -> AuthError.EmailAlreadyLinked
        else -> AuthError.Unknown(errorCode)
    }
}
