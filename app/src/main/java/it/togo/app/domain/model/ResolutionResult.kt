package it.togo.app.domain.model

/**
 * Risultato della risoluzione di un termine di ricerca.
 *
 * Sigillata per exhaustiveness checking in when().
 */
sealed interface ResolutionResult {
    /** Risoluzione univoca: un solo prodotto canonico trovato. */
    data class Single(val resolved: ResolvedProduct) : ResolutionResult

    /** Ambiguità: più prodotti candidati, richiesta disambiguazione UI. */
    data class Ambiguous(val candidates: List<ResolvedProduct>) : ResolutionResult

    /** Nessun risultato: termine vuoto, troppo corto, o nessun match. */
    data object None : ResolutionResult
}