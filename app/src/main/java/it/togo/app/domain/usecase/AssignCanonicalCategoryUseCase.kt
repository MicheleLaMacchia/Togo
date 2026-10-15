package it.togo.app.domain.usecase

import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.category.CategoryAssigner
import it.togo.app.domain.model.CanonicalProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case per l'assegnazione della categoria canonica a un prodotto risolto.
 *
 * Orchestra CategoryAssigner + repository access.
 * Puro dominio: nessuna dipendenza Android, testabile su JVM.
 */
class AssignCanonicalCategoryUseCase(
    private val categoryAssigner: CategoryAssigner
) {

    /**
     * Assegna la categoria canonica al prodotto.
     *
     * @param resolvedProduct Prodotto canonico risolto dal resolver (Story 3.3)
     * @param userExpression Espressione utente originale (per lookup regole apprese)
     * @return CategoryAssignment con categoria suggerita, confidence, origine
     */
    suspend operator fun invoke(
        resolvedProduct: CanonicalProduct,
        userExpression: String? = null
    ): CategoryAssignment = withContext(Dispatchers.IO) {
        categoryAssigner.assign(resolvedProduct, userExpression)
    }
}