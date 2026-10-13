package it.togo.app.domain.category

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel

/**
 * Rappresenta una correzione esplicita della categoria canonica da parte dell'utente.
 *
 * @property originalLevel3Id Categoria originariamente suggerita (da CategoryAssignment)
 * @property correctedLevel3Id Nuova categoria scelta dall'utente
 * @property rootOriginalLevel3Id Prima categoria suggerita (per undo completo catena)
 * @property product Prodotto canonico interessato
 * @property timestamp Timestamp della correzione
 */
data class CategoryCorrection(
    val originalLevel3Id: Long,
    val correctedLevel3Id: Long,
    val rootOriginalLevel3Id: Long,
    val product: CanonicalProduct,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Crea nuova correzione da assegnazione (primo step) */
    constructor(
        originalLevel3Id: Long,
        correctedLevel3Id: Long,
        product: CanonicalProduct,
        timestamp: Long = System.currentTimeMillis()
    ) : this(
        originalLevel3Id = originalLevel3Id,
        correctedLevel3Id = correctedLevel3Id,
        rootOriginalLevel3Id = originalLevel3Id,
        product = product,
        timestamp = timestamp
    )

    /** True se la correzione è diversa dall'originale (effettiva modifica) */
    val isEffective: Boolean = originalLevel3Id != correctedLevel3Id

    /** True se diversa dalla radice (catena di correzioni) */
    val hasCorrectionChain: Boolean = rootOriginalLevel3Id != correctedLevel3Id

    /** Crea oggetto per annullamento (ripristina original immediato) */
    fun undo(): CategoryCorrection = copy(
        correctedLevel3Id = originalLevel3Id,
        timestamp = System.currentTimeMillis()
    )

    /** Crea oggetto per annullamento completo (ripristina radice) */
    fun undoFull(): CategoryCorrection = copy(
        correctedLevel3Id = rootOriginalLevel3Id,
        timestamp = System.currentTimeMillis()
    )

    /** Crea correzione successiva nella catena (A→B, poi B→C) */
    fun nextCorrection(newLevel3Id: Long): CategoryCorrection = copy(
        originalLevel3Id = correctedLevel3Id,
        correctedLevel3Id = newLevel3Id,
        timestamp = System.currentTimeMillis()
    )

    /** Descrizione testuale per UI: "Categoria suggerita: X → La tua scelta: Y" */
    fun toDiffString(
        originalName: String,
        correctedName: String
    ): String = "Categoria suggerita: $originalName → La tua scelta: $correctedName"
}