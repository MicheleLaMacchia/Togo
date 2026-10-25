package it.togo.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.RenameUserAttributeResult
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.UserAttributeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Categoria selezionabile nel dialog di modifica prodotto. */
data class UserCategoryOption(
    val id: Long,
    val label: String,
)

/** Stato delle tre sezioni "I miei dati" delle Impostazioni. */
data class UserDataUiState(
    val products: List<CanonicalProduct> = emptyList(),
    val brands: List<UserAttribute> = emptyList(),
    val variants: List<UserAttribute> = emptyList(),
    val categories: List<UserCategoryOption> = emptyList(),
)

/** Esiti delle operazioni, consumati dalle schermate per chiudere i dialog. */
sealed interface UserDataEvent {
    data object ProductUpdated : UserDataEvent
    data object ProductDeleted : UserDataEvent
    data object AttributeUpdated : UserDataEvent
    data object AttributeDeleted : UserDataEvent
    data class Error(val message: String) : UserDataEvent
}

/**
 * ViewModel condiviso dalle tre sezioni delle Impostazioni:
 * prodotti, marche e varianti creati dall'utente.
 *
 * I prodotti arrivano dal catalogo (`is_user_defined = 1`), marche e varianti
 * dal registro `USER_ATTRIBUTE`. Le operazioni di scrittura non toccano le voci
 * già presenti nella lista attiva o nello storico: eliminare un prodotto o un
 * attributo agisce solo su catalogo/registro e sui suggerimenti.
 */
class UserDataViewModel(
    private val catalogRepository: CatalogRepository,
    private val userAttributeRepository: UserAttributeRepository,
) : ViewModel() {

    private val _events = MutableSharedFlow<UserDataEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UserDataEvent> = _events.asSharedFlow()

    val uiState = combine(
        emptyOnError(catalogRepository.getUserProducts(), emptyList<CanonicalProduct>()),
        emptyOnError(
            userAttributeRepository.observe(UserAttributeType.BRAND),
            emptyList<UserAttribute>(),
        ),
        emptyOnError(
            userAttributeRepository.observe(UserAttributeType.VARIANT),
            emptyList<UserAttribute>(),
        ),
        emptyOnError(catalogRepository.getTaxonomy(), emptyList<TaxonomyLevel>()),
    ) { products, brands, variants, taxonomy ->
        UserDataUiState(
            products = products,
            brands = brands,
            variants = variants,
            categories = buildCategoryOptions(taxonomy),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserDataUiState(),
    )

    /**
     * Aggiorna nome e categoria di un prodotto user-defined.
     *
     * Il controllo duplicati è applicativo (come alla creazione): stesso nome
     * normalizzato nella stessa categoria.
     */
    fun updateProduct(id: Long, rawName: String, level3Id: Long) {
        val name = ProductNameNormalizer.capitalizeFirstWord(rawName)
        if (name.isBlank()) {
            emitError("Inserisci un nome valido")
            return
        }

        viewModelScope.launch {
            try {
                val normalized = ProductNameNormalizer.normalize(name)
                val duplicate = catalogRepository.getByLevel3(level3Id).any { product ->
                    product.id != id &&
                        ProductNameNormalizer.normalize(product.name) == normalized
                }
                if (duplicate) {
                    _events.emit(
                        UserDataEvent.Error(
                            "Esiste già un prodotto con questo nome nella categoria scelta"
                        )
                    )
                    return@launch
                }

                val updated = catalogRepository.updateUserProduct(id, name, level3Id)
                _events.emit(
                    if (updated) {
                        UserDataEvent.ProductUpdated
                    } else {
                        UserDataEvent.Error("Prodotto non aggiornato")
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _events.emit(UserDataEvent.Error("Errore durante la modifica del prodotto"))
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            try {
                val deleted = catalogRepository.deleteUserProduct(id)
                _events.emit(
                    if (deleted) {
                        UserDataEvent.ProductDeleted
                    } else {
                        UserDataEvent.Error("Prodotto non eliminato")
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _events.emit(UserDataEvent.Error("Errore durante l'eliminazione del prodotto"))
            }
        }
    }

    fun renameAttribute(attribute: UserAttribute, rawValue: String) {
        viewModelScope.launch {
            try {
                when (val result = userAttributeRepository.rename(
                    id = attribute.id,
                    type = attribute.type,
                    rawValue = rawValue,
                )) {
                    is RenameUserAttributeResult.Renamed -> {
                        _events.emit(UserDataEvent.AttributeUpdated)
                    }

                    is RenameUserAttributeResult.Duplicate -> {
                        _events.emit(
                            UserDataEvent.Error("«${result.existing.value}» esiste già")
                        )
                    }

                    RenameUserAttributeResult.Invalid -> {
                        _events.emit(UserDataEvent.Error("Inserisci un valore valido"))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _events.emit(UserDataEvent.Error("Errore durante la modifica"))
            }
        }
    }

    fun deleteAttribute(attribute: UserAttribute) {
        viewModelScope.launch {
            try {
                val deleted = userAttributeRepository.delete(attribute.id)
                _events.emit(
                    if (deleted) {
                        UserDataEvent.AttributeDeleted
                    } else {
                        UserDataEvent.Error("Valore non eliminato")
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _events.emit(UserDataEvent.Error("Errore durante l'eliminazione"))
            }
        }
    }

    private fun emitError(message: String) {
        _events.tryEmit(UserDataEvent.Error(message))
    }

    /** Un errore su una sorgente non deve svuotare le altre sezioni. */
    private fun <T> emptyOnError(source: kotlinx.coroutines.flow.Flow<T>, empty: T): kotlinx.coroutines.flow.Flow<T> =
        source.catch { emit(empty) }

    private fun buildCategoryOptions(taxonomy: List<TaxonomyLevel>): List<UserCategoryOption> {
        val level1 = taxonomy.filterIsInstance<TaxonomyLevel.Level1>().associateBy { it.id }
        val level2 = taxonomy.filterIsInstance<TaxonomyLevel.Level2>().associateBy { it.id }

        return taxonomy.filterIsInstance<TaxonomyLevel.Level3>().mapNotNull { level3 ->
            val parent2 = level2[level3.level2Id] ?: return@mapNotNull null
            val parent1 = level1[parent2.level1Id] ?: return@mapNotNull null
            UserCategoryOption(
                id = level3.id,
                label = "${parent1.name} › ${parent2.name} › ${level3.name}",
            )
        }
    }
}
