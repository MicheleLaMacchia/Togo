package it.togo.app.presentation.activelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.core.TogoLog
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.product.ProductUnitResolver
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.usecase.ResolveVoiceProductUseCase
import it.togo.app.domain.usecase.ShareListUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class ActiveListViewModel(
    private val shoppingRepository: ShoppingListRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val shareListUseCase = ShareListUseCase(shoppingRepository, catalogRepository)
    private val resolveVoiceProduct = ResolveVoiceProductUseCase(catalogRepository)

    /**
     * Comando vocale confermato che non identifica un prodotto univoco (o ha
     * unità non compatibili): la UI apre la scheda di aggiunta già precompilata.
     */
    private val _voicePrefillRequest = MutableStateFlow<ParsedCommand?>(null)
    val voicePrefillRequest = _voicePrefillRequest.asStateFlow()

    private val _uiState = MutableStateFlow(ActiveListUiState())
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ActiveListUiState())

    private val taxonomyCache = MutableStateFlow<TaxonomyCache?>(null)

    private val _snackbarCounter = AtomicInteger(0)
    private val _checkoutSnackbarCounter = AtomicInteger(0)

    private var _isCheckingOut = false
    private val _deletedItemForUndo = MutableStateFlow<ShoppingItem?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    init {
        loadTaxonomy()
        observeAllItems()
    }

    private fun loadTaxonomy() {
        viewModelScope.launch {
            catalogRepository.getTaxonomy()
                .onEach { levels ->
                    val l1 = levels.filterIsInstance<TaxonomyLevel.Level1>().sortedBy { it.sortOrder }
                    val l2 = levels.filterIsInstance<TaxonomyLevel.Level2>().sortedBy { it.sortOrder }
                    val l3 = levels.filterIsInstance<TaxonomyLevel.Level3>().sortedBy { it.sortOrder }
                    taxonomyCache.value = TaxonomyCache(l1, l2, l3)
                }
                .launchIn(viewModelScope)
        }
    }

    private fun observeAllItems() {
        viewModelScope.launch {
            combine(
                shoppingRepository.getAllItems(),
                taxonomyCache.filterNotNull()
            ) { items, taxonomy ->
                buildUiState(items, taxonomy)
            }
                .onEach { newState ->
                    // I campi transitori (snackbar/undo) non arrivano dal DB:
                    // vanno preservati, altrimenti l'emissione Room successiva
                    // al checkout cancellerebbe subito il messaggio e l'undo.
                    val current = _uiState.value
                    _uiState.value = newState.copy(
                        checkoutSnackbarMessage = current.checkoutSnackbarMessage,
                        checkedItemsForUndo = current.checkedItemsForUndo,
                    )
                }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun buildUiState(
        items: List<ShoppingItem>,
        taxonomy: TaxonomyCache
    ): ActiveListUiState {
        val (l1List, l2List, l3List) = taxonomy

        val l1ById = l1List.associateBy { it.id }
        val l2ById = l2List.associateBy { it.id }
        val l3ById = l3List.associateBy { it.id }

        // Una sola emissione: ogni voce è in esattamente una delle due liste.
        val activeOnly = items.filterNot { it.isChecked }
        val checkedItems = items.filter { it.isChecked }

        val itemsByL3 = mutableMapOf<Long, MutableList<ShoppingItem>>()
        val productNames = mutableMapOf<Long, String>()
        val categoryLabels = mutableMapOf<Long, String>()
        for (item in activeOnly) {
            val product = catalogRepository.getById(item.productId) ?: continue
            productNames[item.productId] = product.name
            categoryLabels[item.productId] = canonicalCategoryLabel(product, l1ById, l2ById, l3ById)
            val l3Id = product.level3Id
            if (l3List.any { it.id == l3Id }) {
                itemsByL3.getOrPut(l3Id) { mutableListOf() }.add(item)
            }
        }

        // Le voci spuntate non entrano nei gruppi attivi, ma servono nome e
        // categoria nella sezione "Presi".
        for (item in checkedItems) {
            if (productNames.containsKey(item.productId)) continue
            val product = catalogRepository.getById(item.productId) ?: continue
            productNames[item.productId] = product.name
            categoryLabels[item.productId] = canonicalCategoryLabel(product, l1ById, l2ById, l3ById)
        }

        itemsByL3.values.forEach { list -> list.sortBy { item -> item.productId } }

        val groups = mutableListOf<CategoryGroup>()
        for (l1 in l1List) {
            val l2s = l2List.filter { it.level1Id == l1.id }
            val l2Groups = mutableListOf<Level2Group>()
            for (l2 in l2s) {
                val l3s = l3List.filter { it.level2Id == l2.id }
                val l3Groups = mutableListOf<Level3Group>()
                for (l3 in l3s) {
                    val l3Items = itemsByL3[l3.id].orEmpty()
                    if (l3Items.isNotEmpty()) {
                        l3Groups.add(Level3Group(l3, l3Items))
                    }
                }
                if (l3Groups.isNotEmpty()) {
                    l2Groups.add(Level2Group(l2, l3Groups))
                }
            }
            if (l2Groups.isNotEmpty()) {
                groups.add(CategoryGroup(l1, l2Groups))
            }
        }

        return ActiveListUiState(
            activeGroups = groups,
            checkedItems = checkedItems,
            productNames = productNames,
            categoryLabels = categoryLabels,
            error = null
        )
    }

    /**
     * Etichetta canonica leggibile della categoria: "L1, L2, L3".
     *
     * Riempie sempre una stringa non vuota (fallback localizzato) anche se la
     * tassonomia non è disponibile o il prodotto non ha un L3 valido.
     */
    private fun canonicalCategoryLabel(
        product: CanonicalProduct,
        l1ById: Map<Long, TaxonomyLevel.Level1>,
        l2ById: Map<Long, TaxonomyLevel.Level2>,
        l3ById: Map<Long, TaxonomyLevel.Level3>
    ): String {
        val l3 = l3ById[product.level3Id]
        val l2 = l3?.let { l2ById[it.level2Id] }
        val l1 = l2?.let { l1ById[it.level1Id] }
        return when {
            l1 != null && l2 != null && l3 != null -> "${l1.name}, ${l2.name}, ${l3.name}"
            l2 != null && l3 != null -> "${l2.name}, ${l3.name}"
            l3 != null -> l3.name
            else -> "Categoria non assegnata"
        }
    }

    fun onEvent(event: UiEvent) {
        TogoLog.d("ActiveList.onEvent: $event")
        when (event) {
            is UiEvent.CheckOff -> handleCheckOff(event.itemId, event.isChecked)
            // Legacy: il testo vocale confermato passa da VoiceConfirmationSheet
            // alla sheet di aggiunta; qui non si scrive nulla.
            is UiEvent.VoiceCommand -> { }
            is UiEvent.DeleteItem -> handleDeleteItem(event.itemId)
            // Apre il foglio vocale: gestito direttamente dalla UI.
            is UiEvent.NavigateToVoice -> { }
            // Navigazione: gestita da AppNavigator prima di arrivare qui.
            is UiEvent.NavigateToHistory -> { }
            // Navigazione: gestita da AppNavigator prima di arrivare qui.
            is UiEvent.NavigateToSettings -> { }
            // La UI chiama direttamente shareListIntent().
            is UiEvent.ShareList -> { }
            is UiEvent.Checkout -> handleCheckout()
            // Apre il bottom sheet di inserimento manuale: gestito dalla UI.
            is UiEvent.AddItemManual -> { }
            is UiEvent.UndoDelete -> handleUndoDelete()
            is UiEvent.UndoCheckout -> handleUndoCheckout()
        }
    }

    suspend fun shareListIntent(): android.content.Intent? {
        val intent = shareListUseCase()
        TogoLog.d(
            if (intent == null) {
                "ShareList: nessun articolo non spuntato, intent nullo"
            } else {
                "ShareList: intent pronto"
            }
        )
        return intent
    }

    /**
     * Aggiunge direttamente alla lista un comando vocale già confermato.
     *
     * Se prodotto, quantità e unità sono univoci e compatibili, la voce viene
     * salvata subito: l'utente ha già dato la conferma esplicita nel riepilogo
     * vocale. Se il comando è ambiguo, incompleto o l'unità non è compatibile,
     * viene richiesta la scheda di aggiunta precompilata tramite
     * [voicePrefillRequest], così l'utente può scegliere/correggere.
     */
    fun addVoiceCommand(command: ParsedCommand) {
        viewModelScope.launch {
            try {
                val resolution = resolveVoiceProduct(command)
                val product = resolution.products.singleOrNull()
                val quantity = command.quantity
                val unit = command.unit

                if (product == null ||
                    !command.isComplete ||
                    quantity == null ||
                    !quantity.isFinite() ||
                    quantity <= 0.0 ||
                    unit == null ||
                    !supportsUnit(product, unit)
                ) {
                    _voicePrefillRequest.value = command
                    return@launch
                }

                val variant = ProductNameNormalizer.capitalizeFirstWord(command.variant.orEmpty())
                    .takeIf { it.isNotBlank() }
                    ?.takeUnless { productNameContains(product.name, it) }

                val item = ShoppingItem(
                    id = UUID.randomUUID().toString(),
                    productId = product.id,
                    quantity = quantity,
                    unit = unit,
                    variant = variant,
                    isChecked = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )

                withContext(Dispatchers.IO) {
                    shoppingRepository.insert(item)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Aggiunta vocale fallita: ${e.message}",
                )
            }
        }
    }

    /** La UI ha aperto la scheda precompilata: il comando è stato consumato. */
    fun consumeVoicePrefillRequest() {
        _voicePrefillRequest.value = null
    }

    private fun supportsUnit(product: CanonicalProduct, unit: StandardUnit): Boolean {
        val compatible = product.compatibleUnits
            .ifEmpty { ProductUnitResolver.forProduct(product) }
            .ifEmpty { listOf(StandardUnit.PIECE) }
        return compatible.any { it.isCompatibleWith(unit) }
    }

    private fun productNameContains(productName: String, value: String): Boolean {
        val normalizedName = ProductNameNormalizer.normalize(productName)
        val normalizedValue = ProductNameNormalizer.normalize(value)
        return normalizedValue.isNotEmpty() && normalizedName.contains(normalizedValue)
    }

    private fun findItem(itemId: String): ShoppingItem? {
        return _uiState.value.activeGroups
            .flatMap { group -> group.level2Groups.flatMap { l2 -> l2.level3Groups.flatMap { l3 -> l3.items } } }
            .find { it.id == itemId }
            ?: _uiState.value.checkedItems.find { it.id == itemId }
    }

    private fun handleCheckOff(itemId: String, isChecked: Boolean) {
        val item = findItem(itemId) ?: return

        if (isChecked) {
            _uiState.value = _uiState.value.copy(pendingCheckOffItemId = itemId)

            viewModelScope.launch {
                delay(300)
                _uiState.value = _uiState.value.copy(
                    activeGroups = _uiState.value.activeGroups.map { group ->
                        group.copy(
                            level2Groups = group.level2Groups.map { l2 ->
                                l2.copy(
                                    level3Groups = l2.level3Groups
                                        .map { l3 ->
                                            l3.copy(items = l3.items.filter { it.id != itemId })
                                        }
                                        .filter { it.items.isNotEmpty() }
                                )
                            }.filter { it.level3Groups.isNotEmpty() }
                        )
                    },
                    pendingCheckOffItemId = null,
                    checkingOffItemId = itemId
                )

                withContext(Dispatchers.IO) {
                    shoppingRepository.update(item.copyWith(isChecked = true))
                }
                delay(300)
                _uiState.value = _uiState.value.copy(checkingOffItemId = null)
            }
        } else {
            val updated = item.copyWith(isChecked = false)
            _uiState.value = _uiState.value.copy(
                activeGroups = _uiState.value.activeGroups.map { group ->
                    group.copy(
                        level2Groups = group.level2Groups.map { l2 ->
                            l2.copy(
                                level3Groups = l2.level3Groups.map { l3 ->
                                    if (l3.items.any { it.id == itemId }) {
                                        l3.copy(items = (l3.items + updated).sortedBy { it.productId })
                                    } else {
                                        l3
                                    }
                                }.filter { it.items.isNotEmpty() }
                            )
                        }.filter { it.level3Groups.isNotEmpty() }
                    )
                },
                checkedItems = _uiState.value.checkedItems.filter { it.id != itemId }
            )

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    shoppingRepository.update(updated)
                }
            }
        }
    }

    private fun handleDeleteItem(itemId: String) {
        val item = findItem(itemId) ?: return

        _deletedItemForUndo.value = item

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shoppingRepository.delete(itemId)
                }
                showUndoSnackbar("Voce eliminata")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Eliminazione fallita: ${e.message}")
            }
        }
    }

    private fun handleUndoDelete() {
        _deletedItemForUndo.value?.let { item ->
            val restored = item.copyWith()
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    shoppingRepository.insert(restored)
                }
            }
            _deletedItemForUndo.value = null
        }
        _snackbarMessage.value = null
    }

    private fun showUndoSnackbar(message: String) {
        val currentCount = _snackbarCounter.incrementAndGet()
        _snackbarMessage.value = message
        viewModelScope.launch {
            delay(5000)
            if (_snackbarCounter.get() == currentCount && _snackbarMessage.value != null) {
                _snackbarMessage.value = null
            }
        }
    }

    private fun showCheckoutSnackbar(message: String) {
        val currentCount = _checkoutSnackbarCounter.incrementAndGet()
        _uiState.value = _uiState.value.copy(checkoutSnackbarMessage = message)
        viewModelScope.launch {
            delay(5000)
            if (_checkoutSnackbarCounter.get() == currentCount && _uiState.value.checkoutSnackbarMessage != null) {
                _uiState.value = _uiState.value.copy(checkoutSnackbarMessage = null)
            }
        }
    }

    /**
     * Conclude la spesa: sposta atomicamente le voci spuntate ("Presi") nello
     * storico e le rimuove dalla lista attiva (spec 2-5).
     *
     * La transazione è in `ShoppingListRepository.checkout`: se fallisce le
     * voci restano in "Presi" e viene mostrato un errore.
     */
    private fun handleCheckout() {
        if (_isCheckingOut) return
        _isCheckingOut = true

        // Le voci da concludere sono quelle spuntate, non quelle attive:
        // il pulsante vive nella sezione "Presi".
        val items = _uiState.value.checkedItems

        if (items.isEmpty()) {
            _isCheckingOut = false
            _uiState.value = _uiState.value.copy(checkedItemsForUndo = emptyList())
            showCheckoutSnackbar("Nessuna voce in 'Presi' da concludere")
            return
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shoppingRepository.checkout(items.map { it.id })
                }
                TogoLog.i("Checkout: ${items.size} voci spostate nello storico")
                _uiState.value = _uiState.value.copy(checkedItemsForUndo = items)
                showCheckoutSnackbar("Spesa conclusa (${items.size} voci)")
            } catch (e: Exception) {
                TogoLog.e("Checkout fallito", e)
                _uiState.value = _uiState.value.copy(
                    error = "Checkout fallito: ${e.message}",
                    checkedItemsForUndo = emptyList(),
                )
            } finally {
                _isCheckingOut = false
            }
        }
    }

    /**
     * Annulla il checkout: le voci erano state rimosse dalla lista attiva dalla
     * transazione, quindi vanno reinserite (non aggiornate) come non spuntate.
     *
     * Le righe di storico restano registrate (scelta di prodotto, spec 2-5).
     */
    private fun handleUndoCheckout() {
        val items = _uiState.value.checkedItemsForUndo
        if (items.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            checkedItemsForUndo = emptyList(),
            checkoutSnackbarMessage = null,
        )

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    items.forEach { item ->
                        shoppingRepository.insert(item.copyWith(isChecked = false))
                    }
                }
                TogoLog.i("Undo checkout: ${items.size} voci ripristinate in lista")
            } catch (e: Exception) {
                TogoLog.e("Undo checkout fallito", e)
                _uiState.value = _uiState.value.copy(
                    error = "Annulla checkout fallito: ${e.message}",
                )
            }
        }
    }

    private data class TaxonomyCache(
        val l1: List<TaxonomyLevel.Level1>,
        val l2: List<TaxonomyLevel.Level2>,
        val l3: List<TaxonomyLevel.Level3>,
    )
}
