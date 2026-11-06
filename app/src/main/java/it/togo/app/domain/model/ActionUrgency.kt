package it.togo.app.domain.model

/**
 * Urgenza di un'azione (CAP-7).
 *
 * L'elenco è chiuso e l'ordine di [ordered] è quello di ordinamento dentro un
 * ambito. Un valore sconosciuto ricade nel default [A_TEMPO_PERSO].
 */
enum class ActionUrgency(val label: String) {
    URGENTE("Urgente"),
    A_TEMPO_PERSO("A tempo perso"),
    ECCEZIONALE("Eccezionale");

    companion object {
        /** Ordine di ordinamento richiesto dalla spec. */
        val ordered: List<ActionUrgency> = listOf(URGENTE, A_TEMPO_PERSO, ECCEZIONALE)

        /**
         * Converte il valore persistito nella relativa urgenza. Un valore
         * mancante o fuori dall'elenco ricade nel default.
         */
        fun fromLabel(value: String?): ActionUrgency {
            val trimmed = value?.trim().orEmpty()
            return entries.firstOrNull { it.label.equals(trimmed, ignoreCase = true) }
                ?: A_TEMPO_PERSO
        }
    }
}
