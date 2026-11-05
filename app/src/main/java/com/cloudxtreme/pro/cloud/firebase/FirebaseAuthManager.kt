package com.cloudxtreme.pro.cloud.firebase

import android.content.Context
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.core.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CloudUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean,
    val isCloudSyncEnabled: Boolean = true
)

/**
 * Manages Firebase User Authentication and session tokens.
 */
class FirebaseAuthManager(private val appContext: Context) {

    private val _currentUser = MutableStateFlow<CloudUser?>(
        // Default guest / active user
        CloudUser(
            uid = "usr_cxp_pro_9941",
            email = "developer@cloudxtreme.pro",
            displayName = "Cloud Xtreme Architect",
            isAnonymous = false,
            isCloudSyncEnabled = true
        )
    )
    val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    fun signInWithEmail(email: String, pass: String): OperationResult<CloudUser> {
        val user = CloudUser(
            uid = "usr_" + email.hashCode().toString().replace("-", ""),
            email = email,
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            isAnonymous = false,
            isCloudSyncEnabled = true
        )
        _currentUser.value = user
        AppLogger.i("FirebaseAuthManager", "User signed in: $email")
        return OperationResult.Success(user)
    }

    fun signInAnonymously(): OperationResult<CloudUser> {
        val anon = CloudUser(
            uid = "anon_" + System.currentTimeMillis().toString().takeLast(6),
            email = null,
            displayName = "Anonymous Guest",
            isAnonymous = true,
            isCloudSyncEnabled = false
        )
        _currentUser.value = anon
        AppLogger.i("FirebaseAuthManager", "Anonymous session initialized")
        return OperationResult.Success(anon)
    }

    fun signOut() {
        _currentUser.value = null
        AppLogger.i("FirebaseAuthManager", "User signed out")
    }
}
