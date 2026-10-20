package com.nabdaldhikr.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore
    
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
    private val VIBRATION_KEY = booleanPreferencesKey("vibration")
    private val REMINDER_KEY = booleanPreferencesKey("reminder")
    
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(false)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            _isDarkMode.value = preferences[DARK_MODE_KEY] ?: true
            _fontScale.value = preferences[FONT_SCALE_KEY] ?: 1.0f
            _vibrationEnabled.value = preferences[VIBRATION_KEY] ?: true
            _reminderEnabled.value = preferences[REMINDER_KEY] ?: false
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DARK_MODE_KEY] = enabled
            }
        }
    }

    fun setFontScale(scale: Float) {
        val coercedScale = scale.coerceIn(0.8f, 1.6f)
        _fontScale.value = coercedScale
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[FONT_SCALE_KEY] = coercedScale
            }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[VIBRATION_KEY] = enabled
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        _reminderEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[REMINDER_KEY] = enabled
            }
        }
    }
}
