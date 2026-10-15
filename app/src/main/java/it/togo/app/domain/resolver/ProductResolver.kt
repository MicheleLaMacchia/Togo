package it.togo.app.domain.resolver

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ResolutionResult
import it.togo.app.domain.model.ResolvedProduct
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolver di sinonimi verso prodotti canonici.
 *
 * Logica di risoluzione:
 * 1. Normalizza il termine (trim, lowercase, accent-fold NFKD)
 * 2. Se termine vuoto o troppo corto (<2 char) → None
 * 3. Cerca match esatti nella tabella SYNONYM (via CatalogRepository.search)
 * 4. Se match multipli con score uguale → Ambiguous
 * 5. Se un solo match → Single con confidence calcolata
 * 6. Se nessun match → None
 *
 * Score di confidenza:
 * - 1.0 = match esatto (termine == sinonimo)
 * - 0.9 = match per prefisso (termine inizia con sinonimo)
 * - 0.7 = match per sottostringa (termine contiene sinonimo)
 * - <0.5 = scartato (soglia minima)
 *
 * NON modifica il database: solo lettura e matching in memoria.
 */
class ProductResolver(
    private val catalogRepository: CatalogRepository
) {

    /**
     * Massima lunghezza termine di ricerca.
     */
    private val MAX_QUERY_LENGTH = 100

    /**
     * Minima lunghezza termine per avviare la ricerca.
     */
    private val MIN_QUERY_LENGTH = 2

    /**
     * Soglia minima di confidenza per considerare un match valido.
     */
    private val MIN_CONFIDENCE = 0.5

    /**
     * Numero massimo di candidati in caso di ambiguità.
     */
    private val MAX_CANDIDATES = 10

    /**
     * Risolve un termine verso un prodotto canonico.
     *
     * @param term Termine grezzo (es. "latte intero", "Caffè")
     * @return ResolutionResult: Single, Ambiguous, o None
     */
    suspend fun resolve(term: String): ResolutionResult = withContext(Dispatchers.Default) {
        val normalized = normalize(term)

        if (normalized.length < MIN_QUERY_LENGTH) {
            return@withContext ResolutionResult.None
        }

        // Cerca prodotti corrispondenti (max 50 dal DB)
        val candidates = catalogRepository.search(normalized)
            .firstOrNull()
            .orEmpty()
            .mapNotNull { product ->
                val score = calculateConfidence(normalized, product)
                if (score >= MIN_CONFIDENCE) {
                    ResolvedProduct(product, product.name, score)
                } else null
            }
            .sortedByDescending { it.confidence }
            .take(MAX_CANDIDATES)

        return@withContext when {
            candidates.isEmpty() -> ResolutionResult.None
            candidates.size == 1 -> ResolutionResult.Single(candidates.first())
            else -> {
                // Verifica se il migliore score è univoco
                val bestScore = candidates.first().confidence
                val bestMatches = candidates.filter { it.confidence == bestScore }
                if (bestMatches.size == 1) {
                    ResolutionResult.Single(bestMatches.first())
                } else {
                    ResolutionResult.Ambiguous(bestMatches)
                }
            }
        }
    }

    /**
     * Normalizza il termine di ricerca.
     * - trim + lowercase
     * - decomposizione NFKD
     * - rimozione segni diacritici
     * - troncamento a MAX_QUERY_LENGTH
     */
    private fun normalize(term: String): String {
        if (term.isBlank()) return ""
        val decomposed = java.text.Normalizer.normalize(term.trim().lowercase(), java.text.Normalizer.Form.NFKD)
        val folded = buildString {
            for (ch in decomposed) {
                val type = Character.getType(ch)
                if (type != Character.NON_SPACING_MARK.toInt() &&
                    type != Character.COMBINING_SPACING_MARK.toInt() &&
                    type != Character.ENCLOSING_MARK.toInt()
                ) {
                    append(ch)
                }
            }
        }
        return if (folded.length > MAX_QUERY_LENGTH) folded.take(MAX_QUERY_LENGTH) else folded
    }

    /**
     * Calcola lo score di confidenza per un prodotto candidato.
     *
     * @param normalizedQuery Query normalizzata
     * @param product Prodotto canonico candidato
     * @return Score di confidenza in [0.0, 1.0]
     */
    private fun calculateConfidence(normalizedQuery: String, product: CanonicalProduct): Double {
        val normalizedName = normalize(product.name)

        return when {
            normalizedQuery == normalizedName -> 1.0
            normalizedName.startsWith(normalizedQuery) -> 0.9
            normalizedName.contains(normalizedQuery) -> 0.7
            else -> 0.0
        }
    }
}