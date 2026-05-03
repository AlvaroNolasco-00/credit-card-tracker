package com.alvaronolasco.creditcardtracker.data.repository

import android.util.Log
import com.alvaronolasco.creditcardtracker.data.firestore.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun pushUserProfile(uid: String, email: String?, isAnonymous: Boolean) {
        val profileData = mapOf(
            "uid" to uid,
            "email" to email,
            "isAnonymous" to isAnonymous,
            "lastLoginAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).set(profileData).await()
    }

    suspend fun pushEntity(collection: String, docId: String, data: Map<String, Any?>) {
        firestore.collection(collection).document(docId).set(data).await()
    }

    suspend fun deleteEntity(collection: String, docId: String) {
        firestore.collection(collection).document(docId).delete().await()
    }

    suspend fun pullCollection(collection: String, uid: String): List<Map<String, Any?>> {
        return try {
            firestore.collection(collection)
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.data }
        } catch (e: Exception) {
            Log.w("FirestoreSyncRepo", "pullCollection($collection) failed", e)
            emptyList()
        }
    }

    suspend fun pullOldSubcollection(uid: String, sub: String): List<Map<String, Any?>> {
        return try {
            firestore.collection("users").document(uid).collection(sub)
                .get().await()
                .documents
                .mapNotNull { it.data }
        } catch (e: Exception) {
            Log.w("FirestoreSyncRepo", "pullOldSubcollection($sub) failed", e)
            emptyList()
        }
    }
}
