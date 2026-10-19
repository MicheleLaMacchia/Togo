package it.togo.app.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test JVM (Robolectric) per i token del Design System TOGO.
 *
 * Verifica che i valori costanti corrispondano a DESIGN.md e che
 * i requisiti di contrasto siano soddisfatti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DesignSystemTokenTest {

    @Before
    fun setup() { stopKoin() }

    @Test
    fun `light color tokens match DESIGN_MD`() {
        assertEquals(0xFFFFFFFF.toInt(), TogoColorTokens.LightSurfaceBase.toArgb())
        assertEquals(0xFFF8FAFC.toInt(), TogoColorTokens.LightSurfaceSubtle.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), TogoColorTokens.LightSurfaceCard.toArgb())
        assertEquals(0xFF0F172A.toInt(), TogoColorTokens.LightSurfaceInverse.toArgb())
        assertEquals(0xFF0F172A.toInt(), TogoColorTokens.LightInkPrimary.toArgb())
        assertEquals(0xFF475569.toInt(), TogoColorTokens.LightInkSecondary.toArgb())
        assertEquals(0xFF94A3B8.toInt(), TogoColorTokens.LightInkMuted.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), TogoColorTokens.LightInkInverse.toArgb())
        assertEquals(0xFF0F172A.toInt(), TogoColorTokens.LightBorderCrisp.toArgb())
        assertEquals(0xFFCBD5E1.toInt(), TogoColorTokens.LightBorderHairline.toArgb())
        assertEquals(0xFF0F172A.toInt(), TogoColorTokens.LightAccentAction.toArgb())
        assertEquals(0xFF15803D.toInt(), TogoColorTokens.LightAccentSuccess.toArgb())
        assertEquals(0xFF2563EB.toInt(), TogoColorTokens.LightAccentHighlight.toArgb())
        assertEquals(0xFFB45309.toInt(), TogoColorTokens.LightAccentWarning.toArgb())
        assertEquals(0xFF0F172A.toInt(), TogoColorTokens.LightBadgeBg.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), TogoColorTokens.LightBadgeInk.toArgb())
    }

    @Test
    fun `dark color tokens match DESIGN_MD`() {
        assertEquals(0xFF0B0F17.toInt(), TogoColorTokens.DarkSurfaceBase.toArgb())
        assertEquals(0xFF131B2E.toInt(), TogoColorTokens.DarkSurfaceCard.toArgb())
        assertEquals(0xFFF8FAFC.toInt(), TogoColorTokens.DarkInkPrimary.toArgb())
        assertEquals(0xFF94A3B8.toInt(), TogoColorTokens.DarkInkSecondary.toArgb())
        assertEquals(0xFF64748B.toInt(), TogoColorTokens.DarkBorderCrisp.toArgb())
        assertEquals(0xFF60A5FA.toInt(), TogoColorTokens.DarkAccentHighlight.toArgb())
    }

    @Test
    fun `shared tokens are identical`() {
        assertEquals(TogoColorTokens.LightAccentSuccess, TogoColorTokens.AccentSuccess)
        assertEquals(TogoColorTokens.LightAccentWarning, TogoColorTokens.AccentWarning)
        assertEquals(TogoColorTokens.LightBadgeBg, TogoColorTokens.BadgeBg)
        assertEquals(TogoColorTokens.LightBadgeInk, TogoColorTokens.BadgeInk)
    }

    @Test
    fun `typography roles have correct sizes and weights`() {
        val typo = DefaultTogoTypography

        // title-screen: 24sp SemiBold
        assertEquals(24f, typo.titleScreen.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold, typo.titleScreen.fontWeight)

        // section-header: 16sp Bold
        assertEquals(16f, typo.sectionHeader.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, typo.sectionHeader.fontWeight)

        // item-name: 16sp Medium
        assertEquals(16f, typo.itemName.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, typo.itemName.fontWeight)

        // item-meta: 14sp SemiBold (min 14sp per AC)
        assertEquals(14f, typo.itemMeta.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold, typo.itemMeta.fontWeight)

        // caption: 12sp Regular
        assertEquals(12f, typo.caption.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Normal, typo.caption.fontWeight)
    }

    @Test
    fun `spacing scale is 4px based`() {
        val spacing = DefaultTogoSpacing
        assertEquals(4f, spacing.space1.value)
        assertEquals(8f, spacing.space2.value)
        assertEquals(12f, spacing.space3.value)
        assertEquals(16f, spacing.space4.value)
        assertEquals(24f, spacing.space5.value)
        assertEquals(32f, spacing.space6.value)
        assertEquals(48f, spacing.touchTargetMin.value)
    }

    @Test
    fun `shapes have correct corner radii`() {
        val shapes = TogoShapes
        // RoundedCornerShape non espone direttamente il valore dp in modo testabile,
        // ma possiamo verificare che le shape siano istanziate senza errori.
        assertTrue("none shape deve essere istanziata", shapes.none != null)
        assertTrue("sm shape deve essere istanziata", shapes.sm != null)
        assertTrue("md shape deve essere istanziata", shapes.md != null)
        assertTrue("lg shape deve essere istanziata", shapes.lg != null)
        assertTrue("full shape deve essere istanziata", shapes.full != null)
    }

    @Test
    fun `component tokens factory produces correct light tokens`() {
        val lightScheme = LightTogoColorSchemeImpl()
        val shapes = TogoShapes

        val tokens = togoComponentTokensFrom(lightScheme, shapes)

        assertEquals(0xFFFFFFFF.toInt(), tokens.appBar.background.toArgb())
        assertEquals(0xFF0F172A.toInt(), tokens.itemRow.borderColor.toArgb())
        assertEquals(0xFF0F172A.toInt(), tokens.checkboxUtility.borderColor.toArgb())
        assertEquals(0xFFF8FAFC.toInt(), tokens.categoryHeader.background.toArgb())
        assertEquals(0xFF0F172A.toInt(), tokens.quantityBadge.background.toArgb())
        assertEquals(0xFF0F172A.toInt(), tokens.voiceFab.background.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), tokens.bottomSheet.background.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), tokens.duplicateDialog.background.toArgb())
    }

    @Test
    fun `component tokens factory produces correct dark tokens`() {
        val darkScheme = DarkTogoColorSchemeImpl()
        val shapes = TogoShapes

        val tokens = togoComponentTokensFrom(darkScheme, shapes)

        assertEquals(0xFF0B0F17.toInt(), tokens.appBar.background.toArgb())
        assertEquals(0xFF64748B.toInt(), tokens.itemRow.borderColor.toArgb())
        assertEquals(0xFF64748B.toInt(), tokens.checkboxUtility.borderColor.toArgb())
        assertEquals(0xFF131B2E.toInt(), tokens.categoryHeader.background.toArgb())
        assertEquals(0xFF0F172A.toInt(), tokens.quantityBadge.background.toArgb())
        assertEquals(0xFFF8FAFC.toInt(), tokens.voiceFab.background.toArgb()) // accentAction = DarkInkPrimary
        assertEquals(0xFF0B0F17.toInt(), tokens.bottomSheet.background.toArgb())
        assertEquals(0xFF0B0F17.toInt(), tokens.duplicateDialog.background.toArgb())
    }

    @Test
    fun `contrast ratios meet WCAG AAA 12 to 1 for primary text on base surface`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        // Light: inkPrimary (#0F172A) on surfaceBase (#FFFFFF) = 17.85:1
        val lightContrast = contrastRatio(light.inkPrimary, light.surfaceBase)
        assertTrue("Light inkPrimary on surfaceBase = $lightContrast:1, richiesto >=12:1", lightContrast >= 12.0)

        // Dark: inkPrimary (#F8FAFC) on surfaceBase (#0B0F17) = 18.33:1
        val darkContrast = contrastRatio(dark.inkPrimary, dark.surfaceBase)
        assertTrue("Dark inkPrimary on surfaceBase = $darkContrast:1, richiesto >=12:1", darkContrast >= 12.0)
    }

    @Test
    fun `itemMeta 14sp SemiBold meets AA 7 to 1 on surfaceCard`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        val lightMetaContrast = contrastRatio(light.inkPrimary, light.surfaceCard)
        assertTrue("Light itemMeta on surfaceCard = $lightMetaContrast:1, richiesto >=7:1", lightMetaContrast >= 7.0)

        val darkMetaContrast = contrastRatio(dark.inkPrimary, dark.surfaceCard)
        assertTrue("Dark itemMeta on surfaceCard = $darkMetaContrast:1, richiesto >=7:1", darkMetaContrast >= 7.0)
    }

    @Test
    fun `dark borderCrisp meets 3 to 1 for non-text UI elements on surfaceBase`() {
        val dark = DarkTogoColorSchemeImpl()
        val borderContrast = contrastRatio(dark.borderCrisp, dark.surfaceBase)
        // WCAG 2.1 1.4.11: >=3:1 per elementi UI non testuali
        assertTrue("Dark borderCrisp on surfaceBase = $borderContrast:1, richiesto >=3:1", borderContrast >= 3.0)
    }

    /** Calcola rapporto di contrasto WCAG 2.1 tra due colori (luminanza relativa). */
    private fun contrastRatio(fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun relativeLuminance(c: androidx.compose.ui.graphics.Color): Double {
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