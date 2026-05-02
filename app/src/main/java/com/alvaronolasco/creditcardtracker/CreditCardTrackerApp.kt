package com.alvaronolasco.creditcardtracker

import android.app.Application
import com.alvaronolasco.creditcardtracker.data.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CreditCardTrackerApp : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        syncManager.start()
    }
}
