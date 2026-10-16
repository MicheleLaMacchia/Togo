package it.togo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import it.togo.app.presentation.activelist.ActiveListScreen
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.presentation.activelist.UiEvent
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.isSystemDarkTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val viewModel: ActiveListViewModel by inject()
    private val voiceViewModel: VoiceBottomSheetViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TogoTheme(darkTheme = isSystemDarkTheme()) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    onEvent = viewModel::onEvent,
                )
            }
        }
    }
}