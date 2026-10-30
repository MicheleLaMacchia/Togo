package it.togo.app.presentation.activelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.core.TogoLog
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.product.ProductUnitResolver
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.domain.time.Clock
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * ViewModel della lista attiva.
 *
 * Dalla Story 8.7 [ListItemRepository] è l'unica fonte di verità: la lista
 * mostra solo righe `TO_BUY`, la spunta chiama `complete` e la riga passa a
 * `PURCHASED` (visibile nello storico). Non esistono più "Presi", checkout e
 * undo checkout.
 */
class ActiveListViewModel(
    private val listItemRepository: ListItemRepository,
    private val catalogRepository: CatalogRepository,
    private val clock: Clock,
) : ViewModel() {

    private val shareListUseCase = ShareListUseCase(listItemRepository, catalogRepository)
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

    private val _deletedItemForUndo = MutableStateFlow<ListItem?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    init {
        loadTaxonomy()
        observeActiveItems()
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

    private fun observeActiveItems() {
        viewModelScope.launch {
            combine(
                listItemRepository.observeActive(),
                taxonomyCache.filterNotNull()
            ) { items, taxonomy ->
                buildUiState(items, taxonomy)
            }
                .onEach { newState ->
                    _uiState.value = newState
                }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun buildUiState(
        items: List<ListItem>,
        taxonomy: TaxonomyCache
    ): ActiveListUiState {
        val (l1List, l2List, l3List) = taxonomy

        val l1ById = l1List.associateBy { it.id }
        val l2ById = l2List.associateBy { it.id }
        val l3ById = l3List.associateBy { it.id }

        // Tutte le righe ricevute sono TO_BUY: non c'è più separazione tra
        // "attivi" e "spuntati".
        val itemsByL3 = mutableMapOf<Long, MutableList<ListItem>>()
        val productNames = mutableMapOf<Long, String>()
        val categoryLabels = mutableMapOf<Long, String>()
        for (item in items) {
            val product = catalogRepository.getById(item.productId) ?: continue
            productNames[item.productId] = product.name
            categoryLabels[item.productId] = canonicalCategoryLabel(product, l1ById, l2ById, l3ById)
            val l3Id = product.level3Id
            if (l3List.any { it.id == l3Id }) {
                itemsByL3.getOrPut(l3Id) { mutableListOf() }.add(item)
            }
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
            // Apre il bottom sheet di inserimento manuale: gestito dalla UI.
            is UiEvent.AddItemManual -> { }
            is UiEvent.UndoDelete -> handleUndoDelete()
        }
    }

    suspend fun shareListIntent(): android.content.Intent? {
        val intent = shareListUseCase()
        TogoLog.d(
            if (intent == null) {
                "ShareList: nessun articolo attivo, intent nullo"
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

                val item = ListItem.create(
                    productId = product.id,
                    quantity = quantity,
                    unit = unit.name,
                    createdAt = clock.nowMillis(),
                    variant = variant,
                )

                withContext(Dispatchers.IO) {
                    listItemRepository.add(item)
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

    private fun findItem(itemId: String): ListItem? {
        return _uiState.value.activeGroups
            .flatMap { group -> group.level2Groups.flatMap { l2 -> l2.level3Groups.flatMap { l3 -> l3.items } } }
            .find { it.id == itemId }
    }

    private fun handleCheckOff(itemId: String, isChecked: Boolean) {
        // La lista attiva mostra solo TO_BUY: la deselezione non esiste più.
        if (!isChecked) return
        findItem(itemId) ?: return

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

            val completed = withContext(Dispatchers.IO) {
                listItemRepository.complete(itemId)
            }
            if (completed == null) {
                // Riga non più completabile (già rimossa/eliminata): lo stato
                // si riallinea alla prossima emissione di observeActive().
                _uiState.value = _uiState.value.copy(
                    error = "Voce non più disponibile",
                )
            }
            delay(300)
            _uiState.value = _uiState.value.copy(checkingOffItemId = null)
        }
    }

    private fun handleDeleteItem(itemId: String) {
        val item = findItem(itemId) ?: return

        _deletedItemForUndo.value = item

        viewModelScope.launch {
            try {
                val deleted = withContext(Dispatchers.IO) {
                    listItemRepository.delete(itemId)
                }
                if (deleted == null) {
                    _deletedItemForUndo.value = null
                    _uiState.value = _uiState.value.copy(
                        error = "Voce non più eliminabile",
                    )
                } else {
                    showUndoSnackbar("Voce eliminata")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Eliminazione fallita: ${e.message}")
            }
        }
    }

    private fun handleUndoDelete() {
        _deletedItemForUndo.value?.let { item ->
            viewModelScope.launch {
                val restored = withContext(Dispatchers.IO) {
                    listItemRepository.restore(item.id)
                }
                if (restored == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Ripristino non riuscito",
                    )
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

    private data class TaxonomyCache(
        val l1: List<TaxonomyLevel.Level1>,
        val l2: List<TaxonomyLevel.Level2>,
        val l3: List<TaxonomyLevel.Level3>,
    )
}
