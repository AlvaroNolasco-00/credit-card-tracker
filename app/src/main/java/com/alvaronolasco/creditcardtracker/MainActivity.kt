package com.alvaronolasco.creditcardtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.alvaronolasco.creditcardtracker.data.repository.UserPreferencesRepository
import com.alvaronolasco.creditcardtracker.notifications.InactivityReminderScheduler
import com.alvaronolasco.creditcardtracker.ui.navigation.Navigation
import com.alvaronolasco.creditcardtracker.ui.theme.CreditCardTrackerTheme
import com.alvaronolasco.creditcardtracker.widget.WidgetDeepLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var inactivityScheduler: InactivityReminderScheduler

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        intent.getIntExtra("card_id", -1).takeIf { it > 0 }?.let {
            WidgetDeepLink.navigate(it)
        }

        val startDestination = if (userPreferencesRepository.isOnboardingCompleted()) "dashboard" else "onboarding"

        setContent {
            CreditCardTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation(startDestination = startDestination)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userPreferencesRepository.updateLastAppOpen()
        inactivityScheduler.schedule()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getIntExtra("card_id", -1).takeIf { it > 0 }?.let {
            WidgetDeepLink.navigate(it)
        }
    }
}
