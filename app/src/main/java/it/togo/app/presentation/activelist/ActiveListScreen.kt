package it.togo.app.presentation.activelist

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.presentation.additem.AddItemBottomSheet
import it.togo.app.presentation.voice.VoiceBottomSheet
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveListScreen(
    viewModel: ActiveListViewModel,
    voiceViewModel: VoiceBottomSheetViewModel,
    onEvent: (UiEvent) -> Unit,
    // Iniettabile nei test; in produzione risolto da Koin.
    voiceGateway: it.togo.app.data.voice.AndroidVoiceRecognitionGateway =
        org.koin.compose.koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    val voiceUiState by voiceViewModel.uiState.collectAsState()

    val context = LocalContext.current
    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    // Richiesta runtime permesso RECORD_AUDIO per il riconoscimento vocale
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceViewModel.startListening()
        } else {
            val activity = context.findActivity()
            val permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO,
                )
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = if (permanentlyDenied) {
                        "Permesso microfono negato. Abilitalo nelle impostazioni."
                    } else {
                        "Permesso microfono necessario per usare il comando vocale"
                    },
                    actionLabel = if (permanentlyDenied) "Impostazioni" else null,
                    withDismissAction = permanentlyDenied,
                )
                if (permanentlyDenied && result == SnackbarResult.ActionPerformed) {
                    context.startActivity(appSettingsIntent(context.packageName))
                }
            }
        }
    }

    // Launcher per il risultato del riconoscimento vocale del sistema
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        voiceGateway.onActivityResult(result.resultCode, result.data)
    }
    DisposableEffect(voiceGateway) {
        voiceGateway.registerLauncher(speechLauncher)
        onDispose { voiceGateway.unregisterLauncher() }
    }

    // Lambda condivisa: avvia ascolto con controllo permesso
    val startVoiceWithPermission: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) voiceViewModel.startListening()
        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    if (showAddItemSheet || editingItem != null) {
        AddItemBottomSheet(
            onDismiss = {
                showAddItemSheet = false
                editingItem = null
            },
            editItem = editingItem,
        )
    }

    voiceViewModel.onTextConfirmed = { text ->
        onEvent(UiEvent.VoiceCommand(text))
    }

    VoiceBottomSheet(
        uiState = voiceUiState,
        onEdit = { newText -> voiceViewModel.editText(newText) },
        onConfirm = { voiceViewModel.confirm() },
        onRetry = { voiceViewModel.retry() },
        onCancel = { voiceViewModel.cancel() },
        onDismiss = { voiceViewModel.cancel() },
    )

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Spesa (${uiState.activeCount})", style = typography.sectionHeader) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(UiEvent.NavigateToHistory) }) {
                            Icon(Icons.Filled.History, contentDescription = "Storico")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val intent = viewModel.shareListIntent()
                                if (intent != null) {
                                    (context as? android.app.Activity)?.startActivity(
                                        Intent.createChooser(intent, "Condividi lista spesa")
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Condividi lista")
                        }
                        IconButton(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.inkPrimary,
                    )
                )
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        VoiceFab(
                            state = when {
                                voiceUiState is it.togo.app.presentation.voice.VoiceBottomSheetUiState.Listening -> it.togo.app.domain.voice.VoiceRecognitionState.Listening
                                voiceUiState is it.togo.app.presentation.voice.VoiceBottomSheetUiState.Processing -> it.togo.app.domain.voice.VoiceRecognitionState.Processing
                                voiceUiState is it.togo.app.presentation.voice.VoiceBottomSheetUiState.Ready -> it.togo.app.domain.voice.VoiceRecognitionState.Result("")
                                voiceUiState is it.togo.app.presentation.voice.VoiceBottomSheetUiState.Error -> it.togo.app.domain.voice.VoiceRecognitionState.Error(it.togo.app.domain.voice.ErrorCode.UNKNOWN, "")
                                else -> it.togo.app.domain.voice.VoiceRecognitionState.Idle
                            },
                            onClick = {
                                if (voiceUiState is it.togo.app.presentation.voice.VoiceBottomSheetUiState.Listening) {
                                    voiceViewModel.cancel()
                                } else {
                                    startVoiceWithPermission()
                                }
                            }
                        )

                        FloatingActionButton(
                            onClick = { showAddItemSheet = true },
                            containerColor = colors.accentAction,
                            contentColor = colors.inkInverse,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Aggiungi voce",
                                tint = colors.inkInverse,
                            )
                        }
                    }
                }
            },
            floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    uiState.checkoutSnackbarMessage?.let { checkoutSnackbarMessage ->
                        Snackbar(
                            modifier = Modifier.fillMaxWidth(),
                            action = if (uiState.checkedItemsForUndo.isNotEmpty()) {
                                {
                                    TextButton(
                                        onClick = { onEvent(UiEvent.UndoCheckout) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = tokens.snackbar.action,
                                        ),
                                    ) {
                                        Text("Annulla")
                                    }
                                }
                            } else {
                                null
                            },
                            containerColor = tokens.snackbar.container,
                            contentColor = tokens.snackbar.content,
                        ) {
                            Text(checkoutSnackbarMessage, style = typography.itemMeta)
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error.orEmpty(),
                            style = typography.itemMeta,
                            color = colors.warningInk,
                            modifier = Modifier.padding(spacing.space4)
                        )
                    } else if (uiState.activeGroups.isEmpty() && !uiState.hasCheckedItems) {
                        EmptyState(
                            onAddManual = {
                                showAddItemSheet = true
                            },
                            onVoice = startVoiceWithPermission,
                            colors = colors,
                            typography = typography,
                            spacing = spacing,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(spacing.space2)
                        ) {
                            items(uiState.activeGroups, key = { group -> group.level1.id.toString() }) { group ->
                                CategorySection(
                                    group = group,
                                    productNames = uiState.productNames,
                                    categoryLabels = uiState.categoryLabels,
                                    onCheckOff = { itemId, checked -> onEvent(UiEvent.CheckOff(itemId, checked)) },
                                    onEdit = { item -> editingItem = item },
                                    onDelete = { itemId -> onEvent(UiEvent.DeleteItem(itemId)) },
                                    colors = colors,
                                    typography = typography,
                                    spacing = spacing,
                                    tokens = tokens,
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space3))
                            }

                            if (uiState.hasCheckedItems) {
                                item {
                                    PresiSection(
                                        items = uiState.checkedItems,
                                        productNames = uiState.productNames,
                                        categoryLabels = uiState.categoryLabels,
                                        onUncheck = { itemId -> onEvent(UiEvent.CheckOff(itemId, false)) },
                                        onCheckout = { onEvent(UiEvent.Checkout) },
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
        )
}

/** Intent verso i dettagli dell'app, usato quando il permesso è negato per sempre. */
internal fun appSettingsIntent(packageName: String): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.parse("package:$packageName"),
)

/** Risale al Context dell'Activity per la rationale del permesso. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
