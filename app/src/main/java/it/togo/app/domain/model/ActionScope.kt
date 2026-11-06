package it.togo.app.domain.model

/**
 * Ambito di un'azione (CAP-8).
 *
 * L'elenco è chiuso e l'ordine di [ordered] è quello di raggruppamento della
 * pagina "Da fare". Un valore sconosciuto ricade in [ALTRO]: non vengono mai
 * creati ambiti nuovi.
 */
enum class ActionScope(val label: String) {
    CASA("Casa"),
    PRATICHE("Pratiche"),
    SALUTE("Salute"),
    LAVORO("Lavoro"),
    ALTRO("Altro");

    companion object {
        /** Ordine di raggruppamento richiesto dalla spec. */
        val ordered: List<ActionScope> = listOf(CASA, PRATICHE, SALUTE, LAVORO, ALTRO)

        /**
         * Converte il valore persistito nel relativo ambito. Un valore
         * mancante o fuori dall'elenco fisso diventa [ALTRO] (CAP-8).
         */
        fun fromLabel(value: String?): ActionScope {
            val trimmed = value?.trim().orEmpty()
            return entries.firstOrNull { it.label.equals(trimmed, ignoreCase = true) } ?: ALTRO
        }
    }
}
