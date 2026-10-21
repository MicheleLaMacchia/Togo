package it.togo.app.acceptance

import it.togo.app.domain.model.ResolutionResult
import it.togo.app.domain.resolver.ProductResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Audit SM-2 (Story 6.4) del resolver, usato come proxy della deduplicazione.
 *
 * Positivi: ogni sinonimo deve risolvere al prodotto atteso. Un `Single`
 * corretto è un true positive, un `Single` su un altro prodotto è un false
 * positive, `Ambiguous`/`None` sono false negative (il resolver non decide).
 * I nomi canonici verificano che prodotti distinti non collassino tra loro.
 *
 * La deduplicazione esatta lato lista è BLOCCATA: `DetectDuplicateUseCase` e
 * `ResolveDuplicateUseCase` sono commentate in `DomainModule.kt` e
 * `DuplicateResolutionDialog` non ha call site. Il test dedicato verifica la
 * tracciabilità documentale del blocco e non simula una metrica inesistente.
 */
class ResolutionDedupAuditTest {

    private data class Audit(
        val total: Int,
        val truePositives: Int,
        val falsePositives: Int,
        val falseNegatives: Int,
        val ambiguous: Int,
        val none: Int,
        val namesTotal: Int,
        val namesCorrect: Int,
    ) {
        val precision: Double
            get() = if (truePositives + falsePositives == 0) 1.0
            else truePositives.toDouble() / (truePositives + falsePositives)

        val recall: Double
            get() = if (truePositives + falseNegatives == 0) 1.0
            else truePositives.toDouble() / (truePositives + falseNegatives)
    }

    companion object {
        private val audit: Audit by lazy { runBlocking { computeAudit() } }

        private suspend fun computeAudit(): Audit {
            SqliteCatalogRepository.open().use { repository ->
                val resolver = ProductResolver(repository)
                val cases = repository.acceptanceCases()
                var truePositives = 0
                var falsePositives = 0
                var falseNegatives = 0
                var ambiguous = 0
                var none = 0
                var namesTotal = 0
                var namesCorrect = 0

                for (case in cases) {
                    val isName = case.kind == AcceptanceCaseKind.CANONICAL_NAME
                    if (isName) namesTotal++

                    when (val resolution = resolver.resolve(case.utterance)) {
                        is ResolutionResult.Single -> {
                            val correct =
                                resolution.resolved.product.id == case.expectedProductId
                            if (correct) {
                                truePositives++
                                if (isName) namesCorrect++
                            } else {
                                falsePositives++
                            }
                        }

                        is ResolutionResult.Ambiguous -> {
                            ambiguous++
                            falseNegatives++
                        }

                        ResolutionResult.None -> {
                            none++
                            falseNegatives++
                        }
                    }
                }

                return Audit(
                    total = cases.size,
                    truePositives = truePositives,
                    falsePositives = falsePositives,
                    falseNegatives = falseNegatives,
                    ambiguous = ambiguous,
                    none = none,
                    namesTotal = namesTotal,
                    namesCorrect = namesCorrect,
                )
            }
        }
    }

    @Test(timeout = 300_000)
    fun `resolver precision is at least 98 percent`() {
        val result = audit
        assertTrue(
            "corpus vuoto: nessun caso da misurare",
            result.total >= 200,
        )
        assertTrue(
            "nessun true positive: la precision non è misurabile",
            result.truePositives > 0,
        )
        val message = String.format(
            Locale.ROOT,
            "precision=%.4f su %d casi (soglia >= 0.98); TP=%d, FP=%d, " +
                "ambigui=%d, none=%d",
            result.precision,
            result.total,
            result.truePositives,
            result.falsePositives,
            result.ambiguous,
            result.none,
        )
        assertTrue(message, result.precision >= 0.98)
    }

    @Test(timeout = 300_000)
    fun `resolver recall is at least 95 percent`() {
        val result = audit
        assertTrue(
            "corpus vuoto: nessun caso da misurare",
            result.total >= 200,
        )
        val message = String.format(
            Locale.ROOT,
            "recall=%.4f su %d casi (soglia >= 0.95); TP=%d, FN=%d, " +
                "ambigui=%d, none=%d",
            result.recall,
            result.total,
            result.truePositives,
            result.falseNegatives,
            result.ambiguous,
            result.none,
        )
        assertTrue(message, result.recall >= 0.95)
    }

    @Test(timeout = 300_000)
    fun `distinct canonical product names resolve to themselves`() {
        val result = audit
        assertTrue("corpus vuoto: nessun caso da misurare", result.total >= 200)
        assertTrue("nessun nome canonico nel corpus", result.namesTotal > 0)
        assertTrue(
            "i nomi canonici distinti devono restare distinti: " +
                "${result.namesCorrect}/${result.namesTotal} risolti correttamente",
            result.namesCorrect == result.namesTotal,
        )
    }

    @Test
    fun `SM-2 exact duplicate block is documented and traceable`() {
        val deferred = readProjectFile("_bmad-output/implementation-artifacts/deferred-work.md")
        assertTrue(
            "la metrica BLOCCATA deve essere tracciata in deferred-work.md",
            deferred.contains("spec-6-4-privacy-audio-e-audit-performance.md") &&
                deferred.contains("duplicat", ignoreCase = true),
        )

        val audit = readProjectFile(
            "_bmad-output/implementation-artifacts/audit-6-4-privacy-performance.md"
        )
        assertTrue(
            "l'audit deve dichiarare BLOCCATA la deduplicazione esatta",
            audit.contains("Deduplicazione esatta lato lista", ignoreCase = true) &&
                audit.contains("BLOCCATA"),
        )
    }

    private fun readProjectFile(relativePath: String): String =
        projectFile(relativePath).readText()
}
