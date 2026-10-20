package it.togo.app.presentation.activelist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Sezione collassabile "Presi (N)" in fondo alla lista.
 *
 * - Default espansa se checkedItems > 0
 * - Animazione expand/collapse spring
 * - Pulsante "Concludi spesa" in fondo
 * - Voci checkate con checkbox spuntata → tap per uncheck
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresiSection(
    items: List<ShoppingItem>,
    productNames: Map<Long, String> = emptyMap(),
    categoryLabels: Map<Long, String> = emptyMap(),
    onUncheck: (String) -> Unit,
    onCheckout: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    // Semantica dell'intestazione: nome sezione, conteggio e stato espansa/collassata.
    val count = items.size
    val stateText = if (expanded) "espansa" else "collassata"
    val toggleAction = if (expanded) "collassare" else "espandere"
    val headerDescription = "Presi, $count ${if (count == 1) "voce" else "voci"}. Doppio tocco per $toggleAction."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("presi_section")
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            )
        ,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        // Header collassabile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space2, vertical = spacing.space2)
                .background(colors.surfaceSubtle)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = colors.borderHairline,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = strokeWidth,
                    )
                }
                .heightIn(min = 48.dp) // Touch target minimo 48dp
                .combinedClickable(
                    onClick = { expanded = !expanded },
                    role = Role.Button,
                )
                .semantics {
                    contentDescription = headerDescription
                    stateDescription = stateText
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Presi (${items.size})",
                style = typography.sectionHeader,
                color = colors.inkPrimary,
            )
            Icon(
                imageVector = if (expanded)
                    Icons.Filled.ExpandLess
                else
                    Icons.Filled.ExpandMore,
                // Decorativa: l'azione è descritta dalla semantica dell'intera intestazione.
                contentDescription = null,
                tint = colors.inkSecondary,
                modifier = Modifier
                    .size(24.dp),
            )
        }

        // Contenuto animato
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space2)
                    .padding(bottom = spacing.space3),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                items.forEach { item ->
                    ItemRow(
                        item = item, // Usa lo stato reale dell'item
                        productName = productNames[item.productId] ?: "Prodotto #${item.productId}",
                        categoryLabel = categoryLabels[item.productId],
                        // In "Presi" il tap sulla riga non apre la modifica: la riga
                        // annuncia solo dati e stato, l'azione è sulla checkbox.
                        rowActionDescription = null,
                        onCheckChange = { checked ->
                            if (!checked) onUncheck(item.id) // Uncheck = move back to active
                        },
                        onClick = { /* no-op in presa - could enable edit later */ },
                        onDelete = { /* no-op in presa - could enable delete later */ },
                    )
                }

                // Pulsante "Concludi spesa" - nascosto se items vuoto
                if (items.isNotEmpty()) {
                    Button(
                        onClick = {
                            // Haptic feedback 15ms
                            performShortHapticFeedback(context)
                            onCheckout()
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.space2),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = tokens.bottomSheet.checkoutButtonBackground,
                            contentColor = tokens.bottomSheet.checkoutButtonContent,
                        ),
                        shape = tokens.bottomSheet.radiusTop,
                    ) {
                        Text(
                            text = "Concludi spesa",
                            style = typography.sectionHeader.copy(color = tokens.bottomSheet.checkoutButtonContent),
                        )
                    }
                }
            }
        }
    }
}
