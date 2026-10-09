package it.togo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import it.togo.app.presentation.activelist.ActiveListScreen
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.presentation.activelist.UiEvent
import it.togo.app.ui.theme.TogoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ActiveListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TogoTheme {
                ActiveListScreen(onEvent = viewModel::onEvent)
            }
        }
    }
}