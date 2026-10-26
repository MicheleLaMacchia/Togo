package it.togo.app.presentation.activelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
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
            CategoryCountBadge(
                count = itemCount,
                colors = colors,
                typography = typography,
                spacing = spacing,
            )
        }
    }
}

/** Badge numerico condiviso tra header Level1 e Level2. */
@Composable
private fun CategoryCountBadge(
    count: Int,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    if (count > 0) {
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
                text = count.toString(),
                style = typography.caption,
                color = colors.inkInverse,
            )
        }
    }
}

/**
 * Sezione completa categoria: header Level1 collassabile + gruppi Level2.
 *
 * Il conteggio in testa è il totale delle voci attive del Level1 e resta
 * visibile anche quando la sezione è collassata.
 */
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
    var expanded by rememberSaveable(group.level1.id) { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Level1Header(
            group = group,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            colors = colors,
            typography = typography,
            spacing = spacing,
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
            exit = shrinkVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
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
                                        .padding(
                                            horizontal = spacing.space2 + spacing.space1,
                                            vertical = spacing.space1,
                                        ),
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
    }
}

/** Intestazione Level1 con conteggio e controllo espandi/collassa. */
@Composable
private fun Level1Header(
    group: CategoryGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    val count = group.items.size
    val countLabel = if (count == 1) "voce" else "voci"
    val stateText = if (expanded) "espansa" else "collassata"
    val toggleAction = if (expanded) "collassare" else "espandere"
    val headerDescription =
        "${group.level1.name}, $count $countLabel. Doppio tocco per $toggleAction."

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) {
                contentDescription = headerDescription
                stateDescription = stateText
            }
            .testTag("level1_header_${group.level1.id}")
            .padding(vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = group.level1.name.uppercase(Locale.ROOT),
            style = typography.sectionHeader.copy(color = colors.inkPrimary),
            modifier = Modifier.weight(1f),
        )

        CategoryCountBadge(
            count = count,
            colors = colors,
            typography = typography,
            spacing = spacing,
        )

        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = colors.inkSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}
