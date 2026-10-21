package it.togo.app.presentation.additem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.togo.app.domain.model.ResolutionOption
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoComponentTokens
import it.togo.app.ui.theme.togoTypography

/**
 * Dialog di risoluzione duplicati.
 *
 * Mostra un messaggio chiaro con la quantità attuale e la nuova,
 * e offre 3 azioni esplicite più l'annullamento.
 *
 * @property productName Nome del prodotto duplicato
 * @property currentQuantity Quantità attualmente in lista
 * @property newQuantity Nuova quantità da aggiungere
 * @property canSum Se true, l'opzione "Somma" è disponibile (unità compatibili)
 * @property onSelect Callback con la scelta dell'utente
 */
@Composable
fun DuplicateResolutionDialog(
    productName: String,
    currentQuantity: Double,
    newQuantity: Double,
    canSum: Boolean = true,
    onSelect: (ResolutionOption) -> Unit,
) {
    // Testi esposti anche come contentDescription per TalkBack/test.
    val title = "Prodotto già in lista"
    val message = "\"$productName\" è già presente con $currentQuantity. " +
        "Nuova quantità: $newQuantity. Cosa vuoi fare?"

    val colors = togoColors()
    val typography = togoTypography()
    val tokens = togoComponentTokens()

    Dialog(onDismissRequest = { onSelect(ResolutionOption.CANCEL) }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = tokens.duplicateDialog.radius,
            color = tokens.duplicateDialog.background,
            contentColor = colors.inkPrimary,
            border = BorderStroke(
                tokens.duplicateDialog.borderWidth,
                tokens.duplicateDialog.borderColor,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Titolo
                Text(
                    text = title,
                    style = typography.titleScreen,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = title },
                )

                // Messaggio descrittivo
                Text(
                    text = message,
                    style = typography.itemName,
                    color = colors.inkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = message },
                )

                // Azioni
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Somma (primaria, solo se disponibile)
                    if (canSum) {
                        Button(
                            onClick = { onSelect(ResolutionOption.SUM) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "Somma quantità (+$newQuantity → ${currentQuantity + newQuantity})",
                                style = typography.itemMeta,
                            )
                        }
                    }

                    // Sostituisci (secondaria)
                    OutlinedButton(
                        onClick = { onSelect(ResolutionOption.REPLACE) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Sostituisci quantità ($newQuantity)",
                            style = typography.itemMeta,
                        )
                    }

                    // Separa (secondaria)
                    OutlinedButton(
                        onClick = { onSelect(ResolutionOption.SEPARATE) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Crea voce separata",
                            style = typography.itemMeta,
                        )
                    }

                    // Annulla (terziaria)
                    TextButton(
                        onClick = { onSelect(ResolutionOption.CANCEL) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Annulla",
                            style = typography.itemMeta,
                            color = colors.inkSecondary,
                        )
                    }
                }
            }
        }
    }
}