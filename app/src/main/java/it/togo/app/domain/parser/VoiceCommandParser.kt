package it.togo.app.domain.parser

import it.togo.app.domain.model.StandardUnit
import java.util.Locale

/**
 * Servizio puro per il parsing di comandi vocali in italiano.
 *
 * Estrae dal testo grezzo:
 * - Nome prodotto (testo rimanente)
 * - Quantità (numeri + parole come "mezzo", "un", "due")
 * - Unità (kg, g, l, ml, pezzi, conf, barattolo, vasetto)
 * - Variante (integrale, biologico, intero, senza lattosio, ...)
 * - Categoria hint (parole chiave come "latticini", "bevande")
 *
 * Puro dominio: nessuna dipendenza Android, testabile su JVM.
 */
class VoiceCommandParser {

    private data class ProductPhrase(
        val productName: String,
        val variant: String?,
        val categoryHint: String?,
    )

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

        /**
         * Qualificatori che l'app tratta come variante della voce.
         *
         * Le frasi più lunghe vanno prima nell'alternanza, così "senza
         * lattosio" non viene spezzata e "biologico" non viene confuso con
         * "bio". Il match usa i confini di parola.
         */
        private val VARIANT_PHRASES = listOf(
            "parzialmente scremato",
            "parzialmente scremata",
            "senza lattosio",
            "senza glutine",
            "senza zucchero",
            "senza zuccheri",
            "senza sale",
            "biologico",
            "biologica",
            "integrale",
            "integrali",
            "scremato",
            "scremata",
            "vegetale",
            "vegetali",
            "intero",
            "intera",
            "classico",
            "classica",
            "light",
            "bio",
        )

        private val VARIANT_REGEX = Regex(
            "\\b(" + VARIANT_PHRASES.joinToString("|") { it.replace(" ", "\\s+") } + ")\\b",
            RegexOption.IGNORE_CASE,
        )

        private val WHITESPACE_REGEX = Regex("\\s+")
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
        val phrase = extractProductPhrase(singleProductText, quantity, unit)

        val confidence = computeConfidence(phrase.productName, quantity, unit, phrase.categoryHint)
        val needsClarification = quantity != null && unit == null

        return ParsedCommand(
            productName = phrase.productName,
            quantity = quantity,
            unit = unit,
            variant = phrase.variant,
            categoryHint = phrase.categoryHint,
            confidence = confidence,
            needsClarification = needsClarification
        )
    }

    /**
     * Normalizzazione: lowercase, trim e rimozione della punteggiatura finale.
     * Preserva gli accenti (no NFKD accent-folding).
     */
    private fun normalize(text: String): String =
        text.lowercase().trim().trimEnd('.', ',', ';', ':', '!', '?')

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

    /**
     * Estrae nome prodotto, variante e categoria hint dal testo ripulito.
     *
     * L'hint categoria viene rimosso solo quando è l'ultima parola della
     * frase (es. "latte 2 litri latticini"): se invece è la testa del prodotto
     * (es. "pane integrale", "1,5 kg di riso") resta nel nome. La variante
     * viene riconosciuta dentro la frase e rimossa dal nome prodotto.
     */
    private fun extractProductPhrase(
        text: String,
        quantity: Double?,
        unit: StandardUnit?,
    ): ProductPhrase {
        var remaining = text

        // Rimuovi unità
        if (unit != null) {
            for ((regex, _) in UNIT_PATTERNS) {
                remaining = regex.replace(remaining, " ")
            }
        }

        // Rimuovi quantità (numeri o parole italiane con word boundary)
        if (quantity != null) {
            remaining = remaining.replace(Regex("\\d+[.,]?\\d*"), " ")
            remaining = ITALIAN_NUMBER_WORDS_REGEX.replace(remaining, " ")
        }

        // Rimuovi preposizioni articolate (con word boundary completo)
        remaining = PREPOSITIONS_REGEX.replace(remaining, " ")

        // Rimuovi verbi d'azione
        remaining = ACTION_VERBS_REGEX.replace(remaining, " ")
        remaining = collapseSpaces(remaining)

        val tokens = remaining.split(' ').filter { it.isNotBlank() }
        val categoryHint: String?
        if (tokens.size > 1 && tokens.last().lowercase(Locale.ROOT) in CATEGORY_KEYWORDS) {
            // Hint esplicito in coda: "latte latticini" → prodotto "latte".
            categoryHint = tokens.last().lowercase(Locale.ROOT)
            remaining = tokens.dropLast(1).joinToString(" ")
        } else {
            // Parola categoria che fa parte del prodotto: non rimuoverla.
            // "pane integrale" → prodotto "pane", hint "pane".
            categoryHint = CATEGORY_KEYWORDS.firstOrNull { keyword ->
                Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn(remaining)
            }
        }

        val (productName, variant) = extractVariant(remaining)
        return ProductPhrase(
            productName = productName,
            variant = variant,
            categoryHint = categoryHint,
        )
    }

    /**
     * Riconosce i qualificatori di variante nella frase e li rimuove dal nome.
     *
     * Se la frase contiene solo qualificatori (es. "integrale"), non viene
     * svuotata: in quel caso resta tutto nel nome prodotto.
     */
    private fun extractVariant(text: String): Pair<String, String?> {
        val matches = VARIANT_REGEX.findAll(text).toList()
        if (matches.isEmpty()) return text to null

        val withoutVariant = buildString {
            var last = 0
            for (match in matches) {
                append(text, last, match.range.first)
                last = match.range.last + 1
            }
            append(text, last, text.length)
        }.let(::collapseSpaces)

        if (withoutVariant.isBlank()) return text to null

        val variant = matches
            .map { it.value.lowercase(Locale.ROOT) }
            .distinct()
            .joinToString(" ")
            .let(::capitalizeFirstWord)

        return withoutVariant to variant
    }

    private fun collapseSpaces(text: String): String =
        WHITESPACE_REGEX.replace(text, " ").trim()

    private fun capitalizeFirstWord(text: String): String {
        val collapsed = collapseSpaces(text)
        if (collapsed.isEmpty()) return ""
        val firstLetter = collapsed.indexOfFirst { it.isLetter() }
        if (firstLetter < 0) return collapsed
        return collapsed.substring(0, firstLetter) +
            collapsed[firstLetter].uppercaseChar() +
            collapsed.substring(firstLetter + 1)
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