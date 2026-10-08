package it.togo.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test JVM per i token del Design System TOGO.
 *
 * Verifica che i valori costanti corrispondano a DESIGN.md e che
 * i requisiti di contrasto siano soddisfatti (senza dipendenze Android).
 */
class DesignSystemTokenTest {

    @Test
    fun `light color tokens match DESIGN.md`() {
        assertEquals(0xFFFFFFFF, TogoColorTokens.LightSurfaceBase.toArgb())
        assertEquals(0xFFF8FAFC, TogoColorTokens.LightSurfaceSubtle.toArgb())
        assertEquals(0xFFFFFFFF, TogoColorTokens.LightSurfaceCard.toArgb())
        assertEquals(0xFF0F172A, TogoColorTokens.LightSurfaceInverse.toArgb())
        assertEquals(0xFF0F172A, TogoColorTokens.LightInkPrimary.toArgb())
        assertEquals(0xFF475569, TogoColorTokens.LightInkSecondary.toArgb())
        assertEquals(0xFF94A3B8, TogoColorTokens.LightInkMuted.toArgb())
        assertEquals(0xFFFFFFFF, TogoColorTokens.LightInkInverse.toArgb())
        assertEquals(0xFF0F172A, TogoColorTokens.LightBorderCrisp.toArgb())
        assertEquals(0xFFCBD5E1, TogoColorTokens.LightBorderHairline.toArgb())
        assertEquals(0xFF0F172A, TogoColorTokens.LightAccentAction.toArgb())
        assertEquals(0xFF15803D, TogoColorTokens.LightAccentSuccess.toArgb())
        assertEquals(0xFF2563EB, TogoColorTokens.LightAccentHighlight.toArgb())
        assertEquals(0xFFB45309, TogoColorTokens.LightAccentWarning.toArgb())
        assertEquals(0xFF0F172A, TogoColorTokens.LightBadgeBg.toArgb())
        assertEquals(0xFFFFFFFF, TogoColorTokens.LightBadgeInk.toArgb())
    }

    @Test
    fun `dark color tokens match DESIGN.md`() {
        assertEquals(0xFF0B0F17, TogoColorTokens.DarkSurfaceBase.toArgb())
        assertEquals(0xFF131B2E, TogoColorTokens.DarkSurfaceCard.toArgb())
        assertEquals(0xFFF8FAFC, TogoColorTokens.DarkInkPrimary.toArgb())
        assertEquals(0xFF94A3B8, TogoColorTokens.DarkInkSecondary.toArgb())
        assertEquals(0xFF475569, TogoColorTokens.DarkBorderCrisp.toArgb())
        assertEquals(0xFF60A5FA, TogoColorTokens.DarkAccentHighlight.toArgb())
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
        // title-screen: 24sp SemiBold
        assertEquals(24f, TogoTypography.titleScreen.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold, TogoTypography.titleScreen.fontWeight)

        // section-header: 16sp Bold
        assertEquals(16f, TogoTypography.sectionHeader.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, TogoTypography.sectionHeader.fontWeight)

        // item-name: 16sp Medium
        assertEquals(16f, TogoTypography.itemName.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, TogoTypography.itemName.fontWeight)

        // item-meta: 14sp SemiBold (min 14sp per AC)
        assertEquals(14f, TogoTypography.itemMeta.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold, TogoTypography.itemMeta.fontWeight)

        // caption: 12sp Regular
        assertEquals(12f, TogoTypography.caption.fontSize?.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Normal, TogoTypography.caption.fontWeight)
    }

    @Test
    fun `spacing scale is 4px based`() {
        assertEquals(4f, TogoSpacing.space1.value)
        assertEquals(8f, TogoSpacing.space2.value)
        assertEquals(12f, TogoSpacing.space3.value)
        assertEquals(16f, TogoSpacing.space4.value)
        assertEquals(24f, TogoSpacing.space5.value)
        assertEquals(32f, TogoSpacing.space6.value)
        assertEquals(48f, TogoSpacing.touchTargetMin.value)
    }

    @Test
    fun `shapes have correct corner radii`() {
        assertEquals(0f, TogoShapes.none.corners.topLeft.value)
        assertEquals(4f, TogoShapes.sm.corners.topLeft.value)
        assertEquals(8f, TogoShapes.md.corners.topLeft.value)
        assertEquals(12f, TogoShapes.lg.corners.topLeft.value)
        // full è 9999dp (cerchio)
        assertTrue(TogoShapes.full.corners.topLeft.value >= 9000f)
    }

