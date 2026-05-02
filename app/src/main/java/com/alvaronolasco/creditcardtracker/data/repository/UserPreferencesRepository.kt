package com.alvaronolasco.creditcardtracker.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val prefs: SharedPreferences
) {
    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, null))
    val userName: StateFlow<String?> = _userName.asStateFlow()

    fun saveName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
        _userName.value = name.trim()
    }

    fun getFirstLaunchTimestamp(): Long {
        val stored = prefs.getLong(KEY_FIRST_LAUNCH_AT, 0L)
        if (stored == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_FIRST_LAUNCH_AT, now).apply()
            return now
        }
        return stored
    }

    fun dismissSupportCard() {
        prefs.edit().putLong(KEY_SUPPORT_DISMISSED_AT, System.currentTimeMillis()).apply()
    }

    fun shouldShowSupportCard(): Boolean {
        val firstLaunch = getFirstLaunchTimestamp()
        val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstLaunch)
        if (daysSinceFirstLaunch < 7) return false

        val dismissedAt = prefs.getLong(KEY_SUPPORT_DISMISSED_AT, 0L)
        if (dismissedAt == 0L) return true

        val daysSinceDismiss = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - dismissedAt)
        return daysSinceDismiss >= 30
    }

    fun dismissBudgetPrompt(monthYear: String) {
        prefs.edit().putString(KEY_BUDGET_PROMPT_DISMISSED_MONTH, monthYear).apply()
    }

    fun isBudgetPromptDismissed(monthYear: String): Boolean {
        return prefs.getString(KEY_BUDGET_PROMPT_DISMISSED_MONTH, null) == monthYear
    }

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    fun isInactivityNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_INACTIVITY_NOTIFICATIONS, true)
    }

    fun setInactivityNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INACTIVITY_NOTIFICATIONS, enabled).apply()
    }

    fun updateLastAppOpen() {
        prefs.edit().putLong(KEY_LAST_APP_OPEN, System.currentTimeMillis()).apply()
    }

    fun getLastAppOpen(): Long {
        return prefs.getLong(KEY_LAST_APP_OPEN, System.currentTimeMillis())
    }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_LAUNCH_AT = "first_launch_at"
        private const val KEY_SUPPORT_DISMISSED_AT = "support_dismissed_at"
        private const val KEY_BUDGET_PROMPT_DISMISSED_MONTH = "budget_prompt_dismissed_month"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_INACTIVITY_NOTIFICATIONS = "inactivity_notifications_enabled"
        private const val KEY_LAST_APP_OPEN = "last_app_open"
        private const val KEY_LAST_SYNCED_UID = "last_synced_uid"
    }

    fun getLastSyncedUid(): String? = prefs.getString(KEY_LAST_SYNCED_UID, null)
    fun setLastSyncedUid(uid: String?) = prefs.edit().putString(KEY_LAST_SYNCED_UID, uid).apply()
}
