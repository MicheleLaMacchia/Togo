package it.togo.app.domain.usecase

import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository

/**
 * Esito della proposta di un nuovo Livello 3 user-defined (Story 7.2).
 */
sealed interface CreateLevel3Result {
    /** Livello 3 persistito in `TAXONOMY_LEVEL_3` con `is_user_defined = 1`. */
    data class Created(val level3: TaxonomyLevel.Level3) : CreateLevel3Result

    /** Esiste già un Livello 3 con nome equivalente nello stesso Livello 2. */
    data class Duplicate(val candidate: TaxonomyLevel.Level3) : CreateLevel3Result

    /** Input non valido: nessuna scrittura effettuata. */
    data class Invalid(val message: String) : CreateLevel3Result
}

/**
 * Use Case per la proposta di un nuovo Livello 3 sotto un Livello 2 esistente.
 *
 * Il confronto duplicati è uguaglianza dopo [ProductNameNormalizer.normalize]
 * nello stesso `level2Id` (predefiniti + user-defined). NON esiste un vincolo
 * UNIQUE nel DB: l'avviso è applicativo e l'utente può procedere comunque.
 *
 * L'IO è responsabilità del [CatalogRepository]: qui non c'è `withContext`,
 * così i test con dispatcher virtuale restano deterministici.
 */
class CreateUserLevel3UseCase(
    private val catalogRepository: CatalogRepository
) {

    /**
     * Crea un Livello 3 user-defined sotto un Livello 2 esistente.
     *
     * @param name nome digitato dall'utente (viene salvato con trim)
     * @param level2Id Livello 2 genitore, già selezionato nel form
     * @param confirmDuplicate true se l'utente ha confermato l'avviso di duplicato
     */
    suspend operator fun invoke(
        name: String,
        level2Id: Long,
        confirmDuplicate: Boolean = false,
    ): CreateLevel3Result {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return CreateLevel3Result.Invalid("Inserisci il nome del nuovo Livello 3")
        }
        if (level2Id <= 0L) {
            return CreateLevel3Result.Invalid("Seleziona il Livello 2")
        }

        val normalized = ProductNameNormalizer.normalize(trimmed)
        if (normalized.isEmpty()) {
            return CreateLevel3Result.Invalid("Inserisci un nome valido")
        }

        val candidate = catalogRepository.getLevel3ByLevel2(level2Id)
            .firstOrNull { ProductNameNormalizer.normalize(it.name) == normalized }

        if (candidate != null && !confirmDuplicate) {
            return CreateLevel3Result.Duplicate(candidate)
        }

        return CreateLevel3Result.Created(catalogRepository.insertLevel3(trimmed, level2Id))
    }
}
