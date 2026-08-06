package com.capyreader.app.ui.settings.panels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.capyreader.app.preferences.AppPreferences

class ListenSettingsViewModel(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    var apiKey by mutableStateOf(appPreferences.speechOptions.apiKey.get())
        private set

    var voice by mutableStateOf(appPreferences.speechOptions.voice.get())
        private set

    fun updateApiKey(value: String) {
        appPreferences.speechOptions.apiKey.set(value)

        apiKey = value
    }

    fun updateVoice(value: String) {
        appPreferences.speechOptions.voice.set(value)

        voice = value
    }
}
