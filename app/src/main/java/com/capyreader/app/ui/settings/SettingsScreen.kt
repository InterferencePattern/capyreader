package com.capyreader.app.ui.settings

import androidx.compose.runtime.Composable
import com.capyreader.app.ui.settings.panels.SettingsPanel

@Composable
fun SettingsScreen(
    initialPanel: SettingsPanel? = null,
    onRemoveAccount: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    SettingsView(
        initialPanel = initialPanel,
        onNavigateBack = onNavigateBack,
        onRemoveAccount = onRemoveAccount,
    )
}
