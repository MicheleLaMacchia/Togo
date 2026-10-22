package it.togo.app.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Badge "Personalizzato" per i prodotti canonici user-defined (Story 7.1).
 *
 * Non comunica l'informazione solo con il colore: il testo è esplicito.
 * Usa i token `badgeBg`/`badgeInk` già verificati per contrasto.
 */
@Composable
internal fun UserDefinedBadge(modifier: Modifier = Modifier) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Surface(
        modifier = modifier,
        color = colors.badgeBg,
        contentColor = colors.badgeInk,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "Personalizzato",
            style = typography.caption,
            modifier = Modifier.padding(
                horizontal = spacing.space2,
                vertical = spacing.space1,
            ),
        )
    }
}
