package it.togo.app.domain.model

/**
 * Risultato di ricerca che rappresenta una categoria (Livello 3), non un
 * prodotto.
 *
 * Serve a far emergere le categorie quando l'utente cerca un termine generico
 * che non è il nome di un prodotto (es. "pasta", "legumi"): i prodotti restano
 * in testa ai risultati, le categorie vengono mostrate in coda dopo un
 * divisorio.
 */
data class CatalogCategoryResult(
    val id: Long,
    val name: String,
    val level1Name: String?,
    val level2Name: String?,
    val productCount: Int,
)
