package it.togo.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.presentation.activelist.voiceFabContainerColor
import it.togo.app.presentation.activelist.voiceFabContentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test JVM (Robolectric) per i contrasti della Dark Mode (Story 6.3).
 *
 * Usa lo stesso calcolo WCAG 2.1 di [DesignSystemTokenTest] e verifica le
 * coppie testo/sfondo usate dai componenti coinvolti nella dark mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DarkModeContrastTest {

    @Before
    fun setup() {
        stopKoin()
    }

    @Test
    fun `dark color tokens match the acceptance criteria palette`() {
        val dark = DarkTogoColorSchemeImpl()

        assertEquals(0xFF0B0F17.toInt(), dark.surfaceBase.toArgb())
        assertEquals(0xFF131B2E.toInt(), dark.surfaceCard.toArgb())
        assertEquals(0xFFF8FAFC.toInt(), dark.inkPrimary.toArgb())
        assertEquals(0xFF94A3B8.toInt(), dark.inkSecondary.toArgb())
        assertEquals(0xFF64748B.toInt(), dark.borderCrisp.toArgb())
        assertEquals(0xFF60A5FA.toInt(), dark.accentHighlight.toArgb())
        assertEquals(0xFFF59E0B.toInt(), dark.warningInk.toArgb())
        assertEquals(0xFF4ADE80.toInt(), dark.successInk.toArgb())
        assertTrue(dark.isDark)
        assertTrue(!LightTogoColorSchemeImpl().isDark)
    }

    @Test
    fun `modal scrims stay dark in both themes`() {
        // I ModalBottomSheet usano MaterialTheme.colorScheme.scrim: in dark
        // non deve diventare il bianco di surfaceInverse.
        assertEquals(0xFF0F172A.toInt(), Material3LightColorScheme.scrim.toArgb())
        assertEquals(0xFF0F172A.toInt(), Material3DarkColorScheme.scrim.toArgb())
        assertEquals(0xFF0F172A.toInt(), LightTogoColorSchemeImpl().scrim.toArgb())
        assertEquals(0xFF0F172A.toInt(), DarkTogoColorSchemeImpl().scrim.toArgb())
    }

    @Test
    fun `inkPrimary on surfaceBase is at least 7 to 1 in both themes`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        val lightRatio = contrastRatio(light.inkPrimary, light.surfaceBase)
        val darkRatio = contrastRatio(dark.inkPrimary, dark.surfaceBase)

        assertTrue("Light inkPrimary on surfaceBase = $lightRatio:1, richiesto >=7:1", lightRatio >= 7.0)
        assertTrue("Dark inkPrimary on surfaceBase = $darkRatio:1, richiesto >=7:1", darkRatio >= 7.0)
    }

    @Test
    fun `secondary ink is readable on cards in both themes`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        listOf(
            "inkSecondary su surfaceCard (light)" to contrastRatio(
                light.inkSecondary,
                light.surfaceCard,
            ),
            "inkSecondary su surfaceCard (dark)" to contrastRatio(
                dark.inkSecondary,
                dark.surfaceCard,
            ),
        ).forEach { (label, ratio) ->
            assertTrue("$label = $ratio:1, richiesto >=4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun `checkout button content stays readable on the shared green in both themes`() {
        val lightTokens = togoComponentTokensFrom(LightTogoColorSchemeImpl(), TogoShapes)
        val darkTokens = togoComponentTokensFrom(DarkTogoColorSchemeImpl(), TogoShapes)

        val lightRatio = contrastRatio(
            lightTokens.bottomSheet.checkoutButtonContent,
            lightTokens.bottomSheet.checkoutButtonBackground,
        )
        val darkRatio = contrastRatio(
            darkTokens.bottomSheet.checkoutButtonContent,
            darkTokens.bottomSheet.checkoutButtonBackground,
        )

        assertTrue(
            "Concludi spesa (light) = $lightRatio:1, richiesto >=4.5:1",
            lightRatio >= 4.5,
        )
        assertTrue(
            "Concludi spesa (dark) = $darkRatio:1, richiesto >=4.5:1",
            darkRatio >= 4.5,
        )
    }

    @Test
    fun `idle voice fab icon is readable on accentHighlight in both themes`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        // Passa dagli helper usati da VoiceFab, non da una coppia ricopiata nel test.
        val lightRatio = contrastRatio(
            voiceFabContentColor(VoiceRecognitionState.Idle, light),
            voiceFabContainerColor(VoiceRecognitionState.Idle, light),
        )
        val darkRatio = contrastRatio(
            voiceFabContentColor(VoiceRecognitionState.Idle, dark),
            voiceFabContainerColor(VoiceRecognitionState.Idle, dark),
        )

        assertTrue("FAB idle (light) = $lightRatio:1, richiesto >=4.5:1", lightRatio >= 4.5)
        assertTrue("FAB idle (dark) = $darkRatio:1, richiesto >=4.5:1", darkRatio >= 4.5)
    }

    @Test
    fun `voice listening overlay text is readable on the fixed dark backdrop`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        // Il backdrop è lo scrim scuro al 70% sia in light sia in dark: il caso
        // peggiore è quando copre una superficie chiara.
        val overLight = blend(light.scrim, light.surfaceBase, VOICE_OVERLAY_SCRIM_ALPHA)
        val overDark = blend(dark.scrim, dark.surfaceBase, VOICE_OVERLAY_SCRIM_ALPHA)

        val white = TogoColorTokens.OnAccent
        val overLightRatio = contrastRatio(white, overLight)
        val overDarkRatio = contrastRatio(white, overDark)

        assertTrue(
            "Overlay vocale su superficie chiara = $overLightRatio:1, richiesto >=4.5:1",
            overLightRatio >= 4.5,
        )
        assertTrue(
            "Overlay vocale su superficie scura = $overDarkRatio:1, richiesto >=4.5:1",
            overDarkRatio >= 4.5,
        )

        // Il testo secondario usa bianco all'80%: il composito deve restare >=4.5:1.
        val softText = blend(white, overLight, 0.8f)
        val softRatio = contrastRatio(softText, overLight)
        assertTrue(
            "Testo secondario overlay = $softRatio:1, richiesto >=4.5:1",
            softRatio >= 4.5,
        )
    }

    @Test
    fun `search highlight uses accentHighlight and inkInverse with AA contrast`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        val lightRatio = contrastRatio(light.inkInverse, light.accentHighlight)
        val darkRatio = contrastRatio(dark.inkInverse, dark.accentHighlight)

        assertTrue("Highlight ricerca (light) = $lightRatio:1, richiesto >=4.5:1", lightRatio >= 4.5)
        assertTrue("Highlight ricerca (dark) = $darkRatio:1, richiesto >=7:1", darkRatio >= 7.0)
    }

    @Test
    fun `checkbox check and quantity badge stay readable`() {
        val lightTokens = togoComponentTokensFrom(LightTogoColorSchemeImpl(), TogoShapes)
        val darkTokens = togoComponentTokensFrom(DarkTogoColorSchemeImpl(), TogoShapes)

        val lightCheck = contrastRatio(
            lightTokens.checkboxUtility.checkColor,
            lightTokens.checkboxUtility.checkedBackground,
        )
        val darkCheck = contrastRatio(
            darkTokens.checkboxUtility.checkColor,
            darkTokens.checkboxUtility.checkedBackground,
        )
        val lightBadge = contrastRatio(
            lightTokens.quantityBadge.textColor,
            lightTokens.quantityBadge.background,
        )
        val darkBadge = contrastRatio(
            darkTokens.quantityBadge.textColor,
            darkTokens.quantityBadge.background,
        )

        assertTrue("Checkbox check (light) = $lightCheck:1", lightCheck >= 4.5)
        assertTrue("Checkbox check (dark) = $darkCheck:1", darkCheck >= 4.5)
        assertTrue("Badge quantità (light) = $lightBadge:1", lightBadge >= 7.0)
        assertTrue("Badge quantità (dark) = $darkBadge:1", darkBadge >= 7.0)
    }

    @Test
    fun `swipe delete icon stays readable on the shared green`() {
        val lightTokens = togoComponentTokensFrom(LightTogoColorSchemeImpl(), TogoShapes)
        val darkTokens = togoComponentTokensFrom(DarkTogoColorSchemeImpl(), TogoShapes)

        val lightRatio = contrastRatio(
            lightTokens.itemRow.swipeDeleteIconColor,
            lightTokens.itemRow.swipeDeleteBackground,
        )
        val darkRatio = contrastRatio(
            darkTokens.itemRow.swipeDeleteIconColor,
            darkTokens.itemRow.swipeDeleteBackground,
        )

        assertTrue("Icona elimina swipe (light) = $lightRatio:1", lightRatio >= 4.5)
        assertTrue("Icona elimina swipe (dark) = $darkRatio:1", darkRatio >= 4.5)
    }

    @Test
    fun `warning ink is readable in both themes`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        listOf(
            "warning su surfaceBase (light)" to contrastRatio(light.warningInk, light.surfaceBase),
            "warning su surfaceCard (light)" to contrastRatio(light.warningInk, light.surfaceCard),
            "warning su surfaceBase (dark)" to contrastRatio(dark.warningInk, dark.surfaceBase),
            "warning su surfaceCard (dark)" to contrastRatio(dark.warningInk, dark.surfaceCard),
        ).forEach { (label, ratio) ->
            assertTrue("$label = $ratio:1, richiesto >=4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun `success ink is readable in both themes`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        listOf(
            "success su surfaceBase (light)" to contrastRatio(light.successInk, light.surfaceBase),
            "success su surfaceCard (light)" to contrastRatio(light.successInk, light.surfaceCard),
            "success su surfaceBase (dark)" to contrastRatio(dark.successInk, dark.surfaceBase),
            "success su surfaceCard (dark)" to contrastRatio(dark.successInk, dark.surfaceCard),
        ).forEach { (label, ratio) ->
            assertTrue("$label = $ratio:1, richiesto >=4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun `disabled controls keep their label readable in both themes`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        // AddItem (Avanti/Conferma) e VoiceFab in processing usano questa coppia.
        val lightRatio = contrastRatio(light.inkSecondary, light.surfaceSubtle)
        val darkRatio = contrastRatio(dark.inkSecondary, dark.surfaceSubtle)

        assertTrue("Disabilitato (light) = $lightRatio:1, richiesto >=4.5:1", lightRatio >= 4.5)
        assertTrue("Disabilitato (dark) = $darkRatio:1, richiesto >=4.5:1", darkRatio >= 4.5)
    }

    @Test
    fun `snackbar is dark with readable text and action in both themes`() {
        val lightTokens = togoComponentTokensFrom(LightTogoColorSchemeImpl(), TogoShapes)
        val darkTokens = togoComponentTokensFrom(DarkTogoColorSchemeImpl(), TogoShapes)

        listOf(
            "snackbar content (light)" to contrastRatio(
                lightTokens.snackbar.content,
                lightTokens.snackbar.container,
            ),
            "snackbar action (light)" to contrastRatio(
                lightTokens.snackbar.action,
                lightTokens.snackbar.container,
            ),
            "snackbar content (dark)" to contrastRatio(
                darkTokens.snackbar.content,
                darkTokens.snackbar.container,
            ),
            "snackbar action (dark)" to contrastRatio(
                darkTokens.snackbar.action,
                darkTokens.snackbar.container,
            ),
        ).forEach { (label, ratio) ->
            assertTrue("$label = $ratio:1, richiesto >=4.5:1", ratio >= 4.5)
        }

        // SnackbarHost predefinito (permesso notifiche): usa la coppia inverse Material.
        listOf(
            "inverse content (light)" to contrastRatio(
                Material3LightColorScheme.inverseOnSurface,
                Material3LightColorScheme.inverseSurface,
            ),
            "inverse content (dark)" to contrastRatio(
                Material3DarkColorScheme.inverseOnSurface,
                Material3DarkColorScheme.inverseSurface,
            ),
            "inverse action (light)" to contrastRatio(
                Material3LightColorScheme.inversePrimary,
                Material3LightColorScheme.inverseSurface,
            ),
            "inverse action (dark)" to contrastRatio(
                Material3DarkColorScheme.inversePrimary,
                Material3DarkColorScheme.inverseSurface,
            ),
        ).forEach { (label, ratio) ->
            assertTrue("$label = $ratio:1, richiesto >=4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun `duplicate dialog background and border follow dark tokens`() {
        val dark = DarkTogoColorSchemeImpl()
        val tokens = togoComponentTokensFrom(dark, TogoShapes)

        assertEquals(dark.surfaceBase.toArgb(), tokens.duplicateDialog.background.toArgb())
        val borderRatio = contrastRatio(
            tokens.duplicateDialog.borderColor,
            tokens.duplicateDialog.background,
        )
        assertTrue(
            "Bordo dialog duplicati (dark) = $borderRatio:1, richiesto >=3:1",
            borderRatio >= 3.0,
        )
    }

    private fun blend(foreground: Color, background: Color, alpha: Float): Color = Color(
        red = foreground.red * alpha + background.red * (1f - alpha),
        green = foreground.green * alpha + background.green * (1f - alpha),
        blue = foreground.blue * alpha + background.blue * (1f - alpha),
        alpha = 1f,
    )

    /** Calcola rapporto di contrasto WCAG 2.1 tra due colori (luminanza relativa). */
    private fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun relativeLuminance(c: Color): Double {
        val r = linearize((c.red * 255).toInt())
        val g = linearize((c.green * 255).toInt())
        val b = linearize((c.blue * 255).toInt())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearize(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
}
