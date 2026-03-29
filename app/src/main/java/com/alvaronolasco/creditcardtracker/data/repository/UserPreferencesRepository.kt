package com.alvaronolasco.creditcardtracker.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    companion object {
        private const val KEY_USER_NAME = "user_name"
    }
}
