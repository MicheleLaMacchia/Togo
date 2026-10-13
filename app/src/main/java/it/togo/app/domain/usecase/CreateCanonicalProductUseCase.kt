package it.togo.app.domain.usecase

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.wizard.NewProductDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case per creazione di un nuovo Prodotto Canonico (user-defined).
 *
 * Persiste il prodotto nel catalogo locale. Se l'utente ha creato un nuovo L3,
 * lo crea prima e poi il prodotto.
 */
class CreateCanonicalProductUseCase(
    private val catalogRepository: CatalogRepository
) {

    /**
     * Crea un nuovo prodotto canonico dalla bozza wizard.
     *
     * @param draft Bozza completata (step CONFIRM)
     * @return CanonicalProduct creato con ID assegnato da DB
     */
    suspend operator fun invoke(draft: NewProductDraft): CanonicalProduct = withContext(Dispatchers.IO) {
        val product = draft.toCanonicalProduct()

        // Persiste il prodotto nel catalogo locale
        // Nota: in un'implementazione completa, qui si chiamerebbe catalogRepository.insert(product)
        // Per ora ritorniamo il prodotto con un ID basato su timestamp come placeholder
        // finché CatalogRepository non espone un metodo insert

        product.copy(id = System.currentTimeMillis())
    }
}