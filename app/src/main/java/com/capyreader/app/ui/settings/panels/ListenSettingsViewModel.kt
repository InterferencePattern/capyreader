package com.capyreader.app.ui.settings.panels

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.articles.audio.speech.SpeechProvider
import com.capyreader.app.ui.articles.audio.speech.SpeechSettings
import com.capyreader.app.ui.articles.audio.speech.SpeechVoice
import com.capyreader.app.ui.articles.audio.speech.VoiceSampler
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * What the Voice list is currently showing. [Idle] is the resting state and the one the screen
 * opens in: enumeration is never automatic, because a list fetched for a reader who was editing
 * their base URL is a request nobody asked for.
 */
sealed interface VoiceListState {
    data object Idle : VoiceListState
    data object Loading : VoiceListState
    data object Empty : VoiceListState
    data object Failed : VoiceListState
    data class Loaded(val voices: List<SpeechVoice>) : VoiceListState
}

class ListenSettingsViewModel(
    private val appPreferences: AppPreferences,
    private val voiceSampler: VoiceSampler,
    private val httpClient: OkHttpClient,
) : ViewModel() {
    var provider by mutableStateOf(SpeechProvider.from(appPreferences.speechOptions.provider.get()))
        private set

    var apiKey by mutableStateOf(appPreferences.speechOptions.getApiKey(provider).get())
        private set

    var voice by mutableStateOf(appPreferences.speechOptions.getVoice(provider).get())
        private set

    var baseUrl by mutableStateOf(appPreferences.speechOptions.getBaseUrl(provider).get())
        private set

    var voiceList by mutableStateOf<VoiceListState>(VoiceListState.Idle)
        private set

    @get:StringRes
    var sampleError by mutableStateOf<Int?>(null)
        private set

    fun updateProvider(value: SpeechProvider) {
        appPreferences.speechOptions.provider.set(value.id)

        provider = value

        // Credentials are stored per provider, so the fields swap to the new provider's own
        // rather than carrying over a key it would only reject.
        apiKey = appPreferences.speechOptions.getApiKey(value).get()
        voice = appPreferences.speechOptions.getVoice(value).get()
        baseUrl = appPreferences.speechOptions.getBaseUrl(value).get()

        resetVoiceList()
    }

    fun updateApiKey(value: String) {
        appPreferences.speechOptions.getApiKey(provider).set(value)

        apiKey = value

        // The list belongs to the account the old key identified.
        resetVoiceList()
    }

    fun updateVoice(value: String) {
        appPreferences.speechOptions.getVoice(provider).set(value)

        voice = value
    }

    fun updateBaseUrl(value: String) {
        appPreferences.speechOptions.getBaseUrl(provider).set(value)

        baseUrl = value
    }

    fun loadVoices() {
        voiceList = VoiceListState.Loading

        viewModelScope.launch {
            voiceList = try {
                val voices = provider.voices(settings, httpClient)

                if (voices.isEmpty()) {
                    VoiceListState.Empty
                } else {
                    VoiceListState.Loaded(voices)
                }
            } catch (e: Exception) {
                // A rejected key, no network, or a response shape that changed under us are one
                // state to the reader: the list did not load, and the text field still works.
                VoiceListState.Failed
            }
        }
    }

    fun playSample(voice: SpeechVoice) {
        sampleError = null

        viewModelScope.launch {
            sampleError = voiceSampler.play(voice, provider, settings)
        }
    }

    override fun onCleared() {
        voiceSampler.stop()
    }

    private val settings
        get() = SpeechSettings(voice = voice, apiKey = apiKey, baseUrl = baseUrl)

    private fun resetVoiceList() {
        voiceList = VoiceListState.Idle
        sampleError = null
    }
}
