package com.alvaronolasco.creditcardtracker.data.firestore

import android.util.Log
import com.alvaronolasco.creditcardtracker.data.repository.AuthRepository
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationHelper @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val prefs: UserPreferencesRepository,
    private val authRepository: AuthRepository
) {
    private val subcollections = listOf("cards", "expenses", "categories")

    suspend fun migrateIfNeeded() {
        val uid = authRepository.currentUid() ?: return
        if (prefs.getOldSubcollectionMigrated()) return

        Log.d("MigrationHelper", "Checking old subcollections for uid=$uid")
        var migrated = false

        for (sub in subcollections) {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection(sub)
                .get()
                .await()
                .documents

            if (snapshot.isEmpty()) continue

            Log.d("MigrationHelper", "Found ${snapshot.size} docs in users/$uid/$sub, migrating...")

            for (doc in snapshot) {
                val data = doc.data?.toMutableMap() ?: continue
                data["userId"] = uid
                if (!data.containsKey("updatedAt")) {
                    data["updatedAt"] = data["createdAt"] ?: System.currentTimeMillis()
                }
                val newDocId = "${uid}_${data["id"]}"
                firestore.collection(sub).document(newDocId).set(data).await()
            }
            migrated = true
        }

        if (migrated) {
            Log.d("MigrationHelper", "Old subcollection data migrated to root collections")
        }

        prefs.setOldSubcollectionMigrated(true)
    }
}
