package it.togo.app.domain.usecase

import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.repository.CatalogRepository

/**
 * Esito della creazione di un prodotto canonico user-defined (Story 7.1).
 */
sealed interface CreateProductResult {
    /** Prodotto persistito in `CANONICAL_PRODUCT` con `is_user_defined = 1`. */
    data class Created(val product: CanonicalProduct) : CreateProductResult

    /** Esiste già un prodotto con nome equivalente nello stesso Livello 3. */
    data class Duplicate(val candidate: CanonicalProduct) : CreateProductResult

    /** Input non valido: nessuna scrittura effettuata. */
    data class Invalid(val message: String) : CreateProductResult
}

/**
 * Use Case per creazione di un nuovo Prodotto Canonico (user-defined).
 *
 * Il confronto duplicati è uguaglianza dopo [ProductNameNormalizer.normalize]
 * nello stesso Livello 3 scelto. NON esiste un vincolo UNIQUE nel DB: l'avviso
 * è applicativo e l'utente può decidere di procedere comunque.
 *
 * L'IO è responsabilità del [CatalogRepository]: qui non c'è `withContext`,
 * così i test con dispatcher virtuale restano deterministici.
 */
class CreateCanonicalProductUseCase(
    private val catalogRepository: CatalogRepository
) {

    /**
     * Crea un prodotto canonico su un Livello 3 esistente.
     *
     * @param name nome digitato dall'utente (viene salvato con trim)
     * @param level3Id Livello 3 esistente selezionato nel form
     * @param confirmDuplicate true se l'utente ha confermato l'avviso di duplicato
     */
    suspend operator fun invoke(
        name: String,
        level3Id: Long,
        confirmDuplicate: Boolean = false,
    ): CreateProductResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return CreateProductResult.Invalid("Inserisci il nome del prodotto")
        }
        if (level3Id <= 0L) {
            return CreateProductResult.Invalid("Seleziona il Livello 3")
        }

        val normalized = ProductNameNormalizer.normalize(trimmed)
        if (normalized.isEmpty()) {
            return CreateProductResult.Invalid("Inserisci un nome valido")
        }

        val candidate = catalogRepository.getByLevel3(level3Id)
            .firstOrNull { ProductNameNormalizer.normalize(it.name) == normalized }

        if (candidate != null && !confirmDuplicate) {
            return CreateProductResult.Duplicate(candidate)
        }

        return CreateProductResult.Created(catalogRepository.insertProduct(trimmed, level3Id))
    }
}
