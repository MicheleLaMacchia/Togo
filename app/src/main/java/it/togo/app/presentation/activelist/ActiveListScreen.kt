package it.togo.app.presentation.activelist

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AccessibilityLiveRegion
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import it.togo.app.core.TogoLog
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.presentation.additem.AddItemBottomSheet
import it.togo.app.presentation.voice.VoiceBottomSheet
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import it.togo.app.presentation.voice.VoiceConfirmationSheet
import it.togo.app.presentation.voice.VoiceConfirmationViewModel
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Lista Attiva con due pagine: Spesa (sinistra) e Da fare (destra).
 *
 * L'intestazione fissa contiene il titolo della pagina corrente e le frecce
 * sempre annunciate da TalkBack. Il pager non intercetta lo swipe sulle righe:
 * lì il gesto resta quello del cestino. Lo swipe cambia pagina solo se inizia
 * sull'intestazione, su un'intestazione di gruppo o su uno spazio non occupato
 * da righe (AD-8).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActiveListScreen(
    viewModel: ActiveListViewModel,
    voiceViewModel: VoiceBottomSheetViewModel,
    voiceConfirmationViewModel: VoiceConfirmationViewModel,
    onEvent: (UiEvent) -> Unit,
    // Iniettabile nei test; in produzione risolto da Koin.
    voiceGateway: it.togo.app.data.voice.AndroidVoiceRecognitionGateway =
        org.koin.compose.koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Traccia se lo snackbar ha il focus di accessibilità: usata dal timer di
    // auto-dimissione dello snackbar undo azione (pausa su focus/hover).
    var snackbarFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Un solo punto di visualizzazione per il checkout: la snackbar passa
    // dall'host Material invece di essere composta nel contenuto. Evita due
    // testi sovrapposti nella stessa posizione (vedi segnalazione 22/10).
    LaunchedEffect(uiState.checkoutSnackbarMessage, uiState.actionSnackbarMessage) {
        val checkoutMessage = uiState.checkoutSnackbarMessage
        val actionMessage = uiState.actionSnackbarMessage
        val message = checkoutMessage ?: actionMessage ?: return@LaunchedEffect
        val isCheckout = checkoutMessage != null
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (isCheckout && uiState.checkedItemsForUndo.isNotEmpty()) "Annulla" else null,
            // Il timer dei 5 s del checkout resta nel ViewModel; quando azzera
            // il messaggio questo effetto viene cancellato e la snackbar si
            // chiude. I messaggi del foglio azione hanno durata breve.
            duration = if (isCheckout) SnackbarDuration.Indefinite else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            onEvent(UiEvent.UndoCheckout)
        }
    }

    /**
     * Snackbar undo per eliminazione di una singola azione.
     *
     * Durata ≥10 s, pausa su focus/hover (il timer si ferma mentre lo snackbar
     * ha il focus di accessibilità), live region educata e tasto "Annulla"
     * con touch target ≥48dp.
     */
    LaunchedEffect(uiState.actionDeleteUndoCounter) {
        val message = uiState.actionDeleteUndoMessage ?: return@LaunchedEffect
        var finished = false

        // Auto-dimissione dopo 10 secondi. Il timer non avanza mentre lo
        // snackbar ha il focus di accessibilità (TalkBack esplora il
        // messaggio o il tasto Annulla), così l'utente ha sempre almeno
        // 10 s per annullare l'eliminazione.
        scope.launch {
            var elapsed = 0L
            while (!finished && elapsed < 10000) {
                delay(200)
                if (!snackbarFocused) elapsed += 200
            }
            if (!finished) {
                finished = true
                snackbarHostState.dismissCurrentSnackbar()
                viewModel.clearActionUndoSnackbar()
            }
        }

        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Annulla",
            duration = SnackbarDuration.Indefinite,
        )

        finished = true
        if (result == SnackbarResult.ActionPerformed) {
            onEvent(UiEvent.UndoDeleteAction)
        }
        viewModel.clearActionUndoSnackbar()
    }

    val colors = togoColors()
    val typography = togoTypography()
    val tokens = componentTokens()

    val voiceUiState by voiceViewModel.uiState.collectAsState()
    val voiceConfirmationUiState by voiceConfirmationViewModel.uiState.collectAsState()
    val voicePrefillRequest by viewModel.voicePrefillRequest.collectAsState()

    val context = LocalContext.current
    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    // Comando vocale da precompilare nella sheet di aggiunta. Null per
    // l'aggiunta manuale o la modifica di una voce esistente.
    var voicePrefill by remember { mutableStateOf<ParsedCommand?>(null) }

    // Foglio azione "Cose da fare" (Story 8.5): aperto dal FAB (+) sulla
    // pagina Da fare; lo stato è locale perché il foglio è legato alla UI.
    var showActionSheet by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionItem?>(null) }

    // Se il comando confermato non è univoco, il ViewModel chiede di aprire la
    // scheda di aggiunta precompilata. I comandi univoci vengono invece inseriti
    // direttamente in lista.
    LaunchedEffect(voicePrefillRequest) {
        val request = voicePrefillRequest ?: return@LaunchedEffect
        voicePrefill = request
        showAddItemSheet = true
        viewModel.consumeVoicePrefillRequest()
    }

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
                voicePrefill = null
            },
            editItem = editingItem,
            initialVoiceCommand = voicePrefill,
        )
    }

    if (showActionSheet) {
        ActionSheet(
            onDismiss = {
                showActionSheet = false
                editingAction = null
            },
            editingAction = editingAction,
            onConfirm = { text, scope, urgency ->
                val action = editingAction
                if (action != null) {
                    viewModel.updateAction(action, text, scope, urgency)
                } else {
                    viewModel.addAction(text, scope, urgency)
                }
                showActionSheet = false
                editingAction = null
            },
        )
    }

    DisposableEffect(voiceViewModel, voiceConfirmationViewModel) {
        voiceViewModel.onTextConfirmed = { text ->
            // La conferma del testo grezzo apre il riepilogo strutturato:
            // l'utente vede prodotto, variante e quantità prima di toccare
            // la lista.
            voiceConfirmationViewModel.showForRecognition(text)
        }
        onDispose {
            voiceViewModel.onTextConfirmed = null
        }
    }

    DisposableEffect(voiceConfirmationViewModel, viewModel) {
        voiceConfirmationViewModel.onCommandConfirmed = { command ->
            // Conferma esplicita: se il comando è univoco la voce entra subito
            // in lista, senza riaprire il form e senza chiedere una seconda
            // conferma. In caso ambiguo il ViewModel emette voicePrefillRequest.
            viewModel.addVoiceCommand(command)
        }
        onDispose {
            voiceConfirmationViewModel.onCommandConfirmed = null
        }
    }

    VoiceBottomSheet(
        uiState = voiceUiState,
        onEdit = { newText -> voiceViewModel.editText(newText) },
        onConfirm = { voiceViewModel.confirm() },
        onRetry = { voiceViewModel.retry() },
        onCancel = { voiceViewModel.cancel() },
        onDismiss = { voiceViewModel.cancel() },
    )

    VoiceConfirmationSheet(
        uiState = voiceConfirmationUiState,
        onConfirm = { voiceConfirmationViewModel.confirm() },
        onEdit = { newText -> voiceConfirmationViewModel.editText(newText) },
        onCancel = { voiceConfirmationViewModel.cancel() },
        onDismiss = { voiceConfirmationViewModel.cancel() },
    )

    // La pagina corrente vive nel ViewModel: qui la si riflette solo sul pager.
    val page = uiState.currentPage
    val pagerState = rememberPagerState(
        initialPage = page.ordinal,
        pageCount = { ActiveListPage.entries.size },
    )
    LaunchedEffect(page) {
        pagerState.animateScrollToPage(page.ordinal)
    }

    // Lo swipe non è gestito dal pager (userScrollEnabled = false): viene
    // riconosciuto solo dalle zone che possono cambiare pagina. Con due sole
    // pagine qualsiasi direzione porta all'altra pagina.
    val pagerSwipeModifier = rememberPagerSwipeModifier(
        onSwipe = { onEvent(UiEvent.SelectPage(page.other())) },
    )

    val shareList: () -> Unit = {
        TogoLog.d("TopBar: tap su Condividi")
        scope.launch {
            val intent = viewModel.shareListIntent()
            val activity = context.findActivity()
            when {
                intent == null -> {
                    TogoLog.w(
                        "Condividi: nessun articolo attivo da condividere",
                    )
                    snackbarHostState.showSnackbar(
                        "Nessun articolo da condividere: " +
                            "la lista attiva è vuota"
                    )
                }

                activity == null -> {
                    TogoLog.w(
                        "Condividi: context senza Activity, chooser non avviato",
                    )
                    snackbarHostState.showSnackbar(
                        "Impossibile aprire la condivisione"
                    )
                }

                else -> {
                    TogoLog.d("Condividi: apertura chooser di sistema")
                    activity.startActivity(
                        Intent.createChooser(
                            intent,
                            "Condividi lista spesa",
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ActiveListHeader(
                page = page,
                activeCount = uiState.activeCount,
                activeActionsCount = uiState.activeActionsCount,
                swipeModifier = pagerSwipeModifier,
                onEvent = onEvent,
                onShare = shareList,
            )
        },
        floatingActionButton = {
            // Il FAB è contestuale: sulla pagina Spesa aggiunge una voce della
            // spesa, sulla pagina Da fare apre il foglio azione (Story 8.5).
            when (page) {
                ActiveListPage.SHOPPING -> {
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
                                onClick = {
                                    voicePrefill = null
                                    showAddItemSheet = true
                                },
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
                }

                ActiveListPage.ACTIONS -> FloatingActionButton(
                    onClick = {
                        voicePrefill = null
                        showActionSheet = true
                    },
                    containerColor = colors.accentAction,
                    contentColor = colors.inkInverse,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Aggiungi azione",
                        tint = colors.inkInverse,
                    )
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val actionLabel = data.visuals.actionLabel
                Box(
                    modifier = Modifier
                        .semantics {
                            // Live region educata: l'annuncio non interrompe
                            // l'utente, viene letto al prossimo passaggio.
                            accessibilityLiveRegion = AccessibilityLiveRegion.Polite
                            mergeDescendants = true
                        }
                        .onFocusChanged { snackbarFocused = it.isFocused },
                ) {
                    Snackbar(
                        action = if (actionLabel != null) {
                            {
                                TextButton(
                                    onClick = { data.performAction() },
                                    modifier = Modifier.sizeIn(
                                        minWidth = 48.dp,
                                        minHeight = 48.dp,
                                    ),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = tokens.snackbar.action,
                                    ),
                                ) {
                                    Text(actionLabel)
                                }
                            }
                        } else {
                            null
                        },
                        dismissAction = if (data.visuals.withDismissAction) {
                            {
                                IconButton(onClick = { data.dismiss() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Chiudi",
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        containerColor = tokens.snackbar.container,
                        contentColor = tokens.snackbar.content,
                    ) {
                        Text(data.visuals.message, style = typography.itemMeta)
                    }
                }
            }
        },
        content = { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                // Le righe consumano il drag orizzontale per il cestino: il
                // pager non deve mai contenderselo. Lo swipe di pagina passa
                // da pagerSwipeModifier, applicato solo alle zone consentite.
                userScrollEnabled = false,
                key = { index -> ActiveListPage.entries[index].name },
            ) { index ->
                when (ActiveListPage.entries[index]) {
                    ActiveListPage.SHOPPING -> ShoppingPage(
                        uiState = uiState,
                        pagerSwipeModifier = pagerSwipeModifier,
                        onEvent = onEvent,
                        onAddManual = {
                            voicePrefill = null
                            showAddItemSheet = true
                        },
                        onVoice = startVoiceWithPermission,
                        onEdit = { item ->
                            voicePrefill = null
                            editingItem = item
                        },
                    )

                    ActiveListPage.ACTIONS -> ActionsPage(
                        uiState = uiState,
                        pagerSwipeModifier = pagerSwipeModifier,
                        onEvent = onEvent,
                        onEditAction = { action ->
                            editingAction = action
                            showActionSheet = true
                        },
                    )
                }
            }
        },
    )
}

/** Intestazione fissa: titolo dinamico, frecce e azioni globali. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveListHeader(
    page: ActiveListPage,
    activeCount: Int,
    activeActionsCount: Int,
    swipeModifier: Modifier,
    onEvent: (UiEvent) -> Unit,
    onShare: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val count = if (page == ActiveListPage.SHOPPING) activeCount else activeActionsCount

    TopAppBar(
        modifier = swipeModifier,
        title = {
            Text(
                text = "${page.label} ($count)",
                style = typography.sectionHeader,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { onEvent(UiEvent.SelectPage(ActiveListPage.SHOPPING)) },
                enabled = page == ActiveListPage.ACTIONS,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Pagina precedente",
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onEvent(UiEvent.SelectPage(ActiveListPage.ACTIONS)) },
                enabled = page == ActiveListPage.SHOPPING,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Pagina successiva",
                )
            }
            // Storico resta solo sulla pagina Spesa.
            if (page == ActiveListPage.SHOPPING) {
                IconButton(onClick = {
                    TogoLog.d("TopBar: tap su Cronologia")
                    onEvent(UiEvent.NavigateToHistory)
                }) {
                    Icon(Icons.Filled.History, contentDescription = "Storico")
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Condividi lista")
            }
            IconButton(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surface,
            titleContentColor = colors.inkPrimary,
        ),
    )
}

/** Pagina Spesa: contenuto invariato rispetto alla lista a pagina singola. */
@Composable
private fun ShoppingPage(
    uiState: ActiveListUiState,
    pagerSwipeModifier: Modifier,
    onEvent: (UiEvent) -> Unit,
    onAddManual: () -> Unit,
    onVoice: () -> Unit,
    onEdit: (ShoppingItem) -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shopping_page")
            .then(pagerSwipeModifier),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        if (uiState.error != null) {
            Text(
                text = uiState.error.orEmpty(),
                style = typography.itemMeta,
                color = colors.warningInk,
                modifier = Modifier.padding(spacing.space4),
            )
        } else if (uiState.activeGroups.isEmpty() && !uiState.hasCheckedItems) {
            EmptyState(
                onAddManual = onAddManual,
                onVoice = onVoice,
                colors = colors,
                typography = typography,
                spacing = spacing,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                items(uiState.activeGroups, key = { group -> group.level1.id.toString() }) { group ->
                    CategorySection(
                        group = group,
                        productNames = uiState.productNames,
                        categoryLabels = uiState.categoryLabels,
                        onCheckOff = { itemId, checked -> onEvent(UiEvent.CheckOff(itemId, checked)) },
                        onEdit = onEdit,
                        onDelete = { itemId -> onEvent(UiEvent.DeleteItem(itemId)) },
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                        tokens = tokens,
                    )
                    Spacer(modifier = Modifier.padding(spacing.space3))
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

/**
 * Pagina Da fare (storie 8.3/8.4).
 *
 * Le azioni attive sono raggruppate per ambito e ordinate per urgenza e
 * alfabeticamente. Ogni riga è una [ActionRow]: checkbox a sinistra, testo al
 * centro, badge urgenza e ambito sotto il testo, accento verticale. Le azioni
 * non sono prodotti: nessun badge quantità, marca, variante o unità.
 */
@Composable
private fun ActionsPage(
    uiState: ActiveListUiState,
    pagerSwipeModifier: Modifier,
    onEvent: (UiEvent) -> Unit,
    onEditAction: (ActionItem) -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("actions_page")
            .then(pagerSwipeModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.actions.isEmpty() && uiState.checkedActions.isEmpty()) {
            Text(
                text = "Nessuna azione da fare",
                style = typography.itemName,
                color = colors.inkSecondary,
                modifier = Modifier.padding(spacing.space4),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = spacing.space2)
                    .testTag("actions_list"),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                uiState.actionGroups.forEach { group ->
                    // L'ambito è un header; l'urgenza resta un criterio di
                    // ordinamento e vive nel badge della riga.
                    item(key = "action_scope_${group.scope.name}") {
                        Text(
                            text = group.scope.label,
                            style = typography.sectionHeader,
                            color = colors.inkPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = spacing.space2,
                                    vertical = spacing.space1,
                                )
                                .testTag("action_scope_header_${group.scope.name}"),
                        )
                    }

                    items(
                        items = group.actions,
                        key = { action -> action.id },
                    ) { action ->
                        ActionRow(
                            action = action,
                            onCheckChange = { checked ->
                                onEvent(UiEvent.CheckOff(action.id, checked))
                            },
                            onClick = { onEditAction(action) },
                            onDelete = { onEvent(UiEvent.DeleteAction(action.id)) },
                            rowActionDescription = "Doppio tocco per modificare.",
                        )
                    }
                }

                // Sezione "Fatte" in fondo: le azioni spuntate restano visibili
                // e barrate, dalla più recente; si possono solo deselezionare.
                if (uiState.checkedActions.isNotEmpty()) {
                    item(key = "fatto_section") {
                        FatteSection(
                            items = uiState.checkedActions,
                            onUncheck = { actionId ->
                                onEvent(UiEvent.CheckOff(actionId, false))
                            },
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

/**
 * Riconosce uno swipe orizzontale in una zona che può cambiare pagina.
 *
 * Il pager ha `userScrollEnabled = false`: questo modificatore è applicato solo
 * alla pagina (intestazione, intestazioni di gruppo e spazi non occupati da
 * righe). Le righe attive consumano il drag prima che arrivi qui, quindi il loro
 * swipe resta il gesto del cestino (AD-8).
 */
@Composable
private fun rememberPagerSwipeModifier(onSwipe: () -> Unit): Modifier {
    // La lambda viene aggiornata senza riavviare il gesto: una ricomposizione
    // durante il drag non deve cancellare lo swipe in corso.
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    return remember {
        Modifier.pointerInput(Unit) {
            val threshold = 48.dp.toPx()
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    if (abs(totalDrag) >= threshold) currentOnSwipe()
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f },
            )
        }
    }
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
