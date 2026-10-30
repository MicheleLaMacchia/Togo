package it.togo.app.presentation.activelist

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import it.togo.app.R
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.presentation.additem.DuplicateResolutionDialog
import it.togo.app.presentation.voice.VoiceBottomSheet
import it.togo.app.presentation.voice.VoiceBottomSheetUiState
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.isSystemDarkTheme
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Test JVM (Robolectric + Compose) per la Dark Mode (Story 6.3).
 *
 * Verifica che i componenti in scope si renderizzino con `TogoTheme(darkTheme = true)`
 * senza crash e che le semantiche della Story 6.1 restino intatte.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DarkModeComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        stopKoin()
    }

    private val activeItem = listItem(
        productId = 1L,
        quantity = 2.0,
        unit = StandardUnit.PIECE,
        isChecked = false,
    )

    private val level1 = TaxonomyLevel.Level1(id = 1L, name = "Latticini", sortOrder = 1)
    private val level2 = TaxonomyLevel.Level2(
        id = 2L,
        name = "Latticini",
        sortOrder = 1,
        level1Id = 1L,
    )

    @Test
    fun `dark active row exposes edit and completion controls`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                ItemRow(
                    item = activeItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini, Formaggi, Mozzarella",
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
            "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, non selezionato. Doppio tocco per modificare."
        ).assertExists().assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Spunta Mozzarella.").assertExists().assertIsOff()
    }
    @Test
    fun `dark category header keeps title and count`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                CategoryHeader(level1 = level1, level2 = level2, itemCount = 3)
            }
        }

        composeTestRule.onNodeWithText("LATTICINI").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun `dark active row keeps accessible completion control`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                ItemRow(
                    item = activeItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini, Formaggi, Mozzarella",
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Spunta Mozzarella.").assertExists()
    }
    @Test
    fun `dark voice fab idle keeps its accessible label`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                VoiceFab(state = VoiceRecognitionState.Idle, onClick = {})
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Aggiungi prodotto con la voce. Pulsante.")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun `voice listening overlay stays readable and cancellable in dark`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                VoiceListeningOverlay(visible = true, onCancel = {})
            }
        }

        composeTestRule.onNodeWithText("Ascolto in corso...").assertExists()
        composeTestRule
            .onNodeWithText("Annulla")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun `duplicate dialog keeps its content and actions in dark`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                DuplicateResolutionDialog(
                    productName = "Latte UHT",
                    currentQuantity = 1.0,
                    newQuantity = 2.0,
                    onSelect = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Prodotto già in lista").assertExists()
        composeTestRule
            .onNodeWithText("Somma quantità (+2.0 → 3.0)")
            .assertExists()
            .assertHasClickAction()
        composeTestRule.onNodeWithText("Annulla").assertExists().assertHasClickAction()
    }

    @Test
    fun `switching light to dark and back keeps ui state consistent`() {
        var darkTheme by mutableStateOf(false)

        composeTestRule.setContent {
            TogoTheme(darkTheme = darkTheme) {
                ItemRow(
                    item = activeItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini",
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }

        val description = "Mozzarella, 2 pezzi, Latticini, non selezionato. " +
            "Doppio tocco per modificare."
        composeTestRule.onNodeWithContentDescription(description).assertExists()

        composeTestRule.runOnIdle { darkTheme = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(description).assertExists()
        composeTestRule.onNodeWithContentDescription("Spunta Mozzarella.").assertExists()

        composeTestRule.runOnIdle { darkTheme = false }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(description).assertExists()
        composeTestRule.onNodeWithContentDescription("Spunta Mozzarella.").assertExists()
    }

    @Test
    @Config(qualifiers = "night")
    fun `night system qualifier enables dark theme`() {
        RuntimeEnvironment.setQualifiers("+night")

        composeTestRule.setContent {
            TogoTheme(darkTheme = isSystemDarkTheme()) {
                Text(
                    text = togoColors().surfaceBase.toArgb().toString(),
                    modifier = Modifier.testTag("surface"),
                )
            }
        }

        composeTestRule
            .onNodeWithTag("surface")
            .assertTextEquals(0xFF0B0F17.toInt().toString())
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `notnight system qualifier keeps light theme`() {
        RuntimeEnvironment.setQualifiers("+notnight")

        composeTestRule.setContent {
            TogoTheme(darkTheme = isSystemDarkTheme()) {
                Text(
                    text = togoColors().surfaceBase.toArgb().toString(),
                    modifier = Modifier.testTag("surface"),
                )
            }
        }

        composeTestRule
            .onNodeWithTag("surface")
            .assertTextEquals(0xFFFFFFFF.toInt().toString())
    }

    @Test
    fun `dark theme exposes dark tokens to composition locals`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                val colors = togoColors()
                Text(
                    text = "${colors.surfaceBase.toArgb()}|" +
                        "${colors.inkPrimary.toArgb()}|${colors.warningInk.toArgb()}",
                    modifier = Modifier.testTag("tokens"),
                )
            }
        }

        composeTestRule.onNodeWithTag("tokens").assertTextEquals(
            "${0xFF0B0F17.toInt()}|${0xFFF8FAFC.toInt()}|${0xFFF59E0B.toInt()}"
        )
    }

    @Test
    @Config(qualifiers = "night")
    fun `night window and status bar resolve to the dark theme`() {
        RuntimeEnvironment.setQualifiers("+night")
        val context = RuntimeEnvironment.getApplication()
        assertEquals(
            0xFF0B0F17.toInt(),
            context.getColor(R.color.togo_window_background),
        )

        val themed = ContextThemeWrapper(context, R.style.Theme_Togo)
        val attrs = themed.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.statusBarColor, android.R.attr.windowLightStatusBar)
        )
        assertEquals(0xFF0B0F17.toInt(), attrs.getColor(0, 0))
        assertFalse(attrs.getBoolean(1, true))
        attrs.recycle()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `light window and status bar resolve to the light theme`() {
        RuntimeEnvironment.setQualifiers("+notnight")
        val context = RuntimeEnvironment.getApplication()
        assertEquals(
            0xFFFFFFFF.toInt(),
            context.getColor(R.color.togo_window_background),
        )

        val themed = ContextThemeWrapper(context, R.style.Theme_Togo)
        val attrs = themed.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.statusBarColor, android.R.attr.windowLightStatusBar)
        )
        assertEquals(0xFFFFFFFF.toInt(), attrs.getColor(0, 0))
        assertTrue(attrs.getBoolean(1, false))
        attrs.recycle()
    }

    @Test
    fun `dark voice bottom sheet keeps content and cancel target`() {
        // Stato Error (non Listening) per evitare l'animazione infinita del pulse
        // nei test Compose; il componente e il target di "Annulla" sono quelli reali.
        composeTestRule.setContent {
            TogoTheme(darkTheme = true) {
                VoiceBottomSheet(
                    uiState = VoiceBottomSheetUiState.Error(
                        code = "no_match",
                        message = "Non ho capito, riprova.",
                    ),
                    onEdit = {},
                    onConfirm = {},
                    onRetry = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Errore").assertExists()
        composeTestRule.onNodeWithText("Non ho capito, riprova.").assertExists()
        composeTestRule
            .onNodeWithText("Annulla")
            .assertExists()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }
}
