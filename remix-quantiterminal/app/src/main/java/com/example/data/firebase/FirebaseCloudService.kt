package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class CloudSessionData(
    val vmId: String = "",
    val currentDirectory: String = "/home/clouduser",
    val envVars: Map<String, String> = emptyMap(),
    val isBypassTargeting: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val savedByEmail: String = ""
)

class FirebaseCloudService(private val context: Context) {

    private val TAG = "FirebaseCloudService"

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth initialization fallback: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization fallback: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        // Observe auth state changes
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    _currentUser.value = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "developer@cloudterm.internal",
                        displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Cloud Developer",
                        photoUrl = user.photoUrl?.toString(),
                        isAnonymous = user.isAnonymous
                    )
                } else {
                    _currentUser.value = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach auth listener: ${e.message}")
        }
    }

    suspend fun signInWithGoogleCredential(
        context: Context,
        webClientId: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val credentialManager = CredentialManager.create(context)
        
        try {
            val clientId = webClientId?.takeIf { it.isNotBlank() }
                ?: "113848550377-cloudterm.apps.googleusercontent.com" // Default client fallback

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetSignInWithGoogleOption.Builder(clientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                val authInstance = auth
                if (authInstance != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = authInstance.signInWithCredential(firebaseCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val profile = UserProfile(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: googleIdTokenCredential.id,
                            displayName = firebaseUser.displayName ?: googleIdTokenCredential.displayName ?: "Cloud User",
                            photoUrl = firebaseUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString()
                        )
                        _currentUser.value = profile
                        return@withContext Result.success(profile)
                    }
                } else {
                    // Fallback profile if auth instance is not provisioned
                    val profile = UserProfile(
                        uid = googleIdTokenCredential.id,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "Cloud User",
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                    _currentUser.value = profile
                    return@withContext Result.success(profile)
                }
            }

            return@withContext Result.failure(Exception("Unsupported credential type returned."))
        } catch (e: GetCredentialCancellationException) {
            return@withContext Result.failure(Exception("Sign in cancelled by user."))
        } catch (e: Exception) {
            Log.w(TAG, "Google Sign-In failed or fallback needed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Sign in as a demo/dev user for testing or offline environment when Google services are unavailable
     */
    fun signInDevProfile(email: String = "universallove322@gmail.com", displayName: String = "Universal Love") {
        val devProfile = UserProfile(
            uid = "cloudterm_user_${Math.abs(email.hashCode())}",
            email = email,
            displayName = displayName,
            photoUrl = null
        )
        _currentUser.value = devProfile
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth?.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Sign out exception: ${e.message}")
        }
        _currentUser.value = null
    }

    suspend fun saveSessionToFirestore(
        vmId: String,
        workingDirectory: String,
        envVars: Map<String, String>,
        isBypass: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext false
        val db = firestore ?: return@withContext false

        try {
            val sessionData = hashMapOf(
                "vmId" to vmId,
                "currentDirectory" to workingDirectory,
                "envVars" to envVars,
                "isBypassTargeting" to isBypass,
                "lastUpdated" to System.currentTimeMillis(),
                "savedByEmail" to user.email
            )

            db.collection("users")
                .document(user.uid)
                .collection("sessions")
                .document(vmId)
                .set(sessionData, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced session to Firestore for VM: $vmId, dir: $workingDirectory")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session to Firestore: ${e.message}")
            false
        }
    }

    suspend fun loadSessionFromFirestore(
        vmId: String
    ): CloudSessionData? = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext null
        val db = firestore ?: return@withContext null

        try {
            val doc = db.collection("users")
                .document(user.uid)
                .collection("sessions")
                .document(vmId)
                .get()
                .await()

            if (doc.exists()) {
                val dir = doc.getString("currentDirectory") ?: "/home/clouduser"
                @Suppress("UNCHECKED_CAST")
                val envs = (doc.get("envVars") as? Map<String, String>) ?: emptyMap()
                val isBypass = doc.getBoolean("isBypassTargeting") ?: true
                val lastUp = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                val email = doc.getString("savedByEmail") ?: user.email

                CloudSessionData(
                    vmId = vmId,
                    currentDirectory = dir,
                    envVars = envs,
                    isBypassTargeting = isBypass,
                    lastUpdated = lastUp,
                    savedByEmail = email
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading session from Firestore: ${e.message}")
            null
        }
    }
}
