package it.togo.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ResolutionResult
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Schermata ricerca catalogo con risoluzione sinonimi.
 *
 * - Campo ricerca con debounce 300ms
 * - Highlighting termini trovati (background secondaryContainer, bold)
 * - Gestione ambiguità (lista candidati)
 * - Stati: vuoto, loading, risultati, nessun risultato
 */
@Composable
fun SearchScreen(onEvent: (SearchUiEvent) -> Unit) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SearchViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        // Header con titolo
        Text(
            text = "Cerca nel catalogo",
            style = typography.titleScreen,
            color = colors.inkPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space4, vertical: spacing.space3),
        )

        // Campo ricerca
        var searchQuery by remember { mutableStateOf("") }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal: spacing.space4)
                .background(
                    color = colors.surfaceCard,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.itemRow.radius),
                )
                .border(
                    width = 1.dp,
                    color = colors.borderHairline,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.itemRow.radius),
                ),
            color = colors.surfaceCard,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Search,
                    contentDescription = "Cerca",
                    tint = colors.inkMuted,
                    modifier = Modifier.size(24.dp),
                )

                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onEvent(SearchUiEvent.QueryChanged(it))
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Cerca prodotto...",
                            style = typography.itemName,
                            color = colors.inkMuted,
                        )
                    },
                    singleLine = true,
                    textStyle = typography.itemName,
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        textColor = colors.inkPrimary,
                        cursorColor = colors.accentHighlight,
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        onEvent(SearchUiEvent.Clear)
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Clear,
                            contentDescription = "Pulisci",
                            tint = colors.inkMuted,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        // Lista risultati
        if (uiState.hasResults) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                items(searchResults, key = { it.id }) { product ->
                    SearchResultItem(
                        product = product,
                        query = searchQuery,
                        onClick = { onEvent(SearchUiEvent.ItemSelected(product)) },
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space2))
                }
            }
        } else if (uiState.hasQuery) {
            // Stato vuoto con query ma nessun risultato
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.SearchOff,
                    contentDescription = "Nessun risultato",
                    tint = colors.inkMuted,
                    modifier = Modifier.size(64.dp),
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space3))
                Text(
                    text = "Nessun risultato per \"${searchQuery}\"",
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Item risultato ricerca con highlighting termini */
@Composable
fun SearchResultItem(
    product: CanonicalProduct,
    query: String,
    onClick: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    val highlightedName = buildAnnotatedStringHighlight(product.name, query)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .combinedClickable(onClick = onClick)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(tokens.itemRow.radius)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.itemRow.radius),
        border = androidx.compose.ui.graphics.BorderStroke(tokens.itemRow.borderWidth, colors.borderCrisp),
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.checkboxUtility.radius),
                    )
                    .border(tokens.checkboxUtility.borderWidth, colors.borderHairline, androidx.compose.foundation.shape.RoundedCornerShape(tokens.checkboxUtility.radius))
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(tokens.checkboxUtility.radius)),
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
                val highlightedName = buildAnnotatedStringHighlight(product.name, query)
                Text(
                    text = highlightedName,
                    style = typography.itemName,
                    color = colors.inkPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = TextDecoration.None,
                )

                // Meta opzionale: brand + variante + condition
                val metaParts = buildList<String> {
                    if (product.brand?.isNotBlank() == true) add(product.brand)
                    if (product.variant?.isNotBlank() == true) add(product.variant)
                    if (product.condition?.isNotBlank() == true) add(product.condition)
                }
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

/**
 * Costruisce AnnotatedString con highlighting dei termini della query.
 * Supporta query multi-termine (es. "latte intero" → highlight "latte" e "intero" separatamente).
 * Usa la stessa normalizzazione NFKD del backend per consistenza (es. "caffè" → "cafe").
 */
fun buildAnnotatedStringHighlight(text: String, searchQuery: String): AnnotatedString {
    if (text.isBlank() || searchQuery.isBlank()) {
        return AnnotatedString(text)
    }

    // Usa la stessa normalizzazione del backend per consistenza
    val normalizedSearch = it.togo.app.data.database.CatalogQueries.normalize(searchQuery)
    val lowerText = it.togo.app.data.database.CatalogQueries.normalize(text)
    val builder = AnnotatedString.Builder(text)

    // Supporta query multi-termine (es. "latte intero" → highlight "latte" e "intero" separatamente)
    val searchTerms = normalizedSearch.split(" ").filter { it.isNotBlank() }.toSet()

    var lastMatchEnd = -1
    for (term in searchTerms) {
        var index = 0
        while (true) {
            index = text.lowercase().indexOf(term, index)
            if (index == -1) break

            // Evita sovrapposizioni con match precedenti
            if (index >= lastMatchEnd) {
                builder.addStyle(
                    SpanStyle(
                        backgroundColor = colors.searchHighlightBackground ?: colors.accentHighlight,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    index,
                    index + term.length
                )
                lastMatchEnd = index + term.length
            }
            index += term.length
        }
    }

    return builder.toAnnotatedString()
}