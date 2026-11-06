package it.togo.app.domain.action

/**
 * Validazione del testo di un'azione (CAP-2, CAP-6).
 *
 * Il testo deve essere non vuoto e stare su una sola riga. Il valore salvato
 * viene solo ripulito dagli spazi iniziali e finali: la forma scritta
 * dall'utente resta invariata, compresi accenti e spazi interni.
 */
object ActionTextValidator {

    const val EMPTY_TEXT_MESSAGE = "Il testo dell'azione non può essere vuoto."
    const val MULTILINE_TEXT_MESSAGE = "Il testo dell'azione deve stare su una sola riga."

    /** Restituisce il messaggio d'errore, oppure null se il testo è valido. */
    fun validate(raw: String): String? {
        val text = raw.trim()
        return when {
            text.isEmpty() -> EMPTY_TEXT_MESSAGE
            text.contains('\n') || text.contains('\r') -> MULTILINE_TEXT_MESSAGE
            else -> null
        }
    }

    /** Forma da persistire: trim senza alterare il resto del testo. */
    fun prepare(raw: String): String = raw.trim()
}
