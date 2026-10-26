package it.togo.app.domain.parser

import it.togo.app.domain.model.StandardUnit

/**
 * Comando vocale strutturato risultante dal parsing.
 *
 * @property productName Nome prodotto estratto (es. "latte", "caffè macinato")
 * @property quantity Quantità numerica (es. 2.0, 0.5) o null se non specificata
 * @property unit Unità di misura (es. L, KG) o null se non specificata
 * @property variant Qualificatore estratto (es. "Integrale", "Senza lattosio")
 *   o null se non presente. Viene applicato al campo variante della voce.
 * @property categoryHint Hint categoria (es. "latticini", "bevande") o null
 * @property confidence Livello di confidenza del parsing (0.0-1.0)
 * @property needsClarification True se manca quantità o unità e serve conferma
 */
data class ParsedCommand(
    val productName: String,
    val quantity: Double? = null,
    val unit: StandardUnit? = null,
    val variant: String? = null,
    val categoryHint: String? = null,
    val confidence: Double = 0.0,
    val needsClarification: Boolean = false
) {
    /** True se il comando è valido per creazione voce */
    val isValid: Boolean
        get() = productName.isNotBlank() && confidence > 0.0

    /** True se ha quantità e unità complete */
    val isComplete: Boolean
        get() = quantity != null && unit != null && !needsClarification

    /** Testo descrittivo per UI */
    val displayText: String
        get() = buildString {
            if (quantity != null) {
                append(quantity)
                append(" ")
                if (unit != null) {
                    append(unit.displayName)
                    append(" ")
                }
                append("di ")
            }
            append(productName)
            if (!variant.isNullOrBlank()) {
                append(" ")
                append(variant)
            }
            if (categoryHint != null) {
                append(" (")
                append(categoryHint)
                append(")")
            }
        }
}