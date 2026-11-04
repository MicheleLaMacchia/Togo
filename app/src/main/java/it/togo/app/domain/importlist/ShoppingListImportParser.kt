package it.togo.app.domain.importlist

import it.togo.app.domain.model.StandardUnit
import java.util.Locale

/**
 * Risultato della lettura di un testo esportato da TOGO.
 *
 * Il parser valida solo la struttura e le righe; la corrispondenza con la
 * tassonomia del catalogo è responsabilità di
 * [it.togo.app.domain.usecase.ImportShoppingListUseCase].
 */
sealed interface ImportParseResult {
    data class Success(val list: ParsedShoppingList) : ImportParseResult
    data class Error(val message: String) : ImportParseResult
}

/** Lista completa letta dal testo, già divisa in categorie. */
data class ParsedShoppingList(
    val sections: List<ParsedSection>,
) {
    val itemCount: Int
        get() = sections.sumOf { section ->
            section.level2s.sumOf { level2 ->
                level2.level3s.sumOf { level3 -> level3.items.size }
            }
        }
}

data class ParsedSection(
    val name: String,
    val level2s: List<ParsedLevel2>,
)

data class ParsedLevel2(
    val name: String,
    val level3s: List<ParsedLevel3>,
)

data class ParsedLevel3(
    val name: String,
    val items: List<ParsedItem>,
)

/**
 * Una riga prodotto dell'export.
 *
 * [description] è la riga senza la quantità finale: contiene ancora prodotto,
 * marca e variante (es. "Spaghetti Barilla integrale").
 */
data class ParsedItem(
    val lineNumber: Int,
    val description: String,
    val quantity: Double,
    val unit: StandardUnit?,
)

/**
 * Parser del formato prodotto da [it.togo.app.domain.usecase.ShareListUseCase]:
 *
 * ```
 * [FRUTTA E VERDURA]
 * + FRUTTA FRESCA
 * - MELE
 *   Golden Delicious 2 pz
 * ```
 *
 * Le categorie sono accettate in qualunque combinazione di maiuscole/minuscole:
 * il confronto con il catalogo ignora accenti e case. La struttura invece deve
 * essere quella dell'export: intestazioni a inizio riga, voci con due spazi.
 */
object ShoppingListImportParser {

    private val SECTION_PATTERN = Regex("""^\[([^\]]+)]$""")
    private val QUANTITY_PATTERN = Regex("""^(.*?)\s+(\d+(?:[.,]\d+)?)\s+(\p{L}+)$""")

    fun parse(rawText: String): ImportParseResult {
        if (rawText.isBlank()) {
            return ImportParseResult.Error(
                "Il testo è vuoto. Incolla la lista esportata da TOGO."
            )
        }

        val normalizedText = rawText.replace("\r\n", "\n").replace('\r', '\n')
        val sections = mutableListOf<MutableSection>()
        var currentSection: MutableSection? = null
        var currentLevel2: MutableLevel2? = null
        var currentLevel3: MutableLevel3? = null

        for ((index, rawLine) in normalizedText.lines().withIndex()) {
            val lineNumber = index + 1
            if (rawLine.isBlank()) continue

            when {
                rawLine.startsWith("[") -> {
                    val header = SECTION_PATTERN.matchEntire(rawLine.trim())
                    if (header == null) {
                        return error(
                            lineNumber,
                            "la categoria deve essere racchiusa tra parentesi quadre, " +
                                "ad esempio [FRUTTA E VERDURA]."
                        )
                    }
                    val name = header.groupValues[1].trim()
                    if (name.isEmpty()) {
                        return error(lineNumber, "il nome della categoria è vuoto.")
                    }
                    currentSection = MutableSection(name, lineNumber).also { sections += it }
                    currentLevel2 = null
                    currentLevel3 = null
                }

                rawLine.startsWith("+ ") -> {
                    val section = currentSection ?: return error(
                        lineNumber,
                        "la sottocategoria \"+ ...\" deve stare sotto una categoria [ ... ]."
                    )
                    val name = rawLine.removePrefix("+ ").trim()
                    if (name.isEmpty()) {
                        return error(lineNumber, "il nome della sottocategoria è vuoto.")
                    }
                    currentLevel2 = MutableLevel2(name, lineNumber).also { section.level2s += it }
                    currentLevel3 = null
                }

                rawLine.startsWith("- ") -> {
                    val level2 = currentLevel2 ?: return error(
                        lineNumber,
                        "la categoria \"- ...\" deve stare sotto una sottocategoria \"+ ...\"."
                    )
                    val name = rawLine.removePrefix("- ").trim()
                    if (name.isEmpty()) {
                        return error(lineNumber, "il nome della categoria è vuoto.")
                    }
                    currentLevel3 = MutableLevel3(name, lineNumber).also { level2.level3s += it }
                }

                rawLine.startsWith("  ") -> {
                    val level3 = currentLevel3 ?: return error(
                        lineNumber,
                        "la voce prodotto deve stare sotto una categoria \"- ...\"."
                    )
                    val description = rawLine.trim()
                    if (description.isEmpty()) {
                        return error(lineNumber, "la voce prodotto è vuota.")
                    }
                    val parsed = parseQuantity(description)
                    level3.items += ParsedItem(
                        lineNumber = lineNumber,
                        description = parsed.description,
                        quantity = parsed.quantity,
                        unit = parsed.unit,
                    )
                }

                else -> return error(
                    lineNumber,
                    "la riga non è nel formato dell'export. Attesi [CATEGORIA], " +
                        "+ SOTTOCATEGORIA, - CATEGORIA oppure una voce indentata con due spazi."
                )
            }
        }

        if (sections.isEmpty()) {
            return ImportParseResult.Error(
                "Il testo non contiene categorie. Incolla la lista esportata da TOGO."
            )
        }

        val parsedSections = mutableListOf<ParsedSection>()
        for (section in sections) {
            if (section.level2s.isEmpty()) {
                return error(
                    section.lineNumber,
                    "la categoria \"${section.name}\" non contiene sottocategorie (+ ...)."
                )
            }
            val parsedLevel2s = mutableListOf<ParsedLevel2>()
            for (level2 in section.level2s) {
                if (level2.level3s.isEmpty()) {
                    return error(
                        level2.lineNumber,
                        "la sottocategoria \"${level2.name}\" non contiene categorie (- ...)."
                    )
                }
                val parsedLevel3s = mutableListOf<ParsedLevel3>()
                for (level3 in level2.level3s) {
                    if (level3.items.isEmpty()) {
                        return error(
                            level3.lineNumber,
                            "la categoria \"${level3.name}\" non contiene prodotti."
                        )
                    }
                    parsedLevel3s += ParsedLevel3(level3.name, level3.items)
                }
                parsedLevel2s += ParsedLevel2(level2.name, parsedLevel3s)
            }
            parsedSections += ParsedSection(section.name, parsedLevel2s)
        }

        val list = ParsedShoppingList(parsedSections)
        if (list.itemCount == 0) {
            return ImportParseResult.Error(
                "Il testo non contiene prodotti da importare."
            )
        }
        return ImportParseResult.Success(list)
    }

