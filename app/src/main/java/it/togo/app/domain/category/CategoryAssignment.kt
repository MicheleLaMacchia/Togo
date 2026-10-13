package it.togo.app.domain.category

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel

/**
 * Risultato dell'assegnazione della categoria canonica a un prodotto.
 *
 * @property suggestedLevel3Id ID del Livello 3 suggerito come categoria canonica
 * @property confidence Livello di confidenza (0.0-1.0): 1.0 = default prodotto, 0.9 = regola appresa, 0.5 = fallback, <0.9 = incerto
 * @property source Origine dell'assegnazione: DEFAULT, LEARNED, FALLBACK, USER_CORRECTED
 * @property product Il prodotto canonico a cui si assegna la categoria
 * @property alternatives Altre categorie candidate (es. default prodotto se rule usata, o rule se default usato)
 * @property userExpression Espressione utente che ha triggerato l'assegnazione (per lookup rule)
 */
data class CategoryAssignment(
    val suggestedLevel3Id: Long,
    val confidence: Double,
    val source: AssignmentSource,
    val product: CanonicalProduct,
    val alternatives: List<TaxonomyLevel.Level3> = emptyList(),
    val userExpression: String? = null
) {
    /** True se l'assegnazione richiede conferma/correzione UI (confidence <= 0.9) */
    val requiresConfirmation: Boolean = confidence <= 0.9

    /** True se l'assegnazione proviene da regola appresa */
    val isFromLearnedRule: Boolean = source == AssignmentSource.LEARNED

    /** True se l'assegnazione è il default del prodotto */
    val isDefault: Boolean = source == AssignmentSource.DEFAULT

    /** True se l'assegnazione è un fallback (nessun default valido) */
    val isFallback: Boolean = source == AssignmentSource.FALLBACK
}

/** Origine dell'assegnazione categoria */
enum class AssignmentSource {
    DEFAULT,       // Categoria di default del prodotto (level3Id del CanonicalProduct)
    LEARNED,       // Regola appresa (LEARNED_RULE)
    FALLBACK,      // Fallback: primo L3 non user-defined disponibile
    USER_CORRECTED // Correzione esplicita utente (prevalenza su tutto)
}