package it.togo.app.presentation.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoComponentTokens
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypographyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Schermata di ricerca nel catalogo con evidenziazione dei termini trovati.
 *
 * - Campo ricerca con debounce 300ms
 * - Highlighting termini trovati (background accentHighlight, bold)
 * - Gestione ambiguità (lista candidati)
 * - Stati: vuoto, loading, risultati, nessun risultato, errore
 * - Touch target ≥48dp su controlli
 */
@Composable
fun SearchScreen(onEvent: (SearchUiEvent) -> Unit) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SearchViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val colors = togoColors()
    val typography = togoTypographyTokens()
    val spacing = togoSpacing()
    val tokens = togoComponentTokens()

    val textFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SearchUiEvent.QueryChanged(""))
        delay(300)
        textFieldFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing[2]),
    ) {
        // Campo ricerca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing[4])
                .background(
                    color = colors.surfaceCard,
                    shape = RoundedCornerShape(tokens.itemRow.radius),
                )
                .border(
                    width = 1.dp,
                    color = colors.borderHairline,
                    shape = RoundedCornerShape(tokens.itemRow.radius),
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Cerca",
                tint = colors.inkMuted,
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = spacing[3]),
            )

            TextField(
                value = uiState.query,
                onValueChange = { viewModel.onEvent(SearchUiEvent.QueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing[3],
                        vertical = spacing[2],
                    )
                    .focusRequester(textFieldFocusRequester),
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
                    errorIndicatorColor = Color.Transparent,
                ),
            )
        }

        // Lista risultati
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing[4]),
            verticalArrangement = Arrangement.spacedBy(spacing[2]),
        ) {
            items(uiState.results, key = { it.id }) { product ->
                SearchResultItem(
                    product = product,
                    query = uiState.query,
                    onClick = { onEvent(SearchUiEvent.ItemSelected(product)) },
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
    val typography = togoTypographyTokens()
    val spacing = togoSpacing()
    val tokens = togoComponentTokens()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing[2], vertical = spacing[1])
            .combinedClickable(onClick = onClick)
            .clip(RoundedCornerShape(tokens.itemRow.radius)),
        shape = RoundedCornerShape(tokens.itemRow.radius),
        border = BorderStroke(tokens.itemRow.borderWidth, colors.borderCrisp),
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
                    text = buildAnnotatedStringHighlight(product.name, query),
                    style = typography.itemName,
                    color = colors.inkPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Meta opzionale: solo nome per ora
                if (product.name.isNotBlank()) {
                    Text(
                        text = product.name,
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
 * Ogni occurrenza di query (case-insensitive) nel testo viene evidenziata
 * con background accentHighlight e grassetto.
 */
fun buildAnnotatedStringHighlight(text: String, searchQuery: String): AnnotatedString {
    if (text.isBlank() || searchQuery.isBlank()) {
        return AnnotatedString(text)
    }

    val normalizedSearch = searchQuery.lowercase()
    val lowerText = text.lowercase()
    val builder = AnnotatedString.Builder(text)

    var index = 0
    while (true) {
        index = lowerText.indexOf(normalizedSearch, index)
        if (index == -1) break

        builder.addStyle(
            SpanStyle(
                backgroundColor = Color(0xFF2563EB), // accentHighlight
                fontWeight = FontWeight.Bold,
            ),
            index,
            index + searchQuery.length,
        )
        index += searchQuery.length
    }

    return builder.toAnnotatedString()
}