package it.togo.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

private const val FORMAT_EXAMPLE = """[FRUTTA E VERDURA]
+ FRUTTA FRESCA
- MELE
  Golden Delicious 2 pz"""

/**
 * Schermata "Importa lista".
 *
 * L'utente incolla il testo prodotto dall'export di TOGO. Il pulsante analizza
 * il testo: se il formato non è valido compare una modale con il motivo, se è
 * valido compare la conferma con il numero di prodotti da importare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportListScreen(
    viewModel: ImportListViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    LaunchedEffect(uiState.successMessage) {
        val message = uiState.successMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSuccess()
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Impossibile importare", style = typography.itemTitle) },
            text = { Text(message, style = typography.itemMeta) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) {
                    Text("OK")
                }
            },
        )
    }

    uiState.confirmation?.let { plan ->
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isImporting) viewModel.cancelImport()
            },
            title = { Text("Conferma importazione", style = typography.itemTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    Text(
                        text = if (plan.productCount == 1) {
                            "Verrà importato 1 prodotto."
                        } else {
                            "Verranno importati ${plan.productCount} prodotti."
                        },
                        style = typography.itemMeta,
                    )
                    val creationParts = buildList {
                        if (plan.newProductCount > 0) {
                            add(
                                if (plan.newProductCount == 1) "1 prodotto"
                                else "${plan.newProductCount} prodotti"
                            )
                        }
                        if (plan.newLevel3Count > 0) {
                            add(
                                if (plan.newLevel3Count == 1) "1 categoria"
                                else "${plan.newLevel3Count} categorie"
                            )
                        }
                    }
                    if (creationParts.isNotEmpty()) {
                        Text(
                            text = "Nel catalogo verranno creati " +
                                creationParts.joinToString(" e ") + ".",
                            style = typography.caption,
                            color = colors.inkSecondary,
                        )
                    }
                    Text(
                        text = "I prodotti già presenti nella lista attiva verranno aggiornati.",
                        style = typography.caption,
                        color = colors.inkSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmImport,
                    enabled = !uiState.isImporting,
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.accentAction,
                        )
                    } else {
                        Text("Accetta")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelImport,
                    enabled = !uiState.isImporting,
                ) {
                    Text("Annulla")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importa lista", style = typography.sectionHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Indietro",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.inkPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Text(
                text = "Incolla il testo che hai esportato da TOGO. L'import legge " +
                    "prodotti, categorie, quantità e unità.",
                style = typography.itemMeta,
                color = colors.inkPrimary,
            )

            Text(
                text = "Formato atteso:",
                style = typography.caption,
                color = colors.inkSecondary,
            )

            Text(
                text = FORMAT_EXAMPLE,
                style = typography.itemMeta,
                color = colors.inkSecondary,
            )

            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                label = { Text("Testo della lista") },
                placeholder = { Text("Incolla qui il testo esportato...") },
                minLines = 8,
                maxLines = 16,
                enabled = !uiState.isAnalyzing && !uiState.isImporting,
            )

            if (uiState.isAnalyzing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.accentAction,
                    )
                    Text(
                        text = "Analisi del testo...",
                        style = typography.itemMeta,
                        color = colors.inkSecondary,
                    )
                }
            }

            Button(
                onClick = viewModel::analyze,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.text.isNotBlank() &&
                    !uiState.isAnalyzing &&
                    !uiState.isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentAction,
                    contentColor = colors.inkInverse,
                    disabledContainerColor = colors.surfaceSubtle,
                    disabledContentColor = colors.inkSecondary,
                ),
            ) {
                Text("Analizza e importa", style = typography.sectionHeader)
            }
        }
    }
}
