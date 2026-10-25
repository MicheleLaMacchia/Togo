package it.togo.app.presentation.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.category.AssignmentSource
import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.CatalogCategoryResult
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.domain.product.ProductUnitResolver
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.UserAttributeRepository
import it.togo.app.domain.usecase.CorrectCategoryUseCase
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.CreateLevel3Result
import it.togo.app.domain.usecase.CreateProductResult
import it.togo.app.domain.usecase.CreateUserLevel3UseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel per il Bottom Sheet "Aggiungi/Modifica Voce Manuale".
 *
 * Supporta due modalità:
 * - INSERT: nuova voce (default)
 * - EDIT: modifica voce esistente (pre-popolato, step 2 default)
 *
 * Gestisce:
 * - Ricerca prodotti con debounce 300ms
 * - Validazione step-by-step
 * - Mapping a ShoppingItem su conferma (insert o update)
 */
class AddItemViewModel(
    private val catalogRepository: CatalogRepository,
    private val shoppingRepository: ShoppingListRepository,
    private val createProductUseCase: CreateCanonicalProductUseCase,
    private val createLevel3UseCase: CreateUserLevel3UseCase,
    private val editItem: ShoppingItem? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val correctCategoryUseCase: CorrectCategoryUseCase? = null,
    private val userAttributeRepository: UserAttributeRepository? = null,
) : ViewModel() {

    // Flag per modalità edit - ora parte dello UI state
    val isEditMode: Boolean = editItem != null

    // ---- State ----
    private val _uiState = MutableStateFlow(AddItemUiState(isEditMode = editItem != null))

    /** Emesso dopo un insert/update riuscito; la sheet si chiude. */
    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()
    val uiState = _uiState
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), AddItemUiState(isEditMode = editItem != null))

    // ---- Ricerca prodotti con debounce 300ms ----
    private val _searchQuery = MutableStateFlow("")
    val searchResults = _searchQuery
        .debounce(300) // debounce 300ms
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length >= 2) {
                // Prodotti e categorie viaggiano insieme: i primi restano in
                // testa, le categorie vengono mostrate dopo un divisorio.
                combine(
                    catalogRepository.search(query),
                    searchCategoriesSafely(query),
                ) { products, categories -> products to categories }
                    .onStart { _uiState.update { it.copy(isLoading = true) } }
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
                    .catch { error ->
                        _uiState.update { it.copy(isLoading = false, errors = it.errors + ("search" to (error.message ?: "Errore ricerca"))) }
                        emit(emptyList<CanonicalProduct>() to emptyList<CatalogCategoryResult>())
                    }
                    .onEach { (products, categories) ->
                        _uiState.update { state ->
                            // Con un filtro categoria attivo la lista mostra i
                            // prodotti della categoria, non i risultati testuali.
                            if (state.activeCategory != null) {
                                state.copy(isLoading = false)
                            } else {
                                state.copy(
                                    searchResults = products,
                                    categoryResults = categories,
                                    isLoading = false,
                                )
                            }
                        }
                    }
            } else {
                kotlinx.coroutines.flow.flowOf(
                    emptyList<CanonicalProduct>() to emptyList<CatalogCategoryResult>()
                )
                    .onEach {
                        _uiState.update { state ->
                            if (state.activeCategory != null) {
                                state.copy(isLoading = false)
                            } else {
                                state.copy(
                                    searchResults = emptyList(),
                                    categoryResults = emptyList(),
                                    isLoading = false,
                                )
                            }
                        }
                    }
            }
        }
        .stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            emptyList<CanonicalProduct>() to emptyList<CatalogCategoryResult>(),
        )

    init {
        searchResults.launchIn(viewModelScope)
        _searchQuery.value = ""
        loadTaxonomy()
        observeAttributeSuggestions()
        if (isEditMode) {
            initializeEditMode()
        } else {
            _searchQuery.value = ""
        }
    }

    /**
     * Osserva le marche/varianti aggiunte dall'utente. In produzione il
     * registro `USER_ATTRIBUTE` è la fonte dei suggerimenti, così eliminare un
     * valore dalle Impostazioni lo rimuove davvero anche dai suggerimenti.
     * Senza registro (test/fake) si ricade sul comportamento storico basato su
     * lista attiva + storico.
     */
    private fun observeAttributeSuggestions() {
        viewModelScope.launch {
            try {
                val suggestions = userAttributeRepository?.observeSuggestions()
                    ?: shoppingRepository.getAttributeSuggestions()
                suggestions.collect { values ->
                    _uiState.update {
                        it.copy(
                            brandSuggestions = values.brands,
                            variantSuggestions = values.variants,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Suggerimenti best-effort: il form resta usabile senza.
            }
        }
    }

    /**
     * Precompila marca/variante/conservazione con l'ultimo uso dello stesso
     * prodotto, così riaggiungendo una voce già conosciuta la si ritrova.
     */
    private fun prefillLastAttributes(productId: Long) {
        viewModelScope.launch {
            try {
                val attributes = shoppingRepository.getLastAttributes(productId)
                    ?: return@launch
                _uiState.update { state ->
                    if (state.selectedProduct?.id != productId) return@update state
                    state.copy(
                        brand = state.brand.ifBlank { attributes.brand.orEmpty() },
                        variant = state.variant.ifBlank { attributes.variant.orEmpty() },
                        condition = state.condition.ifBlank { attributes.condition.orEmpty() },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Precompilazione best-effort.
            }
        }
    }

    /** Carica la tassonomia per il form prodotto e per la correzione categoria. */
    private fun loadTaxonomy() {
        viewModelScope.launch {
            try {
                val levels = catalogRepository.getTaxonomy().first()
                _uiState.update { it.copy(taxonomy = levels) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // La UI mostra un errore solo quando serve davvero (creazione o correzione).
            }
        }
    }

    /** Inizializza stato per modalità edit */
    private fun initializeEditMode() {
        editItem?.let { item ->
            // Trova prodotto canonico per l'item
            viewModelScope.launch {
                try {
                    val product = catalogRepository.getById(item.productId)
                        ?: throw IllegalStateException("Prodotto non più disponibile nel catalogo")
                    
                    val compatible = compatibleUnitsFor(product)
                        .toMutableList()
                        .apply { if (item.unit !in this) add(item.unit) }

                    _uiState.update {
                        it.copy(
                            selectedProduct = product,
                            searchResults = emptyList(),
                            searchQuery = product.name,
                            compatibleUnits = compatible,
                            selectedUnit = item.unit, // Singola assegnazione: usa l'unità dell'item in edit
                            quantityText = formatQuantityText(item.quantity),
                            brand = item.brand ?: "",
                            variant = item.variant ?: "",
                            condition = item.condition ?: "",
                            currentStep = 2, // In modifica si parte da step 2 (quantità/unità)
                            errors = emptyMap(),
                            isCategoryCorrectionVisible = false,
                            categoryCorrectionError = null,
                            categoryCorrectionMessage = null,
                            pendingCategoryCorrection = null,
                            pendingCategoryAssignment = null,
                            correctedCategoryPath = null,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errors = it.errors + ("product" to "Errore caricamento prodotto: ${e.message}")) }
                }
            }
        }
    }

    /**
     * Le categorie sono un extra: se il repository non le implementa (fake dei
     * test, versioni precedenti) la ricerca dei prodotti deve continuare a
     * funzionare invece di fallire in blocco.
     */
    private fun searchCategoriesSafely(query: String): Flow<List<CatalogCategoryResult>> {
        val flow = try {
            catalogRepository.searchCategories(query)
        } catch (e: Exception) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return flow.catch { emit(emptyList()) }
    }

    /** Handle UI events */
    fun onEvent(event: AddItemUiEvent) {
        when (event) {
            is AddItemUiEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is AddItemUiEvent.ProductSelected -> handleProductSelected(event.product)
            is AddItemUiEvent.CategorySelected -> handleCategorySelected(event.category)
            is AddItemUiEvent.ClearCategoryFilter -> handleClearCategoryFilter()
            is AddItemUiEvent.QuantityChanged -> handleQuantityChanged(event.quantityText)
            is AddItemUiEvent.UnitChanged -> handleUnitChanged(event.unit)
            is AddItemUiEvent.BrandChanged -> handleBrandChanged(event.brand)
            is AddItemUiEvent.VariantChanged -> handleVariantChanged(event.variant)
            is AddItemUiEvent.ConditionChanged -> handleConditionChanged(event.condition)
            is AddItemUiEvent.NextStep -> handleNextStep()
            is AddItemUiEvent.PreviousStep -> handlePreviousStep()
            is AddItemUiEvent.Confirm -> handleConfirm()
            is AddItemUiEvent.Cancel -> handleCancel()
            is AddItemUiEvent.Reset -> handleReset()
            is AddItemUiEvent.ShowNewProductForm -> handleShowNewProductForm()
            is AddItemUiEvent.HideNewProductForm -> handleHideNewProductForm()
            is AddItemUiEvent.NewProductNameChanged -> handleNewProductNameChanged(event.name)
            is AddItemUiEvent.NewProductLevel1Selected -> handleNewProductLevel1Selected(event.level1Id)
            is AddItemUiEvent.NewProductLevel2Selected -> handleNewProductLevel2Selected(event.level2Id)
            is AddItemUiEvent.NewProductLevel3Selected -> handleNewProductLevel3Selected(event.level3Id)
            is AddItemUiEvent.CreateNewProduct -> handleCreateNewProduct()
            is AddItemUiEvent.ConfirmDuplicateProduct -> handleConfirmDuplicate()
            is AddItemUiEvent.DismissDuplicateWarning -> handleDismissDuplicateWarning()
            is AddItemUiEvent.ShowLevel3Proposal -> handleShowLevel3Proposal()
            is AddItemUiEvent.HideLevel3Proposal -> handleHideLevel3Proposal()
            is AddItemUiEvent.NewLevel3NameChanged -> handleNewLevel3NameChanged(event.name)
            is AddItemUiEvent.CreateNewLevel3 -> handleCreateNewLevel3()
            is AddItemUiEvent.ConfirmDuplicateLevel3 -> handleConfirmDuplicateLevel3()
            is AddItemUiEvent.DismissDuplicateLevel3Warning -> handleDismissDuplicateLevel3Warning()
            is AddItemUiEvent.ShowCategoryCorrection -> handleShowCategoryCorrection()
            is AddItemUiEvent.HideCategoryCorrection -> handleHideCategoryCorrection()
            is AddItemUiEvent.CategoryCorrectionLevel1Selected ->
                handleCategoryCorrectionLevel1Selected(event.level1Id)
            is AddItemUiEvent.CategoryCorrectionLevel2Selected ->
                handleCategoryCorrectionLevel2Selected(event.level2Id)
            is AddItemUiEvent.CategoryCorrectionLevel3Selected ->
                handleCategoryCorrectionLevel3Selected(event.level3Id)
            is AddItemUiEvent.CategoryCorrectionExpressionChanged ->
                handleCategoryCorrectionExpressionChanged(event.expression)
            is AddItemUiEvent.ApplyCategoryCorrection -> handleApplyCategoryCorrection()
            is AddItemUiEvent.ConfirmRememberCategory -> handleConfirmRememberCategory()
            is AddItemUiEvent.DismissRememberCategory -> handleDismissRememberCategory()
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                errors = emptyMap(),
                // Digitare esce dal filtro categoria e riprende la ricerca.
                activeCategory = null,
                categoryResults = emptyList(),
            )
        }
        _searchQuery.value = query
    }

    /**
     * L'utente ha scelto una categoria dai risultati: la lista mostra i
     * prodotti della categoria (già ordinati per nome dal repository).
     */
    private fun handleCategorySelected(category: CatalogCategoryResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errors = emptyMap()) }
            try {
                val products = catalogRepository.getByLevel3(category.id)
                _uiState.update {
                    it.copy(
                        searchResults = products,
                        categoryResults = emptyList(),
                        activeCategory = category,
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errors = it.errors + ("search" to (e.message ?: "Errore ricerca")),
                    )
                }
            }
        }
    }

    /** Rimuove il filtro categoria e riesegue la ricerca testuale corrente. */
    private fun handleClearCategoryFilter() {
        val snapshot = _uiState.value
        _uiState.update {
            it.copy(
                activeCategory = null,
                searchResults = emptyList(),
                categoryResults = emptyList(),
            )
        }
        if (snapshot.searchQuery.trim().length < 2) return
        viewModelScope.launch {
            try {
                val products = catalogRepository.search(snapshot.searchQuery).first()
                val categories = catalogRepository.searchCategories(snapshot.searchQuery).first()
                _uiState.update {
                    // Se nel frattempo l'utente ha cambiato query o riaperto
                    // un filtro, lo stato più recente vince.
                    if (it.searchQuery == snapshot.searchQuery && it.activeCategory == null) {
                        it.copy(searchResults = products, categoryResults = categories)
                    } else {
                        it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errors = it.errors + ("search" to (e.message ?: "Errore ricerca")))
                }
            }
        }
    }

    private fun handleProductSelected(product: CanonicalProduct) {
        // Le unità arrivano già risolte dal repository; il resolver è il
        // fallback per i repository fake e per i prodotti legacy senza metadati.
        val compatible = compatibleUnitsFor(product)

        // L'ordine è significativo: il resolver mette per primo il default
        // merceologico (kg per pasta/carne, L per bevande, pz per le uova).
        val defaultUnit = compatible.first()

        _uiState.update {
            it.copy(
                selectedProduct = product,
                searchResults = emptyList(),
                categoryResults = emptyList(),
                activeCategory = null,
                searchQuery = product.name,
                compatibleUnits = compatible,
                selectedUnit = defaultUnit,
                quantityText = "1",
                // Gli attributi appartengono alla voce: non trascinarli da un
                // prodotto selezionato in precedenza. La precompilazione con
                // l'ultimo uso dello stesso prodotto avviene subito dopo.
                brand = "",
                variant = "",
                condition = "",
                currentStep = 2,
                errors = emptyMap(),
                isCategoryCorrectionVisible = false,
                categoryCorrectionError = null,
                categoryCorrectionMessage = null,
                pendingCategoryCorrection = null,
                pendingCategoryAssignment = null,
                correctedCategoryPath = null,
            )
        }
        prefillLastAttributes(product.id)
    }

    private fun compatibleUnitsFor(product: CanonicalProduct): List<StandardUnit> {
        val resolved = product.compatibleUnits
            .ifEmpty { ProductUnitResolver.forProduct(product) }
        return resolved.ifEmpty { listOf(StandardUnit.PIECE) }
    }

    // ---- Story 7.1: creazione prodotto canonico user-defined ----

    private fun handleShowNewProductForm() {
        val prefill = _uiState.value.searchQuery.trim()
        _uiState.update {
            it.copy(
                isNewProductFormVisible = true,
                newProductName = prefill,
                newProductLevel1Id = null,
                newProductLevel2Id = null,
                newProductLevel3Id = null,
                newProductError = null,
                duplicateCandidate = null,
                isCreatingProduct = false,
                isLevel3ProposalVisible = false,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                isCreatingLevel3 = false,
            )
        }
        viewModelScope.launch {
            try {
                val levels = catalogRepository.getTaxonomy().first()
                _uiState.update { it.copy(taxonomy = levels) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(newProductError = "Errore durante il caricamento delle categorie")
                }
            }
        }
    }

    private fun handleHideNewProductForm() {
        _uiState.update {
            it.copy(
                isNewProductFormVisible = false,
                newProductError = null,
                duplicateCandidate = null,
                isCreatingProduct = false,
                isLevel3ProposalVisible = false,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                isCreatingLevel3 = false,
            )
        }
    }

    private fun handleNewProductNameChanged(name: String) {
        _uiState.update { it.copy(newProductName = name, newProductError = null) }
    }

    private fun handleNewProductLevel1Selected(level1Id: Long) {
        // La UI disabilita i dropdown durante le creazioni; la guardia evita
        // che un evento programmatico azzeri `isCreatingLevel3` a metà scrittura.
        if (_uiState.value.isCreatingLevel3 || _uiState.value.isCreatingProduct) return
        _uiState.update {
            it.copy(
                newProductLevel1Id = level1Id,
                newProductLevel2Id = null,
                newProductLevel3Id = null,
                newProductError = null,
                isLevel3ProposalVisible = false,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                isCreatingLevel3 = false,
            )
        }
    }

    private fun handleNewProductLevel2Selected(level2Id: Long) {
        // Vedi `handleNewProductLevel1Selected`: non azzerare le guardie di
        // creazione mentre una scrittura è in corso.
        if (_uiState.value.isCreatingLevel3 || _uiState.value.isCreatingProduct) return
        _uiState.update {
            it.copy(
                newProductLevel2Id = level2Id,
                newProductLevel3Id = null,
                newProductError = null,
                isLevel3ProposalVisible = false,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                isCreatingLevel3 = false,
            )
        }
    }

    private fun handleNewProductLevel3Selected(level3Id: Long) {
        if (_uiState.value.isCreatingLevel3 || _uiState.value.isCreatingProduct) return
        _uiState.update {
            it.copy(
                newProductLevel3Id = level3Id,
                newProductError = null,
                isLevel3ProposalVisible = false,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
            )
        }
    }

    private fun handleCreateNewProduct() {
        val state = _uiState.value
        // Guardia di rientranza: due eventi ravvicinati (doppio tap prima della
        // recomposition) inserirebbero due prodotti identici. Blocca anche la
        // creazione mentre è in corso quella di un Livello 3 (Story 7.2).
        if (state.isCreatingProduct || state.isCreatingLevel3) return
        val name = state.newProductName.trim()
        val level3Id = state.newProductLevel3Id

        if (name.isEmpty()) {
            _uiState.update { it.copy(newProductError = "Inserisci il nome del prodotto") }
            return
        }
        if (state.newProductLevel1Id == null ||
            state.newProductLevel2Id == null ||
            level3Id == null
        ) {
            _uiState.update { it.copy(newProductError = "Seleziona Livello 1, Livello 2 e Livello 3") }
            return
        }

        // Evita due dialog contemporanei: la creazione prodotto prende priorità
        // sull'eventuale avviso di duplicato del Livello 3.
        _uiState.update {
            it.copy(
                isCreatingProduct = true,
                newProductError = null,
                duplicateLevel3Candidate = null,
            )
        }
        viewModelScope.launch {
            try {
                handleCreateProductResult(
                    createProductUseCase(name = name, level3Id = level3Id, confirmDuplicate = false)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingProduct = false,
                        newProductError = "Errore durante la creazione del prodotto",
                    )
                }
            }
        }
    }

    private fun handleConfirmDuplicate() {
        val state = _uiState.value
        if (state.isCreatingProduct || state.isCreatingLevel3) return
        val level3Id = state.newProductLevel3Id ?: return
        if (state.duplicateCandidate == null) return
        val name = state.newProductName.trim()

        _uiState.update { it.copy(duplicateCandidate = null, isCreatingProduct = true) }
        viewModelScope.launch {
            try {
                handleCreateProductResult(
                    createProductUseCase(name = name, level3Id = level3Id, confirmDuplicate = true)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingProduct = false,
                        newProductError = "Errore durante la creazione del prodotto",
                    )
                }
            }
        }
    }

    private fun handleDismissDuplicateWarning() {
        _uiState.update { it.copy(duplicateCandidate = null) }
    }

    /** Gestione comune degli esiti della use case (creazione diretta e conferma duplicato). */
    private fun handleCreateProductResult(result: CreateProductResult) {
        when (result) {
            is CreateProductResult.Created -> {
                _uiState.update {
                    it.copy(
                        isNewProductFormVisible = false,
                        newProductName = "",
                        newProductLevel1Id = null,
                        newProductLevel2Id = null,
                        newProductLevel3Id = null,
                        newProductError = null,
                        duplicateCandidate = null,
                        isCreatingProduct = false,
                        isLevel3ProposalVisible = false,
                        newLevel3Name = "",
                        level3ProposalError = null,
                        duplicateLevel3Candidate = null,
                        isCreatingLevel3 = false,
                    )
                }
                // Riuso: porta allo step 2 e applica le unità della categoria.
                handleProductSelected(result.product)
            }

            is CreateProductResult.Duplicate -> {
                _uiState.update {
                    it.copy(
                        duplicateCandidate = result.candidate,
                        // Un solo dialog alla volta: la conferma prodotto ha
                        // priorità sull'eventuale avviso del Livello 3.
                        duplicateLevel3Candidate = null,
                        isCreatingProduct = false,
                    )
                }
            }

            is CreateProductResult.Invalid -> {
                _uiState.update {
                    it.copy(newProductError = result.message, isCreatingProduct = false)
                }
            }
        }
    }

    // ---- Story 7.2: proposta di un nuovo Livello 3 ----

    private fun handleShowLevel3Proposal() {
        val state = _uiState.value
        if (state.isCreatingLevel3 || state.isCreatingProduct) return
        if (state.newProductLevel2Id == null) return
        _uiState.update {
            it.copy(
                isLevel3ProposalVisible = true,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                isCreatingLevel3 = false,
            )
        }
    }

    private fun handleHideLevel3Proposal() {
        if (_uiState.value.isCreatingLevel3 || _uiState.value.isCreatingProduct) return
        _uiState.update {
            it.copy(
                isLevel3ProposalVisible = false,
                newLevel3Name = "",
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                isCreatingLevel3 = false,
            )
        }
    }

    private fun handleNewLevel3NameChanged(name: String) {
        _uiState.update { it.copy(newLevel3Name = name, level3ProposalError = null) }
    }

    private fun handleCreateNewLevel3() {
        val state = _uiState.value
        // Guardia di rientranza contro il doppio tap e contro la creazione
        // concorrente di un prodotto (stessa guardia di `handleCreateNewProduct`).
        if (state.isCreatingLevel3 || state.isCreatingProduct) return
        val level2Id = state.newProductLevel2Id
        val name = state.newLevel3Name.trim()

        if (level2Id == null) {
            _uiState.update { it.copy(level3ProposalError = "Seleziona il Livello 2") }
            return
        }
        if (name.isEmpty()) {
            _uiState.update {
                it.copy(level3ProposalError = "Inserisci il nome del nuovo Livello 3")
            }
            return
        }

        _uiState.update {
            it.copy(
                isCreatingLevel3 = true,
                level3ProposalError = null,
                duplicateLevel3Candidate = null,
                duplicateCandidate = null,
            )
        }
        viewModelScope.launch {
            try {
                handleCreateLevel3Result(
                    createLevel3UseCase(name = name, level2Id = level2Id, confirmDuplicate = false)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingLevel3 = false,
                        level3ProposalError = "Errore durante la creazione del Livello 3",
                    )
                }
            }
        }
    }

    private fun handleConfirmDuplicateLevel3() {
        val state = _uiState.value
        if (state.isCreatingLevel3 || state.isCreatingProduct) return
        val level2Id = state.newProductLevel2Id ?: return
        if (state.duplicateLevel3Candidate == null) return
        val name = state.newLevel3Name.trim()

        _uiState.update { it.copy(duplicateLevel3Candidate = null, isCreatingLevel3 = true) }
        viewModelScope.launch {
            try {
                handleCreateLevel3Result(
                    createLevel3UseCase(name = name, level2Id = level2Id, confirmDuplicate = true)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingLevel3 = false,
                        level3ProposalError = "Errore durante la creazione del Livello 3",
                    )
                }
            }
        }
    }

    private fun handleDismissDuplicateLevel3Warning() {
        _uiState.update { it.copy(duplicateLevel3Candidate = null) }
    }

    /**
     * Gestione comune degli esiti della use case (creazione diretta e conferma duplicato).
     *
     * Il risultato può arrivare dopo che l'utente ha chiuso il form o ha
     * cambiato Livello 2: in quel caso va scartato, altrimenti selezionerebbe
     * un Livello 3 appartenente a una categoria non più attiva.
     *
     * Il nuovo L3 viene aggiunto allo snapshot di tassonomia già in stato
     * (il repository lo ha persistito): si evita un reload che potrebbe
     * fallire dopo una scrittura già riuscita.
     */
    private fun handleCreateLevel3Result(result: CreateLevel3Result) {
        val state = _uiState.value
        if (!state.isNewProductFormVisible || !state.isLevel3ProposalVisible) return

        when (result) {
            is CreateLevel3Result.Created -> {
                if (state.newProductLevel2Id != result.level3.level2Id) {
                    _uiState.update { it.copy(isCreatingLevel3 = false) }
                    return
                }
                _uiState.update {
                    it.copy(
                        taxonomy = it.taxonomy + result.level3,
                        newProductLevel3Id = result.level3.id,
                        isLevel3ProposalVisible = false,
                        newLevel3Name = "",
                        level3ProposalError = null,
                        duplicateLevel3Candidate = null,
                        isCreatingLevel3 = false,
                    )
                }
            }

            is CreateLevel3Result.Duplicate -> {
                if (state.newProductLevel2Id != result.candidate.level2Id) {
                    _uiState.update { it.copy(isCreatingLevel3 = false) }
                    return
                }
                _uiState.update {
                    it.copy(
                        duplicateLevel3Candidate = result.candidate,
                        duplicateCandidate = null,
                        isCreatingLevel3 = false,
                    )
                }
            }

            is CreateLevel3Result.Invalid -> {
                _uiState.update {
                    it.copy(level3ProposalError = result.message, isCreatingLevel3 = false)
                }
            }
        }
    }

    // ---- Story 3.6: correzione categoria + regola appresa ----

    private fun handleShowCategoryCorrection() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        if (correctCategoryUseCase == null) {
            _uiState.update {
                it.copy(categoryCorrectionMessage = "Correzione categoria non disponibile")
            }
            return
        }

        val level3 = state.taxonomy
            .filterIsInstance<TaxonomyLevel.Level3>()
            .firstOrNull { it.id == product.level3Id }
        val level2 = level3?.let { selected ->
            state.taxonomy.filterIsInstance<TaxonomyLevel.Level2>()
                .firstOrNull { it.id == selected.level2Id }
        }
        val level1 = level2?.let { selected ->
            state.taxonomy.filterIsInstance<TaxonomyLevel.Level1>()
                .firstOrNull { it.id == selected.level1Id }
        }

        _uiState.update {
            it.copy(
                isCategoryCorrectionVisible = true,
                categoryCorrectionLevel1Id = level1?.id ?: product.level1Id,
                categoryCorrectionLevel2Id = level2?.id ?: product.level2Id,
                categoryCorrectionLevel3Id = level3?.id ?: product.level3Id,
                categoryCorrectionExpression = state.searchQuery.trim().ifBlank { product.name },
                categoryCorrectionError = null,
                categoryCorrectionMessage = null,
            )
        }
    }

    private fun handleHideCategoryCorrection() {
        if (_uiState.value.isSavingCategoryRule) return
        _uiState.update {
            it.copy(
                isCategoryCorrectionVisible = false,
                categoryCorrectionError = null,
            )
        }
    }

    private fun handleCategoryCorrectionLevel1Selected(level1Id: Long) {
        _uiState.update {
            it.copy(
                categoryCorrectionLevel1Id = level1Id,
                categoryCorrectionLevel2Id = null,
                categoryCorrectionLevel3Id = null,
                categoryCorrectionError = null,
            )
        }
    }

    private fun handleCategoryCorrectionLevel2Selected(level2Id: Long) {
        _uiState.update {
            it.copy(
                categoryCorrectionLevel2Id = level2Id,
                categoryCorrectionLevel3Id = null,
                categoryCorrectionError = null,
            )
        }
    }

    private fun handleCategoryCorrectionLevel3Selected(level3Id: Long) {
        _uiState.update {
            it.copy(
                categoryCorrectionLevel3Id = level3Id,
                categoryCorrectionError = null,
            )
        }
    }

    private fun handleCategoryCorrectionExpressionChanged(expression: String) {
        _uiState.update {
            it.copy(
                categoryCorrectionExpression = expression,
                categoryCorrectionError = null,
            )
        }
    }

    private fun handleApplyCategoryCorrection() {
        val state = _uiState.value
        if (state.isSavingCategoryRule) return
        val product = state.selectedProduct ?: return
        val useCase = correctCategoryUseCase
        if (useCase == null) {
            _uiState.update {
                it.copy(categoryCorrectionError = "Correzione categoria non disponibile")
            }
            return
        }
        val newLevel3Id = state.categoryCorrectionLevel3Id
        if (newLevel3Id == null) {
            _uiState.update {
                it.copy(categoryCorrectionError = "Seleziona una categoria")
            }
            return
        }
        val expression = state.categoryCorrectionExpression.trim().ifBlank { product.name }

        val assignment = CategoryAssignment(
            suggestedLevel3Id = product.level3Id,
            confidence = 1.0,
            source = AssignmentSource.DEFAULT,
            product = product,
            userExpression = expression,
        )

        _uiState.update {
            it.copy(isSavingCategoryRule = true, categoryCorrectionError = null)
        }
        viewModelScope.launch {
            try {
                val correction = useCase(assignment, newLevel3Id)
                if (!correction.isEffective) {
                    _uiState.update {
                        it.copy(
                            isSavingCategoryRule = false,
                            categoryCorrectionError =
                                "Seleziona una categoria diversa da quella attuale",
                        )
                    }
                    return@launch
                }
                val canRemember = useCase.canShowRememberPrompt(assignment, correction)
                _uiState.update {
                    it.copy(
                        isCategoryCorrectionVisible = false,
                        isSavingCategoryRule = false,
                        pendingCategoryCorrection = if (canRemember) correction else null,
                        pendingCategoryAssignment = if (canRemember) assignment else null,
                        correctedCategoryPath = categoryPathFor(
                            newLevel3Id,
                            state.taxonomy,
                        ),
                        categoryCorrectionMessage = if (canRemember) {
                            null
                        } else {
                            "Nessuna regola salvata"
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingCategoryRule = false,
                        categoryCorrectionError =
                            e.message ?: "Errore durante la correzione della categoria",
                    )
                }
            }
        }
    }

    private fun handleConfirmRememberCategory() {
        val state = _uiState.value
        if (state.isSavingCategoryRule) return
        val correction = state.pendingCategoryCorrection ?: return
        val assignment = state.pendingCategoryAssignment ?: return
        val useCase = correctCategoryUseCase
        if (useCase == null) {
            _uiState.update {
                it.copy(categoryCorrectionMessage = "Correzione categoria non disponibile")
            }
            return
        }

        _uiState.update { it.copy(isSavingCategoryRule = true) }
        viewModelScope.launch {
            try {
                useCase.confirmRemember(assignment, correction)
                _uiState.update {
                    it.copy(
                        isSavingCategoryRule = false,
                        pendingCategoryCorrection = null,
                        pendingCategoryAssignment = null,
                        categoryCorrectionMessage = "Regola appresa salvata",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingCategoryRule = false,
                        pendingCategoryCorrection = null,
                        pendingCategoryAssignment = null,
                        categoryCorrectionMessage =
                            "Errore durante il salvataggio della regola",
                    )
                }
            }
        }
    }

    private fun handleDismissRememberCategory() {
        if (_uiState.value.isSavingCategoryRule) return
        correctCategoryUseCase?.declineRemember()
        _uiState.update {
            it.copy(
                isSavingCategoryRule = false,
                pendingCategoryCorrection = null,
                pendingCategoryAssignment = null,
                categoryCorrectionMessage = "Nessuna regola salvata",
            )
        }
    }

    /** Percorso L1 > L2 > L3 della categoria, con fallback leggibile. */
    private fun categoryPathFor(
        level3Id: Long,
        taxonomy: List<TaxonomyLevel>,
    ): String {
        val level3 = taxonomy
            .filterIsInstance<TaxonomyLevel.Level3>()
            .firstOrNull { it.id == level3Id }
            ?: return "Categoria #$level3Id"
        val level2 = taxonomy
            .filterIsInstance<TaxonomyLevel.Level2>()
            .firstOrNull { it.id == level3.level2Id }
        val level1 = level2?.let { selected ->
            taxonomy.filterIsInstance<TaxonomyLevel.Level1>()
                .firstOrNull { it.id == selected.level1Id }
        }
        return listOfNotNull(level1?.name, level2?.name, level3.name)
            .joinToString(" > ")
    }

    private fun handleQuantityChanged(quantityText: String) {
        _uiState.update {
            it.copy(
                // Conserva il testo così com'è: l'utente può svuotare il campo.
                // La validazione avviene su "Avanti"/"Aggiungi".
                quantityText = quantityText,
                errors = it.errors.minus("quantity"),
            )
        }
    }

    private fun handleUnitChanged(unit: StandardUnit) {
        _uiState.update {
            it.copy(selectedUnit = unit, errors = it.errors.minus("unit"))
        }
    }

    private fun handleBrandChanged(brand: String) {
        _uiState.update { it.copy(brand = brand) }
    }

    private fun handleVariantChanged(variant: String) {
        _uiState.update { it.copy(variant = variant) }
    }

    private fun handleConditionChanged(condition: String) {
        _uiState.update { it.copy(condition = condition) }
    }

    private fun handleNextStep() {
        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                1 -> if (state.canProceedToStep2) 2 else 1
                2 -> if (state.canProceedToStep3) 3 else 2
                3 -> 4
                else -> state.currentStep
            }
            // Validazione step corrente
            val errors = validateStep(state.currentStep, state)
            state.copy(currentStep = nextStep, errors = errors)
        }
    }

    private fun handlePreviousStep() {
        // Con il form "Nuovo prodotto" aperto, "Indietro" chiude il form
        // senza cambiare step (Story 7.1).
        if (_uiState.value.isNewProductFormVisible) {
            handleHideNewProductForm()
            return
        }
        _uiState.update { state ->
            // In edit mode, non permettere di tornare allo step 1 (ricerca)
            val minStep = if (isEditMode) 2 else 1
            state.copy(currentStep = maxOf(minStep, state.currentStep - 1))
        }
    }

    private fun handleConfirm() {
        val state = _uiState.value
        // Valida sempre step 4 (conferma) indipendentemente da currentStep
        val errors = validateStep(4, state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, currentStep = 4) }
            return
        }

        val product = state.selectedProduct ?: return
        val unit = state.selectedUnit ?: return

        // Marca e variante si salvano sempre in forma "capitalized": "barilla"
        // e "BARILLA" diventano "Barilla", così non nascono duplicati.
        val brand = ProductNameNormalizer.capitalizeFirstWord(state.brand).ifBlank { null }
        val variant = ProductNameNormalizer.capitalizeFirstWord(state.variant).ifBlank { null }

        val item = if (isEditMode) {
            // Modalità EDIT: aggiorna item esistente mantenendo ID e timestamp creato
            editItem!!.copy(
                productId = state.selectedProduct!!.id,
                quantity = state.quantity,
                unit = unit,
                brand = brand,
                variant = variant,
                condition = state.condition.ifBlank { null },
                isChecked = editItem!!.isChecked, // mantiene stato check
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            // Modalità INSERT: nuova voce
            ShoppingItem(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                quantity = state.quantity,
                unit = unit,
                brand = brand,
                variant = variant,
                condition = state.condition.ifBlank { null },
                isChecked = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        }

        // Inserisci/aggiorna in repository, poi notifica la sheet.
        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (isEditMode) {
                    shoppingRepository.update(item)
                } else {
                    shoppingRepository.insert(item)
                }
                // Registra i valori digitati dall'utente: restano disponibili
                // come suggerimenti e compaiono nelle Impostazioni. I valori
                // predefiniti e i duplicati vengono ignorati dal repository.
                // Best-effort: un errore del registro non deve impedire il
                // salvataggio della voce.
                try {
                    userAttributeRepository?.remember(UserAttributeType.BRAND, brand.orEmpty())
                    userAttributeRepository?.remember(UserAttributeType.VARIANT, variant.orEmpty())
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // I suggerimenti restano quelli già noti.
                }
            }
            handleReset()
            _saved.emit(Unit)
        }
    }

    private fun handleCancel() {
        handleReset()
    }

    private fun handleReset() {
        if (isEditMode) {
            // In edit mode, reset to original values or clear
            initializeEditMode()
        } else {
            _uiState.update { AddItemUiState(isEditMode = false) }
        }
    }

    /** Validazione per step corrente */
    private fun validateStep(step: Int, state: AddItemUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        when (step) {
            1 -> {
                if (!state.canProceedToStep2) errors["product"] = "Seleziona un prodotto"
            }
            2 -> {
                quantityValidationError(state)?.let { errors["quantity"] = it }
                if (state.selectedUnit == null) errors["unit"] = "Seleziona un'unità"
            }
            3 -> {} // Attributi opzionali
            4 -> {
                if (!state.isValidForConfirm) {
                    if (!state.canProceedToStep2) errors["product"] = "Seleziona un prodotto"
                    quantityValidationError(state)?.let { errors["quantity"] = it }
                    if (state.selectedUnit == null) errors["unit"] = "Seleziona un'unità"
                }
            }
        }
        return errors
    }

    /** Messaggio di errore per il campo quantità, o null se valido. */
    private fun quantityValidationError(state: AddItemUiState): String? = when {
        state.quantityText.isBlank() -> "Inserisci una quantità"
        parseQuantityText(state.quantityText) == null -> "Inserisci una quantità valida"
        state.quantity <= 0 -> "La quantità deve essere maggiore di zero"
        else -> null
    }

    /** Factory per modalità INSERT */
    companion object {
        fun forInsert(
            catalogRepository: CatalogRepository,
            shoppingRepository: ShoppingListRepository,
            createProductUseCase: CreateCanonicalProductUseCase,
            createLevel3UseCase: CreateUserLevel3UseCase,
            correctCategoryUseCase: CorrectCategoryUseCase? = null,
        ): AddItemViewModel = AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = shoppingRepository,
            createProductUseCase = createProductUseCase,
            createLevel3UseCase = createLevel3UseCase,
            correctCategoryUseCase = correctCategoryUseCase,
        )

        /** Factory per modalità EDIT */
        fun forEdit(
            catalogRepository: CatalogRepository,
            shoppingRepository: ShoppingListRepository,
            createProductUseCase: CreateCanonicalProductUseCase,
            createLevel3UseCase: CreateUserLevel3UseCase,
            item: ShoppingItem,
            correctCategoryUseCase: CorrectCategoryUseCase? = null,
        ): AddItemViewModel = AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = shoppingRepository,
            createProductUseCase = createProductUseCase,
            createLevel3UseCase = createLevel3UseCase,
            correctCategoryUseCase = correctCategoryUseCase,
            editItem = item,
        )
    }
}
