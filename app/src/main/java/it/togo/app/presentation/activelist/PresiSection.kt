package it.togo.app.presentation.activelist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
@Composable
fun PresiSection(
    items: List<ShoppingItem>,
    onUncheck: (String) -> Unit,
    onCheckout: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                .border(bottom = androidx.compose.ui.graphics.BorderStroke(1.dp, colors.borderHairline))
                .heightIn(min = 48.dp) // Touch target minimo 48dp
            ,
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
                    androidx.compose.material.icons.Icons.Filled.ExpandLess
                else
                    androidx.compose.material.icons.Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collassa" else "Espandi",
                tint = colors.inkSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.CenterEnd),
            )
        }
            .fillMaxWidth()
            .background(colors.surfaceSubtle)
            .border(bottom = androidx.compose.ui.graphics.BorderStroke(1.dp, colors.borderHairline))
            .combinedClickable(onClick = { expanded = !expanded })

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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                items.forEach { item ->
                    ItemRow(
                        item = item, // Usa lo stato reale dell'item
                        onCheckChange = { checked ->
                            if (!checked) onUncheck(item.id) // Uncheck = move back to active
                        },
                        onClick = { /* no-op in presa - could enable edit later */ },
                        onDelete = { /* no-op in presa - could enable delete later */ },
                    )
                }
            }
            .padding(horizontal = spacing.space2)
            .padding(bottom = spacing.space3)

            // Pulsante "Concludi spesa"
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space2),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = tokens.bottomSheet.checkoutButtonBackground,
                    contentColor = tokens.bottomSheet.checkoutButtonContent,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.bottomSheet.radiusTop),
            ) {
                Text(
                    text = "Concludi spesa",
                    style = typography.sectionHeader.copy(color = colors.inkInverse),
                )
            }
                .padding(bottom = spacing.space2)
        }
    }
}