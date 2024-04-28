package io.github.vrcmteam.vrcm.presentation.screens.home.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vrcmteam.vrcm.presentation.compoments.ABottomSheet
import io.github.vrcmteam.vrcm.presentation.configs.LocalConfiguration
import io.github.vrcmteam.vrcm.presentation.configs.locale.LanguageTag
import io.github.vrcmteam.vrcm.presentation.configs.locale.strings
import io.github.vrcmteam.vrcm.presentation.configs.theme.ThemeColor
import io.github.vrcmteam.vrcm.presentation.screens.auth.AuthAnimeScreen
import io.github.vrcmteam.vrcm.presentation.supports.AuthSupporter
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
) {
    ABottomSheet(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 12.dp)
        ) {
            var currentConfiguration by LocalConfiguration.current
            SettingsItem(strings.stettingLanguage){
                LanguageTag.entries.forEach {
                    TextButton(
                        enabled = it.tag != currentConfiguration.languageTag.tag,
                        onClick = {
                            currentConfiguration = currentConfiguration.copy(languageTag = it)
                        }
                    ) {
                        Text(
                            text = it.displayName,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            SettingsItem(strings.stettingThemeMode){
                listOf(null,true,false).forEach{
                    TextButton(
                        enabled = currentConfiguration.isDarkTheme != it,
                        onClick = {
                            currentConfiguration = currentConfiguration.copy(isDarkTheme = it)
                        },

                        ) {
                        Text(
                            text = when (it) {
                                null -> strings.stettingSystemThemeMode
                                true -> strings.stettingDarkThemeMode
                                false -> strings.stettingLightThemeMode
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            val themeColors : List<ThemeColor> = with(currentKoinScope()) { remember(::getAll) }
            SettingsItem(strings.stettingThemeColor){
                themeColors.forEach {
                    TextButton(
                        enabled = it.name != currentConfiguration.themeColor.name,
                        onClick = {
                            currentConfiguration = currentConfiguration.copy(themeColor = it)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = it.colorScheme.primaryContainer,
                            contentColor = it.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = it.name,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            val authSupporter = koinInject<AuthSupporter>()
           val loginOut = LocalNavigator.currentOrThrow.let {
                {
                    onDismissRequest()
                    authSupporter.logout()
                    it replaceAll AuthAnimeScreen(false)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(0.25f))
                TextButton(
                    modifier = Modifier.weight(0.5f),
                    onClick = loginOut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = strings.stettingLogout,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.weight(0.25f))
            }

        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    content: @Composable RowScope.() -> Unit
){
    Surface {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}