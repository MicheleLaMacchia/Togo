package it.togo.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusRingTest {
    @Test
    fun `focus ring painter emits ring and outline strokes for both themes`() {
        assertEquals(2, countFocusRingStrokes(LightTogoColorSchemeImpl().focusRing, TogoColorTokens.LightInkInverse))
        assertEquals(2, countFocusRingStrokes(DarkTogoColorSchemeImpl().focusRing, TogoColorTokens.DarkSurfaceBase))
    }

    private fun countFocusRingStrokes(
        ringColor: androidx.compose.ui.graphics.Color,
        outlineColor: androidx.compose.ui.graphics.Color,
    ): Int {
        var roundRectCount = 0
        val canvas = Proxy.newProxyInstance(
            Canvas::class.java.classLoader,
            arrayOf(Canvas::class.java),
        ) { _, method, _ ->
            if (method.name == "drawRoundRect") roundRectCount++
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                java.lang.Float.TYPE -> 0f
                java.lang.Long.TYPE -> 0L
                else -> null
            }
        } as Canvas

        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(72f, 72f),
        ) {
            drawTogoFocusRing(ringColor, outlineColor)
        }
        return roundRectCount
    }
}
