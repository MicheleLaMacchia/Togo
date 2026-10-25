package it.togo.app.domain.repository

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.CatalogCategoryResult
import it.togo.app.domain.model.TaxonomyLevel
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun search(query: String): Flow<List<CanonicalProduct>>

    /**
     * Categorie (Livelli 3) il cui nome corrisponde a [query].
     *
     * Servono a dare un risultato utile quando la query è un termine generico
     * che non è il nome di un prodotto (es. "pasta": i prodotti si chiamano
     * Spaghetti, Penne, ...). Il chiamante mostra i prodotti prima e le
     * categorie dopo, separate da un divisorio.
     *
     * Il default vuoto mantiene compatibili i repository fake dei test.
     */
    fun searchCategories(query: String): Flow<List<CatalogCategoryResult>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

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

    /**
     * Livelli 3 appartenenti a [level2Id] (predefiniti e user-defined).
     *
     * Usato dal controllo duplicati della proposta di un nuovo Livello 3
     * (Story 7.2) e per aggiornare la tassonomia dopo la creazione. Il default
     * lancia eccezione per non obbligare i fake preesistenti dei test a
     * implementarlo: l'implementazione di produzione deve fare override.
     */
    suspend fun getLevel3ByLevel2(level2Id: Long): List<TaxonomyLevel.Level3> =
        throw UnsupportedOperationException("getLevel3ByLevel2 non implementato dal repository")

    /**
     * Inserisce un Livello 3 user-defined sotto [level2Id] e ritorna il livello
     * con l'ID assegnato dal DB (`is_user_defined = 1`). Il `sortOrder` è
     * calcolato dal repository in fondo alla categoria.
     *
     * Il default lancia eccezione per compatibilità con i fake dei test;
     * l'implementazione di produzione deve fare override.
     */
    suspend fun insertLevel3(name: String, level2Id: Long): TaxonomyLevel.Level3 =
        throw UnsupportedOperationException("insertLevel3 non implementato dal repository")
}