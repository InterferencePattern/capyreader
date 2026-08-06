package com.capyreader.app.ui.settings.panels

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capyreader.app.R
import com.capyreader.app.common.RowItem
import com.capyreader.app.ui.articles.audio.speech.OpenAISpeechProvider
import com.capyreader.app.ui.articles.audio.speech.SpeechProvider
import com.capyreader.app.ui.articles.audio.speech.SpeechVoice
import com.capyreader.app.ui.components.FormSection
import com.capyreader.app.ui.settings.PreferenceSelect
import com.capyreader.app.ui.theme.CapyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ListenSettingsPanel(
    viewModel: ListenSettingsViewModel = koinViewModel(),
) {
    ListenSettingsPanelView(
        provider = viewModel.provider,
        updateProvider = viewModel::updateProvider,
        apiKey = viewModel.apiKey,
        updateApiKey = viewModel::updateApiKey,
        voice = viewModel.voice,
        updateVoice = viewModel::updateVoice,
        baseUrl = viewModel.baseUrl,
        updateBaseUrl = viewModel::updateBaseUrl,
        voiceList = viewModel.voiceList,
        loadVoices = viewModel::loadVoices,
        playSample = viewModel::playSample,
        sampleError = viewModel.sampleError,
    )
}

@Composable
fun ListenSettingsPanelView(
    provider: SpeechProvider,
    updateProvider: (SpeechProvider) -> Unit,
    apiKey: String,
    updateApiKey: (String) -> Unit,
    voice: String,
    updateVoice: (String) -> Unit,
    baseUrl: String,
    updateBaseUrl: (String) -> Unit,
    voiceList: VoiceListState,
    loadVoices: () -> Unit,
    playSample: (SpeechVoice) -> Unit,
    @StringRes sampleError: Int?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        FormSection(title = stringResource(R.string.settings_section_listen_speech)) {
            PreferenceSelect(
                selected = provider,
                update = updateProvider,
                options = SpeechProvider.all,
                optionText = { stringResource(it.title) },
                label = R.string.settings_listen_provider_label,
            )
            if (provider.usesBaseUrl) {
                RowItem {
                    TextField(
                        value = baseUrl,
                        onValueChange = updateBaseUrl,
                        singleLine = true,
                        label = {
                            Text(stringResource(R.string.settings_listen_base_url_label))
                        },
                        placeholder = {
                            Text(stringResource(R.string.settings_listen_base_url_hint))
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Uri,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            RowItem {
                TextField(
                    value = apiKey,
                    onValueChange = updateApiKey,
                    singleLine = true,
                    label = {
                        Text(stringResource(provider.apiKeyLabel))
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Password,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            RowItem {
                TextField(
                    value = voice,
                    onValueChange = updateVoice,
                    singleLine = true,
                    label = {
                        Text(stringResource(provider.voiceLabel))
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // The field above stays whatever this section does: a provider that cannot enumerate,
            // a list that will not load, and a reader who already knows their identifier all need
            // somewhere to type it.
            if (provider.listsVoices) {
                VoiceList(
                    state = voiceList,
                    hasCredentials = apiKey.isNotBlank(),
                    selectedVoice = voice,
                    selectVoice = updateVoice,
                    loadVoices = loadVoices,
                    playSample = playSample,
                    sampleError = sampleError,
                )
            }
            RowItem {
                Text(
                    // Named by address when the reader chose the address: "an OpenAI-compatible
                    // server" would not tell them where their article text is going.
                    text = stringResource(
                        R.string.settings_listen_disclosure,
                        if (provider.usesBaseUrl) baseUrl.ifBlank { stringResource(provider.title) }
                        else stringResource(provider.title),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Four states the reader can tell apart: no key to list with, listing, nothing to list, and a
 * listing that failed. Each is a sentence rather than a spinner that stops, since the fix differs.
 */
@Composable
private fun VoiceList(
    state: VoiceListState,
    hasCredentials: Boolean,
    selectedVoice: String,
    selectVoice: (String) -> Unit,
    loadVoices: () -> Unit,
    playSample: (SpeechVoice) -> Unit,
    @StringRes sampleError: Int?,
) {
    RowItem {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!hasCredentials) {
                Hint(stringResource(R.string.settings_listen_voices_need_key))
                return@Column
            }

            when (state) {
                is VoiceListState.Idle -> Unit
                is VoiceListState.Loading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Hint(stringResource(R.string.settings_listen_voices_loading))
                }

                is VoiceListState.Empty -> Hint(stringResource(R.string.settings_listen_voices_empty))
                is VoiceListState.Failed -> Hint(stringResource(R.string.settings_listen_voices_failed))
                is VoiceListState.Loaded -> {
                    state.voices.forEach { voice ->
                        VoiceRow(
                            voice = voice,
                            selected = voice.id == selectedVoice,
                            select = { selectVoice(voice.id) },
                            playSample = { playSample(voice) },
                        )
                    }

                    // Said once, where the buttons are: every tap of one is a synthesis request
                    // the reader pays for.
                    Hint(stringResource(R.string.settings_listen_sample_cost))
                }
            }

            if (state !is VoiceListState.Loading) {
                TextButton(onClick = loadVoices) {
                    Text(
                        stringResource(
                            if (state is VoiceListState.Idle) R.string.settings_listen_voices_load
                            else R.string.settings_listen_voices_reload
                        )
                    )
                }
            }

            sampleError?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun VoiceRow(
    voice: SpeechVoice,
    selected: Boolean,
    select: () -> Unit,
    playSample: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = select, role = Role.RadioButton),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = voice.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        IconButton(onClick = playSample) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    R.string.settings_listen_voice_sample,
                    voice.name,
                ),
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun ListenSettingsPanelPreview() {
    CapyTheme {
        ListenSettingsPanelView(
            provider = OpenAISpeechProvider,
            updateProvider = {},
            apiKey = "sk-example",
            updateApiKey = {},
            voice = "alloy",
            updateVoice = {},
            baseUrl = "",
            updateBaseUrl = {},
            voiceList = VoiceListState.Loaded(
                listOf(
                    SpeechVoice(id = "alloy", name = "Alloy"),
                    SpeechVoice(id = "sage", name = "Sage"),
                ),
            ),
            loadVoices = {},
            playSample = {},
            sampleError = null,
        )
    }
}
