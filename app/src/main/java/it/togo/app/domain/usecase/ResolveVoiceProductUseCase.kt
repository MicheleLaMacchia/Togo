package it.togo.app.domain.usecase

import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Risultato della risoluzione di un comando vocale sul catalogo.
 *
 * [products] contiene i candidati trovati: un solo elemento significa che il
 * comando identifica un prodotto in modo univoco, più elementi richiedono una
 * scelta dell'utente.
 */
data class VoiceProductResolution(
    val query: String,
    val products: List<CanonicalProduct>,
)

/**
 * Risolve prodotto e variante di un comando vocale.
 *
 * Priorità:
 * 1. match esatto del nome per una frase composta (es. "pane integrale"
 *    → "Pane Integrale", anche se la ricerca LIKE restituisce anche il Bio);
 * 2. tutti i risultati della frase completa;
 * 3. la sola testa del prodotto (es. "pane senza glutine" → "pane").
 *
 * Una singola parola generica ("pane", "latte") non viene forzata sul prodotto
 * con nome esatto: è più utile mostrare le alternative.
 */
class ResolveVoiceProductUseCase(
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(command: ParsedCommand): VoiceProductResolution {
        val productName = command.productName.trim()
        val variant = command.variant?.trim().orEmpty()
        val fullQuery = listOf(productName, variant)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase(Locale.ROOT)

        val fullResults = searchFirst(fullQuery)
        // Il match esatto è una scorciatoia sicura solo quando la frase è
        // composta (prodotto + variante o prodotto multi-parola): per una
        // singola parola generica ("pane") è meglio mostrare le alternative.
        if (fullQuery.contains(' ')) {
            val exactFull = exactMatch(fullResults, fullQuery)
            if (exactFull.isNotEmpty()) {
                return VoiceProductResolution(fullQuery, exactFull)
            }
        }
        if (fullResults.isNotEmpty()) {
            return VoiceProductResolution(fullQuery, fullResults)
        }

        if (productName.isNotBlank()) {
            val baseResults = searchFirst(productName)
            if (baseResults.isNotEmpty()) {
                return VoiceProductResolution(productName, baseResults)
            }

            // Fallback per nomi con preposizioni rimosse dal parser
            // (es. "succo frutta" → "succo").
            val head = productName.substringBefore(' ').trim()
            if (head.length >= 2 && !head.equals(productName, ignoreCase = true)) {
                val headResults = searchFirst(head)
                if (headResults.isNotEmpty()) {
                    return VoiceProductResolution(head, headResults)
                }
            }
        }
        return VoiceProductResolution(fullQuery, emptyList())
    }

    /**
     * Un match esatto è "univoco" solo se il catalogo contiene una sola voce
     * con quel nome normalizzato.
     */
    private fun exactMatch(
        products: List<CanonicalProduct>,
        query: String,
    ): List<CanonicalProduct> {
        val normalizedQuery = ProductNameNormalizer.normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        val matches = products.filter {
            ProductNameNormalizer.normalize(it.name) == normalizedQuery
        }
        return matches.takeIf { it.size == 1 }.orEmpty()
    }

    private suspend fun searchFirst(query: String): List<CanonicalProduct> =
        try {
            if (query.length >= 2) catalogRepository.search(query).first() else emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
}
