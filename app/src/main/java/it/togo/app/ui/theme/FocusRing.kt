package it.togo.app.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Focus indication condivisa: mantiene il ripple Material e disegna un doppio bordo ad alto contrasto. */
@Composable
internal fun rememberTogoFocusRingIndication(
    colors: TogoColorScheme,
    darkTheme: Boolean,
): IndicationNodeFactory {
    val ring = colors.focusRing
    val outline = if (darkTheme) TogoColorTokens.DarkSurfaceBase else TogoColorTokens.LightInkInverse
    return remember(ring, outline) { TogoFocusRingIndication(ring, outline) }
}

private class TogoFocusRingIndication(
    private val ringColor: Color,
    private val contrastOutline: Color,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource) =
        TogoFocusRingNode(interactionSource, ringColor, contrastOutline)

    override fun hashCode(): Int = 31 * ringColor.hashCode() + contrastOutline.hashCode()

    override fun equals(other: Any?): Boolean =
        other is TogoFocusRingIndication &&
            ringColor == other.ringColor && contrastOutline == other.contrastOutline
}

private class TogoFocusRingNode(
    private val interactionSource: InteractionSource,
    private val ringColor: Color,
    private val contrastOutline: Color,
) : DelegatingNode(), DrawModifierNode {
    private val rippleNode = ripple().create(interactionSource)
    private var focused = false

    override fun onAttach() {
        delegate(rippleNode)
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> {
                        focused = true
                        invalidateDraw()
                    }
                    is FocusInteraction.Unfocus -> {
                        focused = false
                        invalidateDraw()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        undelegate(rippleNode)
    }

    override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        drawTogoFocusRing(ringColor, contrastOutline)
    }
}

internal fun DrawScope.drawTogoFocusRing(ringColor: Color, contrastOutline: Color) {
    val ringWidth = 2.dp.toPx()
    val outlineWidth = 6.dp.toPx()
    val inset = outlineWidth / 2f
    val ringBounds = Size(
        width = (size.width - outlineWidth).coerceAtLeast(0f),
        height = (size.height - outlineWidth).coerceAtLeast(0f),
    )
    val topLeft = Offset(inset, inset)
    val radius = CornerRadius(8.dp.toPx())
    drawRoundRect(
        color = contrastOutline,
        topLeft = topLeft,
        size = ringBounds,
        cornerRadius = radius,
        style = Stroke(outlineWidth),
    )
    drawRoundRect(
        color = ringColor,
        topLeft = topLeft,
        size = ringBounds,
        cornerRadius = radius,
        style = Stroke(ringWidth),
    )
}

/** Applica la stessa doppia bordatura ai campi testuali che non usano un'indicazione Material. */
fun Modifier.togoFocusRing(): Modifier = composed {
    val colors = togoColors()
    val outline = if (colors.isDark) TogoColorTokens.DarkSurfaceBase else TogoColorTokens.LightInkInverse
    var focused by remember { mutableStateOf(false) }
    onFocusChanged { focused = it.isFocused }
        .drawWithContent {
            drawContent()
            if (focused) {
                drawTogoFocusRing(colors.focusRing, outline)
            }
        }
}
