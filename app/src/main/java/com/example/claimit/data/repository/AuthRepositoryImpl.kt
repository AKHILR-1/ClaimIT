package com.example.claimit.data.repository

import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.AccountStatus
import com.example.claimit.domain.model.User
import com.example.claimit.domain.model.UserRole
import com.example.claimit.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
    private val firebaseAuth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null },
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null },
    private val realtimeDatabase: FirebaseDatabase? = try {
        FirebaseDatabase.getInstance("https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app")
    } catch (_: Exception) {
        try { FirebaseDatabase.getInstance() } catch (_: Exception) { null }
    }
) : AuthRepository {

    private val TAG = "AuthRepository"

    private val defaultUser = User(
        id = "user_student_1",
        email = "student@campus.edu",
        displayName = "Alex Student",
        collegeEmail = "student@campus.edu",
        studentId = "12104592",
        organizationId = "lpu_main_campus",
        role = UserRole.STUDENT,
        status = AccountStatus.VERIFIED,
        isEmailVerified = true
    )

    private val userDatabase = mutableMapOf<String, User>(
        "student@campus.edu" to defaultUser,
        "admin@campus.edu" to User(
            id = "user_admin_1",
            email = "admin@campus.edu",
            displayName = "Campus Admin Officer",
            collegeEmail = "admin@campus.edu",
            studentId = "ADM-8802",
            organizationId = "lpu_main_campus",
            role = UserRole.ADMIN,
            status = AccountStatus.VERIFIED,
            isEmailVerified = true
        )
    )

    private val _currentUserState = MutableStateFlow<User?>(null)
    override val currentUserState: StateFlow<User?> = _currentUserState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val fbUser = auth.currentUser
        if (fbUser != null) {
            fetchUserProfile(fbUser)
        } else {
            _currentUserState.value = null
        }
    }

    init {
        val auth = firebaseAuth
        if (auth != null) {
            try {
                auth.addAuthStateListener(authStateListener)
                auth.currentUser?.let { fetchUserProfile(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding auth state listener", e)
            }
        }
    }

    private fun fetchUserProfile(fbUser: FirebaseUser) {
        val email = fbUser.email ?: ""
        val isVerified = fbUser.isEmailVerified

        // Attempt retrieval from Realtime Database first under users/{uid}
        val rtdb = realtimeDatabase
        if (rtdb != null) {
            try {
                rtdb.reference.child("users").child(fbUser.uid).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && snapshot.exists()) {
                            val fullName = snapshot.child("fullName").getValue(String::class.java)
                                ?: snapshot.child("displayName").getValue(String::class.java)
                                ?: fbUser.displayName
                                ?: email.substringBefore("@")
                            val collegeEmail = snapshot.child("collegeEmail").getValue(String::class.java) ?: email
                            val collegeEmailVerified = snapshot.child("collegeEmailVerified").getValue(Boolean::class.java) ?: isVerified
                            val contactEmail = snapshot.child("contactEmail").getValue(String::class.java) ?: email
                            val contactEmailVerified = snapshot.child("contactEmailVerified").getValue(Boolean::class.java) ?: isVerified
                            val studentReg = snapshot.child("registrationNumber").getValue(String::class.java)
                                ?: snapshot.child("studentRegistrationNumber").getValue(String::class.java)
                                ?: snapshot.child("studentId").getValue(String::class.java)
                                ?: ""
                            val roleStr = snapshot.child("role").getValue(String::class.java) ?: UserRole.STUDENT.name
                            val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STUDENT }

                            val user = User(
                                id = fbUser.uid,
                                email = contactEmail,
                                displayName = fullName,
                                collegeEmail = collegeEmail,
                                collegeEmailVerified = collegeEmailVerified,
                                contactEmail = contactEmail,
                                contactEmailVerified = contactEmailVerified,
                                studentId = studentReg,
                                registrationNumber = studentReg,
                                organizationId = "lpu_main_campus",
                                role = role,
                                status = AccountStatus.VERIFIED,
                                isEmailVerified = collegeEmailVerified || isVerified,
                                photoUrl = fbUser.photoUrl?.toString() ?: ""
                            )
                            _currentUserState.value = user
                        } else {
                            fetchFirestoreOrFallback(fbUser)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Realtime Database profile fetch failed: ${e.message}, falling back")
                        fetchFirestoreOrFallback(fbUser)
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Exception during Realtime Database profile fetch: ${e.message}")
                fetchFirestoreOrFallback(fbUser)
            }
        } else {
            fetchFirestoreOrFallback(fbUser)
        }
    }

    private fun fetchFirestoreOrFallback(fbUser: FirebaseUser) {
        val email = fbUser.email ?: ""
        val isVerified = fbUser.isEmailVerified

        val db = firestore
        if (db != null) {
            try {
                db.collection("users").document(fbUser.uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val roleStr = doc.getString("role") ?: UserRole.STUDENT.name
                            val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STUDENT }
                            val user = User(
                                id = fbUser.uid,
                                email = email,
                                displayName = doc.getString("displayName") ?: doc.getString("fullName") ?: fbUser.displayName ?: email.substringBefore("@"),
                                collegeEmail = doc.getString("collegeEmail") ?: email,
                                studentId = doc.getString("studentId") ?: doc.getString("studentRegistrationNumber") ?: "",
                                organizationId = doc.getString("organizationId") ?: "lpu_main_campus",
                                role = role,
                                status = AccountStatus.VERIFIED,
                                isEmailVerified = isVerified,
                                photoUrl = fbUser.photoUrl?.toString() ?: ""
                            )
                            _currentUserState.value = user
                        } else {
                            _currentUserState.value = buildFallbackUser(fbUser)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore user profile fetch failed, using fallback profile: ${e.message}")
                        _currentUserState.value = buildFallbackUser(fbUser)
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Exception during user profile fetch: ${e.message}")
                _currentUserState.value = buildFallbackUser(fbUser)
            }
        } else {
            _currentUserState.value = buildFallbackUser(fbUser)
        }
    }

    private fun buildFallbackUser(fbUser: FirebaseUser): User {
        val email = fbUser.email ?: ""
        return User(
            id = fbUser.uid,
            email = email,
            displayName = fbUser.displayName ?: email.substringBefore("@"),
            collegeEmail = email,
            studentId = "",
            organizationId = "lpu_main_campus",
            role = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.STUDENT,
            status = AccountStatus.VERIFIED,
            isEmailVerified = fbUser.isEmailVerified,
            photoUrl = fbUser.photoUrl?.toString() ?: ""
        )
    }

    override suspend fun register(
        email: String,
        pass: String,
        name: String,
        collegeEmail: String,
        studentId: String,
        role: UserRole
    ): Result<User> = withContext(dispatchers.io) {
        try {
            val cleanEmail = email.trim()
            val cleanCollegeEmail = collegeEmail.trim()

            if (cleanEmail.isBlank() || pass.length < 6) {
                return@withContext Result.failure(Exception("Password must be at least 6 characters long."))
            }
            if (!isValidEmail(cleanEmail) || !isValidEmail(cleanCollegeEmail)) {
                return@withContext Result.failure(Exception("Please provide a valid email address format."))
            }

            if (!isCollegeDomain(cleanCollegeEmail)) {
                return@withContext Result.failure(Exception("Please enter a valid email address."))
            }

            // Client cannot self-assign ADMIN role; default to STUDENT
            val assignedRole = UserRole.STUDENT

            val auth = firebaseAuth
            if (auth != null) {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
                val fbUser = authResult.user ?: return@withContext Result.failure(Exception("Failed to create Firebase user account."))

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim().ifBlank { cleanEmail.substringBefore("@") })
                    .build()
                fbUser.updateProfile(profileUpdates).await()

                try {
                    fbUser.sendEmailVerification().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Email verification link dispatch deferred: ${e.message}")
                }

                // 1. REALTIME DATABASE PERSISTENCE (under users/{uid})
                val rtdbMap = mapOf(
                    "fullName" to name.trim().ifBlank { cleanCollegeEmail.substringBefore("@") },
                    "collegeEmail" to cleanCollegeEmail,
                    "collegeEmailVerified" to fbUser.isEmailVerified,
                    "registrationNumber" to studentId.trim(),
                    "studentRegistrationNumber" to studentId.trim(),
                    "contactEmail" to cleanCollegeEmail,
                    "contactEmailVerified" to fbUser.isEmailVerified,
                    "role" to assignedRole.name,
                    "createdAt" to ServerValue.TIMESTAMP,
                    "updatedAt" to ServerValue.TIMESTAMP
                )

                val rtdb = realtimeDatabase
                if (rtdb != null) {
                    try {
                        rtdb.reference.child("users").child(fbUser.uid).setValue(rtdbMap).await()
                        Log.i(TAG, "Successfully saved user profile in Firebase Realtime Database at users/${fbUser.uid}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save profile in Realtime Database: ${e.message}", e)
                        try {
                            fbUser.delete().await()
                        } catch (delEx: Exception) {
                            Log.w(TAG, "Failed to delete Auth user after RTDB error: ${delEx.message}")
                        }
                        val userErrMsg = if (e.message?.contains("Permission denied", ignoreCase = true) == true) {
                            "Permission denied: Realtime Database security rules prevent saving profile. Please check rules in Firebase Console."
                        } else {
                            "Failed to save user profile in Realtime Database: ${e.localizedMessage}"
                        }
                        return@withContext Result.failure(Exception(userErrMsg))
                    }
                } else {
                    Log.e(TAG, "Realtime Database instance is null during registration.")
                    try {
                        fbUser.delete().await()
                    } catch (_: Exception) {}
                    return@withContext Result.failure(Exception("Failed to initialize Realtime Database. Please check network connection and database configuration."))
                }

                // 2. FIRESTORE PERSISTENCE (Backup/Secondary sync)
                val userMap = mapOf(
                    "uid" to fbUser.uid,
                    "id" to fbUser.uid,
                    "fullName" to name.trim().ifBlank { cleanEmail.substringBefore("@") },
                    "displayName" to name.trim().ifBlank { cleanEmail.substringBefore("@") },
                    "email" to cleanEmail,
                    "collegeEmail" to cleanCollegeEmail,
                    "studentRegistrationNumber" to studentId.trim(),
                    "studentId" to studentId.trim(),
                    "organizationId" to "lpu_main_campus",
                    "role" to assignedRole.name,
                    "status" to AccountStatus.VERIFIED.name,
                    "emailVerified" to fbUser.isEmailVerified,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                firestore?.let { db ->
                    try {
                        db.collection("users").document(fbUser.uid).set(userMap).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore user profile document creation warning: ${e.message}")
                    }
                }

                val newUser = User(
                    id = fbUser.uid,
                    email = cleanEmail,
                    displayName = name.trim().ifBlank { cleanEmail.substringBefore("@") },
                    collegeEmail = cleanCollegeEmail,
                    studentId = studentId.trim(),
                    organizationId = "lpu_main_campus",
                    role = assignedRole,
                    status = AccountStatus.VERIFIED,
                    isEmailVerified = fbUser.isEmailVerified
                )

                _currentUserState.value = newUser
                Result.success(newUser)
            } else {
                // In-memory fallback for offline / unit test environment
                if (userDatabase.containsKey(cleanEmail)) {
                    return@withContext Result.failure(Exception("An account with this email already exists."))
                }
                val newUser = User(
                    id = "user_${System.currentTimeMillis()}",
                    email = cleanEmail,
                    displayName = name.trim().ifBlank { cleanEmail.substringBefore("@") },
                    collegeEmail = cleanCollegeEmail,
                    studentId = studentId.trim(),
                    organizationId = "lpu_main_campus",
                    role = assignedRole,
                    status = AccountStatus.VERIFIED,
                    isEmailVerified = true
                )
                userDatabase[cleanEmail] = newUser
                _currentUserState.value = newUser
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun login(email: String, pass: String): Result<User> = withContext(dispatchers.io) {
        try {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank() || pass.isBlank()) {
                return@withContext Result.failure(Exception("Email and password cannot be empty."))
            }

            val auth = firebaseAuth
            if (auth != null) {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
                val fbUser = authResult.user ?: return@withContext Result.failure(Exception("Sign in failed."))

                try {
                    fbUser.reload().await()
                } catch (e: Exception) {
                    Log.w(TAG, "User reload failed: ${e.message}")
                }

                val currentUser = try {
                    val rtdb = realtimeDatabase
                    if (rtdb != null) {
                        val snapshot = rtdb.reference.child("users").child(fbUser.uid).get().await()
                        if (snapshot != null && snapshot.exists()) {
                            val fullName = snapshot.child("fullName").getValue(String::class.java)
                                ?: snapshot.child("displayName").getValue(String::class.java)
                                ?: fbUser.displayName
                                ?: cleanEmail.substringBefore("@")
                            val studentReg = snapshot.child("studentRegistrationNumber").getValue(String::class.java)
                                ?: snapshot.child("studentId").getValue(String::class.java)
                                ?: ""
                            val roleStr = snapshot.child("role").getValue(String::class.java) ?: UserRole.STUDENT.name
                            val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STUDENT }

                            User(
                                id = fbUser.uid,
                                email = cleanEmail,
                                displayName = fullName,
                                collegeEmail = cleanEmail,
                                studentId = studentReg,
                                organizationId = "lpu_main_campus",
                                role = role,
                                status = AccountStatus.VERIFIED,
                                isEmailVerified = fbUser.isEmailVerified,
                                photoUrl = fbUser.photoUrl?.toString() ?: ""
                            )
                        } else {
                            buildFallbackUser(fbUser)
                        }
                    } else {
                        buildFallbackUser(fbUser)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch user profile on login: ${e.message}")
                    buildFallbackUser(fbUser)
                }

                _currentUserState.value = currentUser
                Result.success(currentUser)
            } else {
                // In-memory fallback for offline / unit test environment
                val existing = userDatabase[cleanEmail]
                if (existing != null) {
                    _currentUserState.value = existing
                    Result.success(existing)
                } else if (cleanEmail.contains("@")) {
                    val assignedRole = if (cleanEmail.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.STUDENT
                    val newUser = User(
                        id = "user_${System.currentTimeMillis()}",
                        email = cleanEmail,
                        displayName = cleanEmail.substringBefore("@"),
                        collegeEmail = cleanEmail,
                        studentId = "STU-" + cleanEmail.hashCode().toString().takeLast(6),
                        organizationId = "lpu_main_campus",
                        role = assignedRole,
                        status = AccountStatus.VERIFIED,
                        isEmailVerified = true
                    )
                    userDatabase[cleanEmail] = newUser
                    _currentUserState.value = newUser
                    Result.success(newUser)
                } else {
                    Result.failure(Exception("Invalid credentials or account not found."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> = withContext(dispatchers.io) {
        try {
            val auth = firebaseAuth ?: return@withContext Result.success(Unit)
            val fbUser = auth.currentUser ?: return@withContext Result.failure(Exception("No active user logged in."))
            fbUser.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Send email verification failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun checkEmailVerificationStatus(): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val auth = firebaseAuth ?: return@withContext Result.success(true)
            val fbUser = auth.currentUser ?: return@withContext Result.failure(Exception("No active user logged in."))
            try {
                fbUser.reload().await()
            } catch (e: Exception) {
                Log.w(TAG, "Check verification status reload failed: ${e.message}")
            }
            val isVerified = fbUser.isEmailVerified
            if (isVerified) {
                realtimeDatabase?.let { rtdb ->
                    try {
                        val updates = mapOf<String, Any>(
                            "collegeEmailVerified" to true,
                            "contactEmailVerified" to true,
                            "updatedAt" to ServerValue.TIMESTAMP
                        )
                        rtdb.reference.child("users").child(fbUser.uid).updateChildren(updates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update verification status in RTDB: ${e.message}")
                    }
                }
            }
            val current = _currentUserState.value
            if (current != null) {
                _currentUserState.value = current.copy(
                    isEmailVerified = isVerified,
                    collegeEmailVerified = isVerified,
                    contactEmailVerified = isVerified
                )
            }
            Result.success(isVerified)
        } catch (e: Exception) {
            Log.e(TAG, "Check verification status failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun updateContactEmail(newContactEmail: String, currentPass: String): Result<User> = withContext(dispatchers.io) {
        try {
            val cleanNewEmail = newContactEmail.trim()
            if (!isValidEmail(cleanNewEmail)) {
                return@withContext Result.failure(Exception("Please enter a valid contact email format."))
            }

            val auth = firebaseAuth
            if (auth != null) {
                val fbUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated."))
                val currentEmail = fbUser.email ?: ""

                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(currentEmail, currentPass)
                    fbUser.reauthenticate(credential).await()
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Re-authentication failed. Please verify your current password."))
                }

                try {
                    fbUser.verifyBeforeUpdateEmail(cleanNewEmail).await()
                } catch (e: Exception) {
                    Log.w(TAG, "verifyBeforeUpdateEmail exception: ${e.message}")
                }

                val updates = mapOf<String, Any>(
                    "contactEmail" to cleanNewEmail,
                    "contactEmailVerified" to false,
                    "updatedAt" to ServerValue.TIMESTAMP
                )

                realtimeDatabase?.let { rtdb ->
                    try {
                        rtdb.reference.child("users").child(fbUser.uid).updateChildren(updates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Realtime Database contact email update failed: ${e.message}")
                    }
                }

                val current = _currentUserState.value
                val updated = current?.copy(
                    contactEmail = cleanNewEmail,
                    contactEmailVerified = false,
                    updatedAtTimestamp = System.currentTimeMillis()
                ) ?: buildFallbackUser(fbUser)

                _currentUserState.value = updated
                Result.success(updated)
            } else {
                val current = _currentUserState.value ?: return@withContext Result.failure(Exception("User not authenticated"))
                val updated = current.copy(
                    contactEmail = cleanNewEmail,
                    contactEmailVerified = false,
                    updatedAtTimestamp = System.currentTimeMillis()
                )
                userDatabase[current.email] = updated
                _currentUserState.value = updated
                Result.success(updated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update contact email failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun saveFcmToken(deviceId: String, token: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val user = _currentUserState.value ?: return@withContext Result.success(Unit)
            val rtdb = realtimeDatabase ?: return@withContext Result.success(Unit)

            val tokenMap = mapOf(
                "token" to token,
                "userId" to user.id,
                "organizationId" to user.organizationId,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            rtdb.reference.child("fcmTokens").child(user.id).child(deviceId).setValue(tokenMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Save FCM token failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun removeFcmToken(deviceId: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val user = _currentUserState.value ?: return@withContext Result.success(Unit)
            val rtdb = realtimeDatabase ?: return@withContext Result.success(Unit)

            rtdb.reference.child("fcmTokens").child(user.id).child(deviceId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Remove FCM token failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun logout() = withContext(dispatchers.io) {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out exception: ${e.message}")
        }
        _currentUserState.value = null
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val cleanEmail = email.trim()
            if (!isValidEmail(cleanEmail)) {
                return@withContext Result.failure(Exception("Please enter a valid email address."))
            }
            val auth = firebaseAuth
            if (auth != null) {
                auth.sendPasswordResetEmail(cleanEmail).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun updateProfile(displayName: String, studentId: String): Result<User> = withContext(dispatchers.io) {
        try {
            val auth = firebaseAuth
            if (auth != null) {
                val fbUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated."))
                if (displayName.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    fbUser.updateProfile(profileUpdates).await()
                }

                val updates = mutableMapOf<String, Any>()
                if (displayName.isNotBlank()) {
                    updates["displayName"] = displayName
                    updates["fullName"] = displayName
                }
                if (studentId.isNotBlank()) {
                    updates["studentId"] = studentId
                    updates["studentRegistrationNumber"] = studentId
                }

                // Update Realtime Database
                realtimeDatabase?.let { rtdb ->
                    try {
                        rtdb.reference.child("users").child(fbUser.uid).updateChildren(updates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Realtime Database profile update failed: ${e.message}")
                    }
                }

                // Update Firestore
                firestore?.let { db ->
                    try {
                        db.collection("users").document(fbUser.uid).update(updates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore profile update failed: ${e.message}")
                    }
                }

                val current = _currentUserState.value
                val updated = current?.copy(
                    displayName = displayName.ifBlank { current.displayName },
                    studentId = studentId.ifBlank { current.studentId }
                ) ?: buildFallbackUser(fbUser)

                _currentUserState.value = updated
                Result.success(updated)
            } else {
                val current = _currentUserState.value ?: return@withContext Result.failure(Exception("User not authenticated"))
                val updated = current.copy(
                    displayName = displayName.ifBlank { current.displayName },
                    studentId = studentId.ifBlank { current.studentId }
                )
                userDatabase[current.email] = updated
                _currentUserState.value = updated
                Result.success(updated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update profile failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(dispatchers.io) {
        try {
            val auth = firebaseAuth
            if (auth != null) {
                val fbUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated."))
                realtimeDatabase?.let { rtdb ->
                    try {
                        rtdb.reference.child("users").child(fbUser.uid).removeValue().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Realtime Database user deletion failed: ${e.message}")
                    }
                }
                firestore?.let { db ->
                    try {
                        db.collection("users").document(fbUser.uid).delete().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore user document deletion failed: ${e.message}")
                    }
                }
                fbUser.delete().await()
                _currentUserState.value = null
                Result.success(Unit)
            } else {
                val current = _currentUserState.value ?: return@withContext Result.failure(Exception("User not authenticated"))
                userDatabase.remove(current.email)
                _currentUserState.value = null
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete account failed: ${e.message}", e)
            Result.failure(mapAuthException(e))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.indexOf("@") < email.lastIndexOf(".")
    }

    private fun isCollegeDomain(email: String): Boolean {
        return isValidEmail(email)
    }

    private fun mapAuthException(e: Exception): Exception {
        val message = e.localizedMessage ?: ""
        Log.e(TAG, "Mapping exception: ${e.javaClass.simpleName} - $message")

        if (message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            message.contains("OPERATION_NOT_ALLOWED", ignoreCase = true)) {
            return Exception("Firebase Authentication Email/Password provider is disabled in Firebase Console. Please open Firebase Console -> Authentication -> Sign-in method and enable 'Email/Password'.")
        }

        return when (e) {
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> Exception("An account with this email already exists. Please login instead.")
                    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" -> Exception("Invalid email or password. Please try again.")
                    "ERROR_USER_DISABLED" -> Exception("This account has been disabled by campus administration.")
                    "ERROR_WEAK_PASSWORD" -> Exception("The password is too weak. Please use at least 6 characters.")
                    "ERROR_INVALID_EMAIL" -> Exception("The email address format is invalid.")
                    "ERROR_TOO_MANY_REQUESTS" -> Exception("Too many unsuccessful attempts. Please try again later.")
                    "ERROR_OPERATION_NOT_ALLOWED", "CONFIGURATION_NOT_FOUND" -> Exception("Email/Password authentication is disabled in Firebase Console. Please enable Email/Password under Sign-in method in Firebase Console.")
                    else -> Exception(e.localizedMessage ?: "Authentication error occurred.")
                }
            }
            else -> Exception(e.localizedMessage ?: "An error occurred during authentication.")
        }
    }
}
