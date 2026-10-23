package it.togo.app.presentation.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Informazioni su TOGO.
 *
 * Mostra versione dell'app e note su privacy e riconoscimento vocale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember(context) { appVersionName(context) }
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informazioni su TOGO", style = typography.sectionHeader) },
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
                .padding(horizontal = spacing.space4, vertical = spacing.space5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceVariant,
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier
                        .padding(spacing.space4)
                        .size(40.dp),
                )
            }

            Text(
                text = "TOGO",
                style = typography.titleScreen,
                color = colors.inkPrimary,
            )

            Text(
                text = "La tua lista della spesa, semplice e locale.",
                style = typography.itemMeta,
                color = colors.inkSecondary,
                textAlign = TextAlign.Center,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = "Versione",
                        style = typography.caption,
                        color = colors.inkSecondary,
                    )
                    Text(
                        text = versionName,
                        style = typography.itemTitle,
                        color = colors.inkPrimary,
                    )
                }
            }

            InfoSection(
                title = "Privacy",
                body = "TOGO funziona interamente sul dispositivo. I dati di lista, " +
                    "storico, catalogo e regole apprese restano salvati in locale e non " +
                    "vengono inviati a servizi remoti.",
            )

            InfoSection(
                title = "Riconoscimento vocale",
                body = "Il riconoscimento vocale usa il servizio vocale disponibile sul " +
                    "dispositivo. TOGO non salva le registrazioni audio.",
            )
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    body: String,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = title,
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )
        Text(
            text = body,
            style = typography.itemMeta,
            color = colors.inkSecondary,
        )
    }
}

@Suppress("DEPRECATION")
private fun appVersionName(context: Context): String {
    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    return versionName?.takeIf { it.isNotBlank() } ?: "non disponibile"
}