    @Test
    fun `component tokens factory produces correct light tokens`() {
        val lightScheme = LightTogoColorSchemeImpl()
        val shapes = TogoShapes

        val tokens = TogoComponentTokens.from(lightScheme, shapes)

        assertEquals(0xFFFFFFFF, tokens.appBar.background.toArgb())
        assertEquals(0xFF0F172A, tokens.itemRow.borderColor.toArgb())
        assertEquals(0xFF0F172A, tokens.checkboxUtility.borderColor.toArgb())
        assertEquals(0xFFF8FAFC, tokens.categoryHeader.background.toArgb())
        assertEquals(0xFF0F172A, tokens.quantityBadge.background.toArgb())
        assertEquals(0xFF0F172A, tokens.voiceFab.background.toArgb())
        assertEquals(0xFFFFFFFF, tokens.bottomSheet.background.toArgb())
        assertEquals(0xFFFFFFFF, tokens.duplicateDialog.background.toArgb())
    }

    @Test
    fun `component tokens factory produces correct dark tokens`() {
        val darkScheme = DarkTogoColorSchemeImpl()
        val shapes = TogoShapes

        val tokens = TogoComponentTokens.from(darkScheme, shapes)

        assertEquals(0xFF0B0F17, tokens.appBar.background.toArgb())
        assertEquals(0xFF475569, tokens.itemRow.borderColor.toArgb())
        assertEquals(0xFF475569, tokens.checkboxUtility.borderColor.toArgb())
        assertEquals(0xFF131B2E, tokens.categoryHeader.background.toArgb())
        assertEquals(0xFF0F172A, tokens.quantityBadge.background.toArgb())
        assertEquals(0xFFF8FAFC, tokens.voiceFab.background.toArgb()) // accentAction = DarkInkPrimary
        assertEquals(0xFF0B0F17, tokens.bottomSheet.background.toArgb())
        assertEquals(0xFF0B0F17, tokens.duplicateDialog.background.toArgb())
    }

    @Test
    fun `contrast ratios meet WCAG AAA 12:1 for primary text on base surface`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        // Light: inkPrimary (#0F172A) on surfaceBase (#FFFFFF) = 17.85:1
        val lightContrast = contrastRatio(light.inkPrimary, light.surfaceBase)
        assertTrue("Light inkPrimary on surfaceBase = $lightContrast:1, richiesto ≥12:1", lightContrast >= 12.0)

        // Dark: inkPrimary (#F8FAFC) on surfaceBase (#0B0F17) = 18.33:1
        val darkContrast = contrastRatio(dark.inkPrimary, dark.surfaceBase)
        assertTrue("Dark inkPrimary on surfaceBase = $darkContrast:1, richiesto ≥12:1", darkContrast >= 12.0)
    }

    @Test
    fun `itemMeta 14sp SemiBold meets AA 7:1 on surfaceCard`() {
        val light = LightTogoColorSchemeImpl()
        val dark = DarkTogoColorSchemeImpl()

        val lightMetaContrast = contrastRatio(light.inkPrimary, light.surfaceCard)
        assertTrue("Light itemMeta on surfaceCard = $lightMetaContrast:1, richiesto ≥7:1", lightMetaContrast >= 7.0)

        val darkMetaContrast = contrastRatio(dark.inkPrimary, dark.surfaceCard)
        assertTrue("Dark itemMeta on surfaceCard = $darkMetaContrast:1, richiesto ≥7:1", darkMetaContrast >= 7.0)
    }

    @Test
    fun `dark borderCrisp meets 3:1 for non-text UI elements on surfaceBase`() {
        val dark = DarkTogoColorSchemeImpl()
        val borderContrast = contrastRatio(dark.borderCrisp, dark.surfaceBase)
        // WCAG 2.1 1.4.11: ≥3:1 per elementi UI non testuali
        assertTrue("Dark borderCrisp on surfaceBase = $borderContrast:1, richiesto ≥3:1", borderContrast >= 3.0)
    }

    /** Calcola rapporto di contrasto WCAG 2.1 tra due colori (luminanza relativa). */
    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private fun relativeLuminance(c: Color): Double {
        val r = linearize((c.red * 255).toInt())
        val g = linearize((c.green * 255).toInt())
        val b = linearize((c.blue * 255).toInt())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearize(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
}