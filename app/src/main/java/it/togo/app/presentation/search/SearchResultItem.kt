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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoTypography

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
    val typography = togotypography()
    val spacing = it.togo.app.ui.theme.togoSpacing()
    val tokens = componentTokens()

    val highlightedName = buildAnnotatedStringHighlight(product.name, query)
    val metaParts = buildList<String> {
        if (product.brand?.isNotBlank() == true) add(product.brand)
        if (product.variant?.isNotBlank() == true) add(product.variant)
        if (product.condition?.isNotBlank() == true) add(product.condition)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .combinedClickable(onClick = onClick)
            .clip(RoundedCornerShape(tokens.itemRow.radius)),
        shape = RoundedCornerShape(tokens.itemRow.radius),
        border = BorderStroke(tokens.itemRow.borderWidth, colors.borderCrisp),
        color = tokens.itemRow.background,
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = tokens.itemRow.paddingHorizontal,
                    vertical = tokens.itemRow.paddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
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
                    imageVector = androidx.compose.material.icons.Icons.Filled.ShoppingBasket,
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
                    textDecoration = TextDecoration.None,
                )

                // Meta opzionale: brand + variante + condition
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