package com.alvaronolasco.creditcardtracker

import android.app.Application
import com.alvaronolasco.creditcardtracker.data.SyncManager
import com.alvaronolasco.creditcardtracker.data.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CreditCardTrackerApp : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { authRepository.ensureSignedIn() }
        syncManager.start()
    }
}
