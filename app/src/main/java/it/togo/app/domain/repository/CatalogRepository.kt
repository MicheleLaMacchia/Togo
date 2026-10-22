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

    /**
     * Prodotti canonici appartenenti a [level3Id], ordinati per nome.
     *
     * Usato dal controllo duplicati della creazione prodotto (Story 7.1).
     * Il default lancia eccezione per non obbligare i fake preesistenti dei
     * test a implementarlo: l'implementazione di produzione deve fare override.
     */
    suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct> =
        throw UnsupportedOperationException("getByLevel3 non implementato dal repository")

    /**
     * Inserisce un prodotto canonico user-defined e ritorna il prodotto con
     * l'ID assegnato dal DB (`is_user_defined = 1`).
     *
     * Il default lancia eccezione per compatibilità con i fake dei test;
     * l'implementazione di produzione deve fare override.
     */
    suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct =
        throw UnsupportedOperationException("insertProduct non implementato dal repository")
}