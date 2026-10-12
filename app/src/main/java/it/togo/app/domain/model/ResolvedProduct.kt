package it.togo.app.domain.model

/**
 * Risultato della risoluzione di un termine verso un prodotto canonico.
 *
 * @property product Prodotto canonico risolto
 * @property matchedTerm Il termine effettivamente matchato (sinonimo o nome)
 * @property confidence Score di confidenza: 1.0 = match esatto, <1.0 = match parziale
 */
data class ResolvedProduct(
    val product: CanonicalProduct,
    val matchedTerm: String,
    val confidence: Double
) {
    init {
        require(confidence in 0.0..1.0) { "confidence deve essere in [0.0, 1.0]" }
    }
}