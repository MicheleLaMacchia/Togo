package it.togo.app.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoComponentTokens
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypographyTokens

/**
 * Item risultato ricerca con highlighting termini.
 *
 * Mostra nome prodotto con termini evidenziati (background accentHighlight, bold)
 * e attributi opzionali (marca, variante, conservazione) sotto il nome.
 */
@Composable
fun SearchResultItem(
    product: CanonicalProduct,
    query: String,
    onClick: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypographyTokens()
    val spacing = togoSpacing()
    val tokens = togoComponentTokens()

    val highlightedName = buildAnnotatedStringHighlight(product.name, query)
    val metaParts = buildList<String> {
        if (product.name.isNotBlank()) add(product.name)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing[2], vertical = spacing[1])
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.itemRow.radius)),
        shape = RoundedCornerShape(tokens.itemRow.radius),
        border = androidx.compose.ui.graphics.BorderStroke(tokens.itemRow.borderWidth, colors.borderCrisp),
        color = tokens.itemRow.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = tokens.itemRow.paddingHorizontal,
                    vertical = tokens.itemRow.paddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing[3]),
        ) {
            // 1. Icona prodotto
            Box(
                modifier = Modifier
                    .size(tokens.checkboxUtility.size)
                    .background(
                        color = colors.surfaceSubtle,
                        shape = RoundedCornerShape(tokens.checkboxUtility.radius),
                    )
                    .border(
                        tokens.checkboxUtility.borderWidth,
                        colors.borderHairline,
                        RoundedCornerShape(tokens.checkboxUtility.radius),
                    )
                    .clip(RoundedCornerShape(tokens.checkboxUtility.radius)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBasket,
                    contentDescription = "Prodotto",
                    tint = colors.inkMuted,
                    modifier = Modifier.size(tokens.checkboxUtility.size * 0.55f),
                )
            }

            // 2. Centro: nome + meta
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = highlightedName,
                    style = typography.itemName,
                    color = colors.inkPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Meta opzionale: solo nome per ora
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" · "),
                        style = typography.caption,
                        color = colors.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun buildAnnotatedStringHighlight(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    if (query.isNotBlank()) {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var index = lowerText.indexOf(lowerQuery)
        while (index >= 0) {
            builder.addStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    background = androidx.compose.ui.graphics.Color(0xFF2563EB), // accentHighlight
                ),
                start = index,
                end = index + query.length,
            )
            index = lowerText.indexOf(lowerQuery, index + query.length)
        }
    }
    return builder.toAnnotatedString()
}