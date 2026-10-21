package it.togo.app.presentation.activelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.util.Locale

/**
 * Header categoria (Level1 + Level2).
 *
 * Fascia con bordo sinistro 4dp crisp, titolo uppercase, badge count.
 */
@Composable
fun CategoryHeader(
    level1: TaxonomyLevel.Level1,
    level2: TaxonomyLevel.Level2,
    itemCount: Int,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2)
        ,
        color = tokens.categoryHeader.background,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = tokens.categoryHeader.borderLeftWidth.toPx()
                    val x = strokeWidth / 2f
                    drawLine(
                        color = tokens.categoryHeader.borderLeftColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth,
                    )
                }
                .padding(
                    horizontal = tokens.categoryHeader.paddingHorizontal,
                    vertical = tokens.categoryHeader.paddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = level2.name.uppercase(Locale.ROOT),
                style = typography.sectionHeader,
                color = colors.inkPrimary,
                // A font grande il titolo va a capo invece di spingere il
                // badge conteggio fuori schermo.
                modifier = Modifier.weight(1f),
            )

            // Badge count (dimensione intrinseca, resta visibile a destra)
            if (itemCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = spacing.space1)
                        .background(
                            color = colors.inkPrimary,
                            shape = RoundedCornerShape(spacing.space1),
                        )
                        .padding(horizontal = spacing.space2, vertical = 2.dp),
                ) {
                    Text(
                        text = itemCount.toString(),
                        style = typography.caption,
                        color = colors.inkInverse,
                    )
                }
            }
        }
    }
}

/** Sezione completa categoria: Header Level1 + gruppi Level2 (con Level3) */
@Composable
fun CategorySection(
    group: CategoryGroup,
    productNames: Map<Long, String> = emptyMap(),
    categoryLabels: Map<Long, String> = emptyMap(),
    onCheckOff: (String, Boolean) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onDelete: (String) -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        // Header Level1 (solo al primo Level2 del gruppo)
        Text(
            text = group.level1.name.uppercase(Locale.ROOT),
            style = typography.sectionHeader.copy(color = colors.inkPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space2, vertical = spacing.space1),
        )

        // Sottogruppi Level2
        group.level2Groups.forEach { l2Group ->
            CategoryHeader(
                level1 = group.level1,
                level2 = l2Group.level2,
                itemCount = l2Group.items.size,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                // Gruppi Level3 dentro Level2
                l2Group.level3Groups.forEach { l3Group ->
                    // Header Level3 opzionale (se ci sono più Level3)
                    if (l2Group.level3Groups.size > 1) {
                        Text(
                            text = l3Group.level3.name,
                            style = typography.itemMeta.copy(color = colors.inkSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.space2 + spacing.space1, vertical = spacing.space1),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.space1),
                    ) {
                        l3Group.items.forEach { item ->
                            ItemRow(
                                item = item,
                                productName = productNames[item.productId] ?: "Prodotto #${item.productId}",
                                categoryLabel = categoryLabels[item.productId],
                                rowActionDescription = "Doppio tocco per modificare.",
                                onCheckChange = { checked -> onCheckOff(item.id, checked) },
                                onClick = { onEdit(item) },
                                onDelete = { onDelete(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
