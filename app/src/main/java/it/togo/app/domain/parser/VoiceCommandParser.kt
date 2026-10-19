package it.togo.app.domain.parser

import it.togo.app.domain.model.StandardUnit

/**
 * Servizio puro per il parsing di comandi vocali in italiano.
 *
 * Estrae dal testo grezzo:
 * - Nome prodotto (testo rimanente)
 * - Quantità (numeri + parole come "mezzo", "un", "due")
 * - Unità (kg, g, l, ml, pezzi, conf, barattolo, vasetto)
 * - Categoria hint (parole chiave come "latticini", "bevande")
 *
 * Puro dominio: nessuna dipendenza Android, testabile su JVM.
 */
class VoiceCommandParser {

    companion object {
        private val ITALIAN_NUMBERS = mapOf(
            "un" to 1.0, "uno" to 1.0, "una" to 1.0,
            "due" to 2.0, "tre" to 3.0, "quattro" to 4.0,
            "cinque" to 5.0, "sei" to 6.0, "sette" to 7.0,
            "otto" to 8.0, "nove" to 9.0, "dieci" to 10.0,
            "mezzo" to 0.5, "mezza" to 0.5
        )

        private val UNIT_PATTERNS = listOf(
            Regex("\\b(chilo|chilos|kg)\\b", RegexOption.IGNORE_CASE) to StandardUnit.KILOGRAM,
            Regex("\\b(etto|etti|hg)\\b", RegexOption.IGNORE_CASE) to StandardUnit.GRAM,
            Regex("\\b(grammo|grammi|g)\\b", RegexOption.IGNORE_CASE) to StandardUnit.GRAM,
            Regex("\\b(litro|litri|l)\\b", RegexOption.IGNORE_CASE) to StandardUnit.LITER,
            Regex("\\b(millilitro|millilitri|ml)\\b", RegexOption.IGNORE_CASE) to StandardUnit.MILLILITER,
            Regex("\\b(pezzo|pezzi|pz)\\b", RegexOption.IGNORE_CASE) to StandardUnit.PIECE,
            Regex("\\b(confezione|conf)\\b", RegexOption.IGNORE_CASE) to StandardUnit.PACKAGE,
            Regex("\\b(barattolo|barattoli)\\b", RegexOption.IGNORE_CASE) to StandardUnit.JAR,
            Regex("\\b(vasetto|vasetti)\\b", RegexOption.IGNORE_CASE) to StandardUnit.POT,
            Regex("\\b(bottiglia|bottiglie|bt)\\b", RegexOption.IGNORE_CASE) to StandardUnit.BOTTLE
        )

        private val CATEGORY_KEYWORDS = listOf(
            "latticini", "bevande", "frutta", "verdura", "carne", "pesce",
            "pasta", "riso", "cereali", "surgelati", "dolci", "snack",
            "pane", "formaggi", "yogurt", "uova", "olio", "spezie",
            "detergenti", "igiene", "carta", "casa"
        )

        // Pattern per split multi-prodotto (es. "latte e caffè" → ["latte", "caffè"])
        private val MULTI_PRODUCT_SPLIT = Regex("\\b(e|ed)\\b", RegexOption.IGNORE_CASE)

        // Pattern per rimozione preposizioni articolate (con word boundary completo)
        private val PREPOSITIONS_REGEX = Regex("\\b(di|della|delle|degli|dello|del|dell)\\b", RegexOption.IGNORE_CASE)

        // Pattern per rimozione verbi d'azione
        private val ACTION_VERBS_REGEX = Regex("\\b(compra|aggiungi|metti|prendi|voglio)\\b", RegexOption.IGNORE_CASE)

        // Pattern per rimozione parole numeriche (con word boundary)
        private val ITALIAN_NUMBER_WORDS_REGEX = Regex("\\b(un|uno|una|due|tre|quattro|cinque|sei|sette|otto|nove|dieci|mezzo|mezza)\\b", RegexOption.IGNORE_CASE)
    }

