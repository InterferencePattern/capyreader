package com.capyreader.app.ui.settings.panels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.articles.audio.speech.SpeechProvider

class ListenSettingsViewModel(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    var provider by mutableStateOf(SpeechProvider.from(appPreferences.speechOptions.provider.get()))
        private set

    var apiKey by mutableStateOf(appPreferences.speechOptions.getApiKey(provider).get())
        private set

    var voice by mutableStateOf(appPreferences.speechOptions.getVoice(provider).get())
        private set

    var baseUrl by mutableStateOf(appPreferences.speechOptions.getBaseUrl(provider).get())
        private set

    fun updateProvider(value: SpeechProvider) {
        appPreferences.speechOptions.provider.set(value.id)

        provider = value

        // Credentials are stored per provider, so the fields swap to the new provider's own
        // rather than carrying over a key it would only reject.
        apiKey = appPreferences.speechOptions.getApiKey(value).get()
        voice = appPreferences.speechOptions.getVoice(value).get()
        baseUrl = appPreferences.speechOptions.getBaseUrl(value).get()
    }

    fun updateApiKey(value: String) {
        appPreferences.speechOptions.getApiKey(provider).set(value)

        apiKey = value
    }

    fun updateVoice(value: String) {
        appPreferences.speechOptions.getVoice(provider).set(value)

        voice = value
    }

    fun updateBaseUrl(value: String) {
        appPreferences.speechOptions.getBaseUrl(provider).set(value)

        baseUrl = value
    }
}
