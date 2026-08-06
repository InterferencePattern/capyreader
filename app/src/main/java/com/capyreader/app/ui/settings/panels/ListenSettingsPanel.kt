package com.capyreader.app.ui.settings.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capyreader.app.R
import com.capyreader.app.common.RowItem
import com.capyreader.app.ui.articles.audio.speech.OpenAISpeechProvider
import com.capyreader.app.ui.articles.audio.speech.SpeechProvider
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
        )
    }
}
