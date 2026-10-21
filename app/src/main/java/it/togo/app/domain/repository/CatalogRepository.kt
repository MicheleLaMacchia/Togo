package it.togo.app.domain.repository

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun search(query: String): Flow<List<CanonicalProduct>>
    suspend fun getById(id: Long): CanonicalProduct?
    fun getTaxonomy(): Flow<List<TaxonomyLevel>>

    /**
     * Prodotto il cui sinonimo coincide esattamente con [term] (normalizzato).
     *
     * È un segnale più forte della ricerca LIKE: evita che un sinonimo generico
     * come "pane di" diventi ambiguo solo perché è prefisso di più nomi.
     * Il default null mantiene compatibili i repository fake dei test.
     */
    suspend fun getByExactSynonym(term: String): CanonicalProduct? = null
}