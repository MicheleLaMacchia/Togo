package it.togo.app.domain.usecase

import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.parser.VoiceCommandParser

/**
 * Use Case per il parsing di comandi vocali.
 *
 * Orchestra [VoiceCommandParser] e validazione del risultato.
 */
class ParseVoiceCommandUseCase(
    private val parser: VoiceCommandParser
) {

    /**
     * Parsing di un comando vocale.
     *
     * @param rawText Testo grezzo dal riconoscimento vocale
     * @return ParsedCommand con campi estratti
     */
    operator fun invoke(rawText: String): ParsedCommand {
        val parsed = parser.parse(rawText)

        // Validazione: se prodotto vuoto, confidence = 0
        return if (parsed.productName.isBlank()) {
            parsed.copy(confidence = 0.0, needsClarification = true)
        } else {
            parsed
        }
    }
}