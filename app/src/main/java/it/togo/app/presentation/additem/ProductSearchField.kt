package it.togo.app.presentation.additem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.CatalogCategoryResult
import it.togo.app.presentation.components.UserDefinedBadge
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.util.Locale

/**
 * Campo ricerca prodotto con suggerimenti autocomplete.
 *
 * - TextField con icona ricerca
 * - LazyColumn suggerimenti sotto (max 8)
 * - Tap su risultato → ProductSelected
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<CanonicalProduct>,
    categoryResults: List<CatalogCategoryResult>,
    activeCategory: CatalogCategoryResult?,
    isSearching: Boolean,
    onProductClick: (CanonicalProduct) -> Unit,
    onCategoryClick: (CatalogCategoryResult) -> Unit,
    onClearCategoryFilter: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()
    var isFocused by remember { mutableStateOf(false) }

    val textFieldFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val textFieldFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        // Campo ricerca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space2)
                .background(
                    color = colors.surfaceCard,
                    shape = tokens.itemRow.radius,
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) colors.accentHighlight else colors.borderHairline,
                    shape = tokens.itemRow.radius,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Cerca",
                tint = colors.inkMuted,
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = spacing.space3),
            )

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing.space3,
                        vertical = spacing.space2,
                    )
                    .focusRequester(textFieldFocusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder = {
                    Text(
                        "Cerca prodotto...",
                        style = typography.itemName,
                        color = colors.inkSecondary,
                    )
                },
                singleLine = true,
                textStyle = typography.itemName,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary,
                    cursorColor = colors.accentHighlight,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { textFieldFocusManager.clearFocus() },
                ),
            )

            if (isSearching) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = spacing.space3),
                    color = colors.accentHighlight,
                    strokeWidth = 2.dp,
                )
            }
        }

        // Filtro categoria attivo: la lista sottostante sono i suoi prodotti.
        if (activeCategory != null) {
            ActiveCategoryHeader(
                category = activeCategory,
                onClear = onClearCategoryFilter,
                colors = colors,
                typography = typography,
                spacing = spacing,
                tokens = tokens,
            )
        }

        // Suggerimenti: prima i prodotti, poi le categorie dopo un divisorio.
        val shownCategories = if (activeCategory == null) categoryResults.take(8).distinctBy { it.id } else emptyList()
        if (results.isNotEmpty() || shownCategories.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space2)
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                if (results.isNotEmpty()) {
                    if (shownCategories.isNotEmpty()) {
                        item(key = "header-products") {
                            SectionLabel(
                                text = "Prodotti",
                                colors = colors,
                                typography = typography,
                                spacing = spacing,
                            )
                        }
                    }
                    items(
                        items = results.take(8).distinctBy { it.id },
                        key = { "product-${it.id}" },
                    ) { product ->
                        ProductSuggestionItem(
                            product = product,
                            onClick = { onProductClick(product) },
                            colors = colors,
                            typography = typography,
                            spacing = spacing,
                            tokens = tokens,
                        )
                    }
                }
                if (shownCategories.isNotEmpty()) {
                    item(key = "divider-categories") {
                        SectionDivider(
                            label = "Categorie",
                            colors = colors,
                            typography = typography,
                            spacing = spacing,
                        )
                    }
                    items(
                        items = shownCategories,
                        key = { "category-${it.id}" },
                    ) { category ->
                        CategorySuggestionItem(
                            category = category,
                            onClick = { onCategoryClick(category) },
                            colors = colors,
                            typography = typography,
                            spacing = spacing,
                            tokens = tokens,
                        )
                    }
                }
            }
        }
    }
}

/** Intestazione di sezione (es. "Prodotti"). */
@Composable
private fun SectionLabel(
    text: String,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = spacing.space1, vertical = spacing.space1),
        style = typography.caption,
        color = colors.inkSecondary,
    )
}

/** Divisorio con etichetta centrale: separa i prodotti dalle categorie. */
@Composable
private fun SectionDivider(
    label: String,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space1, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.borderHairline),
        )
        Text(
            text = label,
            style = typography.caption,
            color = colors.inkSecondary,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.borderHairline),
        )
    }
}

/** Riga con la categoria filtrata e il comando per tornare alla ricerca. */
@Composable
private fun ActiveCategoryHeader(
    category: CatalogCategoryResult,
    onClear: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2)
            .background(colors.surfaceSubtle, tokens.itemRow.radius)
            .padding(start = spacing.space3, end = spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Categoria: ${category.name}",
            modifier = Modifier.weight(1f),
            style = typography.itemMeta,
            color = colors.inkPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        TextButton(
            onClick = onClear,
            modifier = Modifier.heightIn(min = spacing.touchTargetMin),
        ) {
            Text(
                text = "Mostra tutti",
                style = typography.caption,
                color = colors.accentAction,
            )
        }
    }
}

/** Singolo risultato di tipo categoria (Livello 3). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategorySuggestionItem(
    category: CatalogCategoryResult,
    onClick: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    val path = listOfNotNull(category.level1Name, category.level2Name).joinToString(" › ")
    val countLabel = if (category.productCount == 1) {
        "1 prodotto"
    } else {
        "${category.productCount} prodotti"
    }
    val meta = if (path.isBlank()) countLabel else "$path · $countLabel"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.touchTargetMin)
            .background(colors.surfaceSubtle, tokens.itemRow.radius)
            .border(1.dp, colors.borderHairline, tokens.itemRow.radius)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = spacing.space3, vertical = spacing.space2),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = category.name,
                style = typography.itemName,
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = typography.caption,
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

/** Singolo elemento suggerimento */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductSuggestionItem(
    product: CanonicalProduct,
    onClick: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    val level3Name = product.level3Name ?: "Sconosciuto"
    val level2Name = product.level2Name ?: ""
    val level1Name = product.level1Name ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space2)
            .background(colors.surfaceCard, tokens.itemRow.radius)
            .border(1.dp, colors.borderHairline, tokens.itemRow.radius)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = spacing.space3, vertical = spacing.space3),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(
                    text = product.name,
                    modifier = Modifier.weight(1f),
                    style = typography.itemName,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (product.isUserDefined) {
                    UserDefinedBadge()
                }
            }

            // Path tassonomico
            val path = buildList<String> {
                if (level1Name.isNotBlank()) add(level1Name)
                if (level2Name.isNotBlank()) add(level2Name)
                add(level3Name)
            }
            if (path.isNotEmpty()) {
                Text(
                    text = path.joinToString(" → "),
                    style = typography.caption,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