    private fun parseQuantity(description: String): ParsedQuantity {
        val match = QUANTITY_PATTERN.matchEntire(description)
            ?: return ParsedQuantity(description, 0.0, null)

        val productText = match.groupValues[1].trim()
        val unit = unitFromToken(match.groupValues[3])
        if (productText.isEmpty() || unit == null) {
            return ParsedQuantity(description, 0.0, null)
        }

        val quantity = match.groupValues[2]
            .replace(',', '.')
            .toDoubleOrNull()
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
            return ParsedQuantity(description, 0.0, null)
        }

        return ParsedQuantity(productText, quantity, unit)
    }

    /**
     * Accetta le forme stampate dall'export e alcuni sinonimi comuni, così il
     * testo copiato da un client di posta che aggiunge spazi non viene perso.
     */
    private fun unitFromToken(token: String): StandardUnit? =
        when (token.lowercase(Locale.ROOT)) {
            "g", "gr", "grammo", "grammi" -> StandardUnit.GRAM
            "kg", "chilo", "chili", "chilogrammo", "chilogrammi" -> StandardUnit.KILOGRAM
            "ml" -> StandardUnit.MILLILITER
            "l", "lt", "litro", "litri" -> StandardUnit.LITER
            "pz", "pezzo", "pezzi" -> StandardUnit.PIECE
            "conf", "confezione", "confezioni" -> StandardUnit.PACKAGE
            "bt", "bottiglia", "bottiglie" -> StandardUnit.BOTTLE
            "vas", "vasetto", "vasetti" -> StandardUnit.JAR
            "latt", "lattina", "lattine" -> StandardUnit.CAN
            "scat", "scatola", "scatole" -> StandardUnit.BOX
            "rot", "rotolo", "rotoli" -> StandardUnit.ROLL
            "pent", "pentola", "pentole" -> StandardUnit.POT
            else -> null
        }

    private fun error(lineNumber: Int, detail: String): ImportParseResult.Error =
        ImportParseResult.Error("Riga $lineNumber: $detail")

    private data class ParsedQuantity(
        val description: String,
        val quantity: Double,
        val unit: StandardUnit?,
    )

    private class MutableSection(val name: String, val lineNumber: Int) {
        val level2s = mutableListOf<MutableLevel2>()
    }

    private class MutableLevel2(val name: String, val lineNumber: Int) {
        val level3s = mutableListOf<MutableLevel3>()
    }

    private class MutableLevel3(val name: String, val lineNumber: Int) {
        val items = mutableListOf<ParsedItem>()
    }
}
