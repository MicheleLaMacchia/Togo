package it.togo.app.acceptance

import it.togo.app.domain.category.CategoryAssigner
import it.togo.app.domain.model.ResolutionResult
import it.togo.app.domain.resolver.ProductResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Accettazione SM-1 (Story 6.4): accuratezza di assegnazione della categoria
 * canonica su un corpus derivato dal catalogo reale.
 *
 * Corpus: per ogni prodotto il nome canonico e tutti i sinonimi (oltre 2.000
 * casi sul catalogo corrente, ben oltre il minimo di 200). Non vengono inventate
 * utterance: ogni riga proviene da `CANONICAL_PRODUCT` o `SYNONYM`.
 *
 * Metrica: un caso è corretto solo se `ProductResolver` restituisce un
 * `Single` e `CategoryAssigner` assegna il `level3_id` atteso. Ambiguità e
 * nessun match contano come non assegnati: non vengono dichiarati corretti
 * per non gonfiare la metrica.
 */
class CategoryAccuracyAcceptanceTest {

    private val minimumCorpusSize = 200
    private val targetAccuracy = 0.95

    @Test(timeout = 300_000)
    fun `category accuracy on the real catalog corpus is at least 95 percent`() = runBlocking {
        SqliteCatalogRepository.open().use { repository ->
            val cases = repository.acceptanceCases()
            assertTrue(
                "corpus troppo piccolo: ${cases.size} casi, minimo $minimumCorpusSize",
                cases.size >= minimumCorpusSize,
            )

            val resolver = ProductResolver(repository)
            val assigner = CategoryAssigner(repository, EmptyLearnedRulesRepository)

            var correct = 0
            var single = 0
            var ambiguous = 0
            var none = 0
            var wrongProduct = 0
            var assignerCalls = 0
            var assignerWrong = 0

            for (case in cases) {
                when (val resolution = resolver.resolve(case.utterance)) {
                    is ResolutionResult.Single -> {
                        single++
                        val productCorrect =
                            resolution.resolved.product.level3Id == case.expectedLevel3Id
                        if (!productCorrect) wrongProduct++

                        // L'assigner viene misurato su ogni Single, non solo sui
                        // prodotti già corretti: altrimenti la metrica sarebbe
                        // condizionata dal resolver e non testerebbe l'assigner.
                        val assignment = assigner.assign(
                            resolution.resolved.product,
                            case.utterance,
                        )
                        assignerCalls++
                        if (assignment.suggestedLevel3Id != case.expectedLevel3Id) {
                            assignerWrong++
                        }
                        if (productCorrect &&
                            assignment.suggestedLevel3Id == case.expectedLevel3Id
                        ) {
                            correct++
                        }
                    }

                    is ResolutionResult.Ambiguous -> ambiguous++
                    ResolutionResult.None -> none++
                }
            }

            assertEquals(
                "l'assigner deve essere invocato per ogni resolution Single",
                single,
                assignerCalls,
            )

            val accuracy = correct.toDouble() / cases.size
            val message = String.format(
                Locale.ROOT,
                "accuracy=%.4f su %d casi (soglia >= %.2f); corretti=%d, single=%d, " +
                    "ambigui=%d, none=%d, prodotto errato=%d, assigner errati=%d",
                accuracy,
                cases.size,
                targetAccuracy,
                correct,
                single,
                ambiguous,
                none,
                wrongProduct,
                assignerWrong,
            )
            assertTrue(message, accuracy >= targetAccuracy)
        }
    }
}
