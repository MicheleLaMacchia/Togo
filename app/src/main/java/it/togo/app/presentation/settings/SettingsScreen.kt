package it.togo.app.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

private const val SECTION_MENU = "menu"
private const val SECTION_PRODUCTS = "products"
private const val SECTION_BRANDS = "brands"
private const val SECTION_VARIANTS = "variants"

/**
 * Menù Impostazioni.
 *
 * L'ingranaggio nella lista attiva apre questa schermata. Da qui si accede:
 * - ai dati creati dall'utente (prodotti, marche, varianti);
 * - alle regole apprese;
 * - alle informazioni sull'app.
 *
 * Le tre sezioni "I miei dati" sono gestite internamente: il menù resta la
 * schermata radice e il back da una sezione torna al menù, senza introdurre
 * nuove destinazioni nel navigatore principale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenLearnedRules: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    userDataViewModel: UserDataViewModel? = null,
) {
    var section by rememberSaveable { mutableStateOf(SECTION_MENU) }

    if (userDataViewModel != null && section != SECTION_MENU) {
        BackHandler { section = SECTION_MENU }

        when (section) {
            SECTION_PRODUCTS -> UserProductsScreen(
                viewModel = userDataViewModel,
                onBack = { section = SECTION_MENU },
            )

            SECTION_BRANDS -> UserAttributesScreen(
                viewModel = userDataViewModel,
                type = UserAttributeType.BRAND,
                onBack = { section = SECTION_MENU },
            )

            SECTION_VARIANTS -> UserAttributesScreen(
                viewModel = userDataViewModel,
                type = UserAttributeType.VARIANT,
                onBack = { section = SECTION_MENU },
            )
        }
        return
    }

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni", style = typography.sectionHeader) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            if (userDataViewModel != null) {
                Text(
                    text = "I miei dati",
                    style = typography.caption,
                    color = colors.inkSecondary,
                )

                SettingsMenuItem(
                    icon = Icons.Filled.ShoppingCart,
                    title = "I miei prodotti",
                    description = "Prodotti creati da te: modifica nome e categoria " +
                        "oppure eliminali.",
                    onClick = { section = SECTION_PRODUCTS },
                )

                SettingsMenuItem(
                    icon = Icons.Filled.Star,
                    title = "Le mie marche",
                    description = "Marche aggiunte da te. Quelle predefinite " +
                        "(Coop, Conad, ...) non compaiono qui.",
                    onClick = { section = SECTION_BRANDS },
                )

                SettingsMenuItem(
                    icon = Icons.Filled.List,
                    title = "Le mie varianti",
                    description = "Varianti aggiunte da te, oltre ai valori predefiniti.",
                    onClick = { section = SECTION_VARIANTS },
                )

                Text(
                    text = "Apprendimento",
                    style = typography.caption,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(top = spacing.space4),
                )
            } else {
                Text(
                    text = "Apprendimento",
                    style = typography.caption,
                    color = colors.inkSecondary,
                )
            }

            SettingsMenuItem(
                icon = Icons.Filled.School,
                title = "Regole apprese",
                description = "Visualizza, modifica, disattiva o elimina le regole " +
                    "create dalle correzioni di categoria.",
                onClick = onOpenLearnedRules,
            )

            Text(
                text = "Informazioni",
                style = typography.caption,
                color = colors.inkSecondary,
                modifier = Modifier.padding(top = spacing.space4),
            )

            SettingsMenuItem(
                icon = Icons.Filled.Info,
                title = "Informazioni su TOGO",
                description = "Versione dell'app, privacy e utilizzo dei dati.",
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = title,
                    style = typography.itemTitle,
                    color = colors.inkPrimary,
                )
                Text(
                    text = description,
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = colors.inkSecondary,
            )
        }
    }
}