    /**
     * Parsing di un comando vocale.
     *
     * @param rawText Testo grezzo dal riconoscimento vocale
     * @return ParsedCommand con campi estratti
     */
    fun parse(rawText: String): ParsedCommand {
        val normalized = normalize(rawText)
        if (normalized.isBlank()) {
            return ParsedCommand(
                productName = "",
                confidence = 0.0,
                needsClarification = true
            )
        }

        // MVP: split multi-prodotto e usa solo il primo
        val singleProductText = MULTI_PRODUCT_SPLIT.split(normalized).firstOrNull()?.trim() ?: normalized

        val quantity = extractQuantity(singleProductText)
        val unit = extractUnit(singleProductText)
        val categoryHint = extractCategoryHint(singleProductText)
        val productName = extractProductName(singleProductText, quantity, unit, categoryHint)

        val confidence = computeConfidence(productName, quantity, unit, categoryHint)
        val needsClarification = quantity != null && unit == null

        return ParsedCommand(
            productName = productName,
            quantity = quantity,
            unit = unit,
            categoryHint = categoryHint,
            confidence = confidence,
            needsClarification = needsClarification
        )
    }

    /** Normalizzazione: lowercase + trim, preserva accenti (no NFKD accent-folding) */
    private fun normalize(text: String): String =
        text.lowercase().trim()

    /** Estrae quantità (numeri decimali o parole italiane con word boundary) */
    private fun extractQuantity(text: String): Double? {
        // Numeri decimali (es. "2.5", "1,5")
        val numberRegex = Regex("(\\d+[.,]?\\d*)")
        val match = numberRegex.find(text)
        if (match != null) {
            val numStr = match.value.replace(',', '.')
            return numStr.toDoubleOrNull()
        }

        // Parole italiane (es. "mezzo", "due") — con word boundary
        for ((word, value) in ITALIAN_NUMBERS) {
            val regex = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                return value
            }
        }

        return null
    }

    /** Estrae unità di misura (mappa pattern regex → StandardUnit) */
    private fun extractUnit(text: String): StandardUnit? {
        for ((regex, unit) in UNIT_PATTERNS) {
            if (regex.containsMatchIn(text)) {
                return unit
            }
        }
        return null
    }

    /** Estrae hint categoria (parole chiave) */
    private fun extractCategoryHint(text: String): String? {
        for (keyword in CATEGORY_KEYWORDS) {
            if (text.contains(keyword, ignoreCase = true)) {
                return keyword
            }
        }
        return null
    }

    /** Estrae nome prodotto (testo rimanente dopo rimozione quantità/unità/categoria) */
    private fun extractProductName(
        text: String,
        quantity: Double?,
        unit: StandardUnit?,
        categoryHint: String?
    ): String {
        var remaining = text

        // Rimuovi unità
        if (unit != null) {
            for ((regex, _) in UNIT_PATTERNS) {
                remaining = regex.replace(remaining, "")
            }
        }

        // Rimuovi quantità (numeri o parole italiane con word boundary)
        if (quantity != null) {
            remaining = remaining.replace(Regex("\\d+[.,]?\\d*"), "")
            remaining = ITALIAN_NUMBER_WORDS_REGEX.replace(remaining, "")
        }

        // Rimuovi preposizioni articolate (con word boundary completo)
        remaining = PREPOSITIONS_REGEX.replace(remaining, " ")

        // Rimuovi verbi d'azione
        remaining = ACTION_VERBS_REGEX.replace(remaining, " ")

        // Rimuovi hint categoria solo se resta un nome prodotto.
        // Es. "1,5 kg di riso": "riso" è anche categoria, ma è l'unico testo
        // rimasto → va mantenuto come nome prodotto.
        if (categoryHint != null) {
            val withoutHint = remaining.replace(categoryHint, "", ignoreCase = true)
            if (withoutHint.isNotBlank()) remaining = withoutHint
        }

        // Normalizza spazi
        remaining = remaining.replace(Regex("\\s+"), " ").trim()

        return remaining
    }

    /**
     * Calcola confidenza in base ai campi estratti.
     *
     * Soglie:
     * - Solo prodotto: 0.8
     * - Prodotto + quantità: 0.9
     * - Prodotto + quantità + unità: 1.0
     * - Categoria hint: +0.05 bonus
     */
    private fun computeConfidence(
        productName: String,
        quantity: Double?,
        unit: StandardUnit?,
        categoryHint: String?
    ): Double {
        if (productName.isBlank()) return 0.0

        var score = 0.8  // Base: prodotto presente

        if (quantity != null) score += 0.1   // 0.9 con quantità
        if (unit != null) score += 0.1       // 1.0 con unità
        if (categoryHint != null) score += 0.05  // bonus categoria

        return score.coerceIn(0.0, 1.0)
    }
}