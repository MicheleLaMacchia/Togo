package it.togo.app.presentation.components

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics

/**
 * Etichetta il pannello del dialog e assegna il focus iniziale al pannello.
 * Dialog e AlertDialog mantengono la propria finestra modale Compose, che
 * confina la navigazione e ripristina il focus al chiamante alla chiusura.
 */
@Composable
fun dialogAccessibilityModifier(title: String): Modifier {
    val focusRequester = remember { FocusRequester() }
    val hasRequestedInitialFocus = remember { mutableStateOf(false) }
    return Modifier
        .focusRequester(focusRequester)
        .focusable()
        .onGloballyPositioned {
            if (!hasRequestedInitialFocus.value) {
                hasRequestedInitialFocus.value = true
                focusRequester.requestFocus()
            }
        }
        .semantics { paneTitle = title }
}
