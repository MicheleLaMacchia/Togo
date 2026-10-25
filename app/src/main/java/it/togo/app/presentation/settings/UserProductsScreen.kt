package it.togo.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Sezione "I miei prodotti": elenca SOLO i prodotti creati dall'utente
 * (`is_user_defined = 1`) e permette di rinominarli, spostarli in un'altra
 * categoria o eliminarli dal catalogo.
 *
 * Eliminare un prodotto non tocca le voci già presenti nella lista attiva o
 * nello storico: sparisce dal catalogo e dalla ricerca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProductsScreen(
    viewModel: UserDataViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    var editingProduct by remember { mutableStateOf<CanonicalProduct?>(null) }
    var deletingProduct by remember { mutableStateOf<CanonicalProduct?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UserDataEvent.ProductUpdated,
                UserDataEvent.ProductDeleted -> {
                    editingProduct = null
                    deletingProduct = null
                }

                is UserDataEvent.Error -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    editingProduct?.let { product ->
        EditProductDialog(
            product = product,
            categories = uiState.categories,
            onConfirm = { name, level3Id ->
                viewModel.updateProduct(product.id, name, level3Id)
            },
            onDismiss = { editingProduct = null },
        )
    }

    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Elimina prodotto", style = typography.itemTitle) },
            text = {
                Text(
                    text = "«${product.name}» verrà rimosso dal catalogo e non sarà più " +
                        "ricercabile.\n\nLe voci già presenti nella lista o nello storico " +
                        "non vengono modificate.",
                    style = typography.itemMeta,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProduct(product.id) }) {
                    Text("Elimina", color = colors.warningInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) {
                    Text("Annulla")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei prodotti", style = typography.sectionHeader) },
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
        if (uiState.products.isEmpty()) {
            EmptyProductsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.space4),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                item {
                    Text(
                        text = "Solo i prodotti creati da te. Il catalogo preinstallato " +
                            "non è modificabile da qui.",
                        style = typography.caption,
                        color = colors.inkSecondary,
                        modifier = Modifier.padding(bottom = spacing.space2),
                    )
                }
                items(uiState.products, key = { it.id }) { product ->
                    UserProductRow(
                        product = product,
                        onEdit = { editingProduct = product },
                        onDelete = { deletingProduct = product },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProductRow(
    product: CanonicalProduct,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val path = categoryPath(product)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                start = spacing.space3,
                top = spacing.space2,
                bottom = spacing.space2,
                end = spacing.space1,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = product.name,
                    style = typography.itemTitle,
                    color = colors.inkPrimary,
                )
                Text(
                    text = path,
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Modifica ${product.name}",
                    tint = colors.accent,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Elimina ${product.name}",
                    tint = colors.warningInk,
                )
            }
        }
    }
}

@Composable
private fun EditProductDialog(
    product: CanonicalProduct,
    categories: List<UserCategoryOption>,
    onConfirm: (String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    var name by remember(product.id) { mutableStateOf(product.name) }
    var selectedLevel3Id by remember(product.id) { mutableStateOf(product.level3Id) }
    var query by remember(product.id) { mutableStateOf("") }

    val filteredCategories = remember(categories, query) {
        val normalizedQuery = ProductNameNormalizer.normalize(query)
        if (normalizedQuery.isEmpty()) {
            categories
        } else {
            categories.filter {
                ProductNameNormalizer.normalize(it.label).contains(normalizedQuery)
            }
        }
    }
    val selectedLabel = categories.firstOrNull { it.id == selectedLevel3Id }?.label
        ?: categoryPath(product)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica prodotto", style = typography.itemTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome", style = typography.caption) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Categoria: $selectedLabel",
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                )

                // La tassonomia può essere lunga: il filtro evita di scorrere
                // decine di categorie per trovare quella giusta.
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cerca categoria", style = typography.caption) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                ) {
                    items(filteredCategories, key = { it.id }) { option ->
                        val selected = option.id == selectedLevel3Id
                        TextButton(
                            onClick = { selectedLevel3Id = option.id },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = option.label,
                                style = typography.itemMeta,
                                color = if (selected) colors.accent else colors.inkPrimary,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedLevel3Id) },
                enabled = name.isNotBlank() && selectedLevel3Id > 0,
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun EmptyProductsState(modifier: Modifier = Modifier) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            spacing.space2,
            Alignment.CenterVertically,
        ),
    ) {
        Text(
            text = "Nessun prodotto creato da te",
            style = typography.itemTitle,
            color = colors.inkPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "I prodotti nuovi che aggiungi con il pulsante + compaiono qui.",
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun categoryPath(product: CanonicalProduct): String =
    listOfNotNull(product.level1Name, product.level2Name, product.level3Name)
        .joinToString(" › ")
