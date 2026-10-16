package it.togo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import it.togo.app.presentation.activelist.ActiveListScreen
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.presentation.rules.LearnedRulesScreen
import it.togo.app.presentation.rules.LearnedRulesViewModel
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.isSystemDarkTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val viewModel: ActiveListViewModel by inject()
    private val voiceViewModel: VoiceBottomSheetViewModel by inject()
    private val learnedRulesViewModel: LearnedRulesViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TogoTheme(darkTheme = isSystemDarkTheme()) {
                AppNavigator(
                    onActiveListEvent = viewModel::onEvent,
                    activeListContent = { onEvent ->
                        ActiveListScreen(
                            viewModel = viewModel,
                            voiceViewModel = voiceViewModel,
                            onEvent = onEvent,
                        )
                    },
                    learnedRulesContent = { onBack ->
                        LearnedRulesScreen(
                            viewModel = learnedRulesViewModel,
                            onBack = onBack,
                        )
                    },
                )
            }
        }
    }
}
