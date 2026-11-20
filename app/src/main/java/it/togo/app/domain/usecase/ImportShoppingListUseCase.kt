package it.togo.app.domain.usecase

import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.importlist.ImportParseResult
import it.togo.app.domain.importlist.ParsedItem
import it.togo.app.domain.importlist.ShoppingListImportParser
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ProductAttribute
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.UserAttributeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/** Una voce pronta per l'importazione, con il percorso di categoria risolto. */
data class PlannedImportItem(
    val level2Id: Long,
    val level2Name: String,
    val level3Id: Long?,
    val level3Name: String,
    val level1Name: String,
    val productId: Long?,
    val productName: String,
    val quantity: Double,
    val unit: StandardUnit,
    val brand: String?,
    val variant: String?,
    val lineNumber: Int,
)

/**
 * Piano di importazione calcolato senza scrivere nel database.
 *
 * [productCount] è il numero di righe prodotto del testo: è il numero mostrato
 * nella conferma prima di procedere.
 */
data class ImportPlan(
    val items: List<PlannedImportItem>,
    val actions: List<ActionItem> = emptyList(),
    /** Duplicate action entries omitted from the parsed input during preview. */
    val skippedDuplicateActions: Int = 0,
) {
    val productCount: Int
        get() = items.size

    val actionCount: Int
        get() = actions.size

    val newLevel3Count: Int
        get() = items.asSequence()
            .filter { it.level3Id == null }
            .map { it.level2Id to ProductNameNormalizer.normalize(it.level3Name) }
            .distinct()
            .count()

    val newProductCount: Int
        get() = items.asSequence()
            .filter { it.productId == null }
            .map {
                Triple(
                    it.level2Id,
                    ProductNameNormalizer.normalize(it.level3Name),
                    ProductNameNormalizer.normalize(it.productName),
                )
            }
            .distinct()
            .count()
}

sealed interface ImportPreviewResult {
    data class Ready(val plan: ImportPlan) : ImportPreviewResult
    data class Error(val message: String) : ImportPreviewResult
}

sealed interface ImportExecutionResult {
    data class Success(
        val imported: Int,
        val importedActions: Int,
        val updatedExisting: Int,
        val createdProducts: Int,
        val createdCategories: Int,
        /** Total duplicate action entries omitted during preview or execution. */
        val skippedDuplicateActions: Int = 0,
    ) : ImportExecutionResult

    data class Error(val message: String) : ImportExecutionResult
}

/**
 * Importa una lista esportata da TOGO.
 *
 * - La struttura del testo è validata da [ShoppingListImportParser].
 * - Le categorie di primo e secondo livello devono esistere nel catalogo.
 * - Una categoria di terzo livello mancante viene creata sotto il livello 2
 *   indicato dal testo.
 * - Un prodotto mancante viene creato nella categoria di terzo livello.
 * - Se il prodotto è già nella lista attiva, la quantità viene sostituita.
 */
class ImportShoppingListUseCase(
    private val catalogRepository: CatalogRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val userAttributeRepository: UserAttributeRepository,
) {

    private val whitespace = Regex("\\s+")
    private val invalidUrgencyMessage =
        "Urgenza non valida: usa Urgente, A tempo perso o Eccezionale."

    /**
     * Legge il testo e prepara il piano senza modificare il database.
     * È il passo che alimenta la modale di conferma.
     */
    suspend fun preview(rawText: String): ImportPreviewResult {
        val parsed = when (val result = ShoppingListImportParser.parse(rawText)) {
            is ImportParseResult.Success -> result.list
            is ImportParseResult.Error -> return ImportPreviewResult.Error(result.message)
        }

        val actions = mutableListOf<ActionItem>()
        val seenActionKeys = mutableSetOf<Triple<String, ActionScope, ActionUrgency>>()
        var skippedDuplicateActions = 0
        var parsedActionCount = 0
        var actionScope = ActionScope.ALTRO
        var actionUrgency: ActionUrgency? = null
        val actionLines = parsed.actionSection?.lines.orEmpty()
        if (parsed.actionSection != null &&
            actionLines.none { it.rawLine.startsWith("  ") && it.rawLine.trim().isNotEmpty() } &&
            actionLines.all {
                it.rawLine.isBlank() ||
                    it.rawLine.startsWith("+ ") ||
                    (it.rawLine.startsWith("- ") && ActionUrgency.entries.any { urgency ->
                        urgency.label.equals(
                            it.rawLine.removePrefix("- ").trim(),
                            ignoreCase = true,
                        )
                    })
            }
        ) {
            return ImportPreviewResult.Error("Il blocco [DA FARE] è vuoto.")
        }
        actionLines.forEach { line ->
            when {
                line.rawLine.isBlank() -> Unit
                line.rawLine.startsWith("+ ") -> {
                    actionScope = ActionScope.fromLabel(line.rawLine.removePrefix("+ "))
                    actionUrgency = null
                }
                line.rawLine.startsWith("- ") -> {
                    val label = line.rawLine.removePrefix("- ").trim()
                    actionUrgency = ActionUrgency.entries.firstOrNull {
                        it.label.equals(label, ignoreCase = true)
                    } ?: return ImportPreviewResult.Error(
                        "Riga ${line.lineNumber}: $invalidUrgencyMessage"
                    )
                }
                line.rawLine.startsWith("  ") && line.rawLine.trim().isNotEmpty() -> {
                    val urgency = actionUrgency
                        ?: return ImportPreviewResult.Error(
                            "Riga ${line.lineNumber}: $invalidUrgencyMessage"
                        )
                    parsedActionCount++
                    val action = ActionItem(
                        text = line.rawLine.trim(),
                        scope = actionScope,
                        urgency = urgency,
                    )
                    val key = Triple(action.normalizedText, action.scope, action.urgency)
                    if (!seenActionKeys.add(key)) {
                        skippedDuplicateActions++
                    } else if (shoppingListRepository.checkDuplicateAction(
                            normalizedText = action.normalizedText,
                            scope = action.scope,
                            urgency = action.urgency,
                        )
                    ) {
                        skippedDuplicateActions++
                    } else {
                        actions += action
                    }
                }
                else -> return ImportPreviewResult.Error(
                    "Riga ${line.lineNumber}: formato non valido nel blocco [DA FARE]. " +
                        "Attesi + AMBITO, - URGENZA o un'azione indentata con due spazi."
                )
            }
        }

        if (parsed.sections.isEmpty()) {
            if (parsedActionCount == 0) {
                return ImportPreviewResult.Error("Il testo non contiene prodotti da importare.")
            }
            return ImportPreviewResult.Ready(
                ImportPlan(
                    items = emptyList(),
                    actions = actions,
                    skippedDuplicateActions = skippedDuplicateActions,
                ),
            )
        }

        val taxonomy = catalogRepository.getTaxonomy().first()
        val level1s = taxonomy.filterIsInstance<TaxonomyLevel.Level1>()
        val level2s = taxonomy.filterIsInstance<TaxonomyLevel.Level2>()
        val level3s = taxonomy.filterIsInstance<TaxonomyLevel.Level3>()

        val knownBrands = knownBrands()
        val knownVariants = knownVariants()

        val planned = mutableListOf<PlannedImportItem>()
        val productsByLevel3 = mutableMapOf<Long, List<CanonicalProduct>>()

        for (section in parsed.sections) {
            val level1 = level1s.firstOrNull { sameName(it.name, section.name) }
            if (level1 == null) {
                for (level2 in section.level2s) {
                    for (level3 in level2.level3s) {
                        for (item in level3.items) {
                            val legacyItem = resolveLegacyCategoryItem(item, knownBrands, knownVariants)
                                ?: return ImportPreviewResult.Error(
                                    "Riga ${item.lineNumber}: la categoria legacy \"${section.name}\" " +
                                        "non esiste e il prodotto \"${item.description}\" non è riconoscibile " +
                                        "nel catalogo aggiornato."
                                )
                            planned += legacyItem
                        }
                    }
                }
                continue
            }

            for (level2 in section.level2s) {
                val parent2 = level2s.firstOrNull {
                    it.level1Id == level1.id && sameName(it.name, level2.name)
                }
                if (parent2 == null) {
                    for (level3 in level2.level3s) {
                        for (item in level3.items) {
                            val legacyItem = resolveLegacyCategoryItem(item, knownBrands, knownVariants)
                                ?: return ImportPreviewResult.Error(
                                    "Riga ${item.lineNumber}: la sottocategoria legacy \"${level2.name}\" " +
                                        "non esiste in \"${level1.name}\" e il prodotto " +
                                        "\"${item.description}\" non è riconoscibile nel catalogo aggiornato."
                                )
                            planned += legacyItem
                        }
                    }
                    continue
                }

                for (level3 in level2.level3s) {
                    val existingLevel3 = level3s.firstOrNull {
                        it.level2Id == parent2.id && sameName(it.name, level3.name)
                    }
                    val products = existingLevel3?.let { existing ->
                        productsByLevel3.getOrPut(existing.id) {
                            catalogRepository.getByLevel3(existing.id)
                        }
                    }.orEmpty()

                    for (item in level3.items) {
                        val exactSynonym = catalogRepository.getByExactSynonym(item.description)
                        val synonymBelongsToAnotherCategory = exactSynonym != null &&
                            existingLevel3 != null && exactSynonym.level3Id != existingLevel3.id
                        val movedLegacyProduct = if (existingLevel3 == null || synonymBelongsToAnotherCategory) {
                            resolveLegacyCategoryItem(item, knownBrands, knownVariants)
                        } else {
                            null
                        }
                        if (movedLegacyProduct != null) {
                            planned += movedLegacyProduct
                            continue
                        }
                        val resolved = exactSynonym?.let { exact ->
                            ResolvedLine(
                                product = exact,
                                productName = exact.name,
                                brand = null,
                                variant = "Bio".takeIf { containsBioTerm(item.description) },
                            )
                        } ?: resolveItem(
                            item = item,
                            products = products,
                            knownBrands = knownBrands,
                            knownVariants = knownVariants,
                        )
                        planned += PlannedImportItem(
                            level2Id = parent2.id,
                            level2Name = parent2.name,
                            level3Id = existingLevel3?.id,
                            level3Name = existingLevel3?.name
                                ?: ProductNameNormalizer.capitalizeFirstWord(level3.name),
                            level1Name = level1.name,
                            productId = resolved.product?.id,
                            productName = resolved.product?.name ?: resolved.productName,
                            quantity = item.quantity,
                            unit = item.unit ?: StandardUnit.PIECE,
                            brand = resolved.brand,
                            variant = resolved.variant,
                            lineNumber = item.lineNumber,
                        )
                    }
                }
            }
        }

        if (planned.isEmpty() && parsedActionCount == 0) {
            return ImportPreviewResult.Error("Il testo non contiene prodotti da importare.")
        }
        return ImportPreviewResult.Ready(
            ImportPlan(
                items = planned,
                actions = actions,
                skippedDuplicateActions = skippedDuplicateActions,
            ),
        )
    }

    /**
     * Applica il piano confermato: crea categorie/prodotti mancanti e salva le
     * voci nella lista attiva.
     */
    suspend fun execute(plan: ImportPlan): ImportExecutionResult = withContext(Dispatchers.IO) {
        var createdCategories = 0
        var createdProducts = 0
        var updatedExisting = 0
        var importedActions = 0
        var skippedDuplicateActions = plan.skippedDuplicateActions

        val level3Ids = mutableMapOf<Pair<Long, String>, Long>()
        val products = mutableMapOf<Pair<Long, String>, CanonicalProduct>()
        val seenActionKeys = mutableSetOf<Triple<String, ActionScope, ActionUrgency>>()

        try {
            shoppingListRepository.withImportTransaction {
                for (item in plan.items) {
                    val level3Id = resolveLevel3Id(
                        item = item,
                        cache = level3Ids,
                        onCreated = { createdCategories++ },
                    )

                    val product = item.productId
                        ?.let { catalogRepository.getById(it) }
                        ?.takeIf { it.level3Id == level3Id }
                        ?: resolveProduct(
                            item = item,
                            level3Id = level3Id,
                            cache = products,
                            onCreated = { createdProducts++ },
                        )

                    val existingItem = shoppingListRepository.getByProductId(product.id)
                    if (existingItem != null) {
                        shoppingListRepository.update(
                            existingItem.copy(
                                quantity = item.quantity,
                                unit = item.unit,
                                brand = item.brand ?: existingItem.brand,
                                variant = item.variant ?: existingItem.variant,
                                updatedAt = System.currentTimeMillis(),
                            )
                        )
                        updatedExisting++
                    } else {
                        val now = System.currentTimeMillis()
                        shoppingListRepository.insert(
                            ShoppingItem(
                                id = UUID.randomUUID().toString(),
                                productId = product.id,
                                quantity = item.quantity,
                                unit = item.unit,
                                brand = item.brand,
                                variant = item.variant,
                                isChecked = false,
                                createdAt = now,
                                updatedAt = now,
                            )
                        )
                    }
                }
                for (action in plan.actions) {
                    val key = Triple(action.normalizedText, action.scope, action.urgency)
                    if (!seenActionKeys.add(key)) {
                        skippedDuplicateActions++
                    } else if (shoppingListRepository.checkDuplicateAction(
                            normalizedText = action.normalizedText,
                            scope = action.scope,
                            urgency = action.urgency,
                        )
                    ) {
                        skippedDuplicateActions++
                    } else {
                        shoppingListRepository.addAction(action)
                        importedActions++
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext ImportExecutionResult.Error(
                "Importazione non riuscita: ${e.message ?: "errore sconosciuto"}."
            )
        }

        ImportExecutionResult.Success(
            imported = plan.productCount,
            importedActions = importedActions,
            updatedExisting = updatedExisting,
            createdProducts = createdProducts,
            createdCategories = createdCategories,
            skippedDuplicateActions = skippedDuplicateActions,
        )
    }

    private suspend fun resolveLevel3Id(
        item: PlannedImportItem,
        cache: MutableMap<Pair<Long, String>, Long>,
        onCreated: () -> Unit,
    ): Long {
        item.level3Id?.let { return it }

        val key = item.level2Id to ProductNameNormalizer.normalize(item.level3Name)
        cache[key]?.let { return it }

        // Ricontrolla prima di creare: tra preview e conferma può essere stata
        // aggiunta la stessa categoria da un'altra schermata.
        val existing = catalogRepository.getLevel3ByLevel2(item.level2Id)
            .firstOrNull { sameName(it.name, item.level3Name) }
        val id = if (existing != null) {
            existing.id
        } else {
            onCreated()
            catalogRepository.insertLevel3(item.level3Name, item.level2Id).id
        }
        cache[key] = id
        return id
    }

    private suspend fun resolveProduct(
        item: PlannedImportItem,
        level3Id: Long,
        cache: MutableMap<Pair<Long, String>, CanonicalProduct>,
        onCreated: () -> Unit,
    ): CanonicalProduct {
        val key = level3Id to ProductNameNormalizer.normalize(item.productName)
        cache[key]?.let { return it }

        val existing = catalogRepository.getByLevel3(level3Id)
            .firstOrNull { sameName(it.name, item.productName) }
        val product = if (existing != null) {
            existing
        } else {
            onCreated()
            catalogRepository.insertProduct(item.productName, level3Id)
        }
        cache[key] = product
        return product
    }

    private suspend fun knownBrands(): List<String> {
        val used = readListOrEmpty {
            shoppingListRepository.getAttributeSuggestions().first().brands
        }
        val userDefined = readListOrEmpty {
            userAttributeRepository.observe(UserAttributeType.BRAND).first().map { it.value }
        }
        return (ProductAttribute.defaultBrands + used + userDefined)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { ProductNameNormalizer.normalize(it) }
            .sortedByDescending { normalizedWordCount(it) }
    }

    private suspend fun knownVariants(): List<String> {
        val used = readListOrEmpty {
            shoppingListRepository.getAttributeSuggestions().first().variants
        }
        val userDefined = readListOrEmpty {
            userAttributeRepository.observe(UserAttributeType.VARIANT).first().map { it.value }
        }
        return (used + userDefined)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { ProductNameNormalizer.normalize(it) }
            .sortedByDescending { normalizedWordCount(it) }
    }

    /**
     * I suggerimenti sono un'euristica: se la lettura fallisce l'import può
     * comunque procedere con i valori predefiniti. La cancellazione, invece,
     * non va mai inghiottita.
     */
    private suspend fun readListOrEmpty(block: suspend () -> List<String>): List<String> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    private fun resolveItem(
        item: ParsedItem,
        products: List<CanonicalProduct>,
        knownBrands: List<String>,
        knownVariants: List<String>,
    ): ResolvedLine {
        val words = splitWords(item.description)
        if (words.isEmpty()) {
            return ResolvedLine(null, item.description, null, null)
        }
        val hasBio = words.any(::isBioTerm)
        val searchableWords = words.filterNot(::isBioTerm)
        val effectiveWords = searchableWords.ifEmpty { words }

        val productMatch = products
            .mapNotNull { product ->
                val nameWords = splitWords(product.name)
                if (nameWords.isEmpty() || nameWords.size > effectiveWords.size) return@mapNotNull null
                if (startsWith(effectiveWords, nameWords)) product to nameWords.size else null
            }
            .maxByOrNull { it.second }

        if (productMatch != null) {
            val (product, matchedWords) = productMatch
            val remainder = effectiveWords.drop(matchedWords)
            val attributes = splitAttributes(remainder, knownBrands)
            return ResolvedLine(
                product = product,
                productName = product.name,
                brand = attributes.brand,
                variant = listOfNotNull(attributes.variant, "Bio".takeIf { hasBio })
                    .joinToString(" ").ifBlank { null },
            )
        }

        val unknown = splitUnknownProduct(effectiveWords, knownBrands, knownVariants)
        return if (hasBio) unknown.copy(
            variant = listOfNotNull(unknown.variant, "Bio").joinToString(" "),
        ) else unknown
    }

    private fun containsBioTerm(value: String): Boolean = splitWords(value).any(::isBioTerm)

    private fun isBioTerm(word: String): Boolean =
        ProductNameNormalizer.normalize(word) in
            setOf("bio", "biologico", "biologica", "biologici", "biologiche")

    /**
     * Risolve una riga proveniente da un percorso di categoria legacy.
     * Cerca il prodotto per nome/sinonimo nell'intero catalogo e usa il percorso
     * attuale del prodotto, evitando di ricrearlo sotto la vecchia categoria.
     */
    private suspend fun resolveLegacyCategoryItem(
        item: ParsedItem,
        knownBrands: List<String>,
        knownVariants: List<String>,
    ): PlannedImportItem? {
        val expressionWords = splitWords(item.description)
        val hasBio = expressionWords.any(::isBioTerm)
        val baseDescription = expressionWords.filterNot(::isBioTerm).joinToString(" ")
        val searchDescription = baseDescription.ifBlank { item.description }

        val exact = catalogRepository.getByExactSynonym(item.description)
            ?: catalogRepository.getByExactSynonym(searchDescription)
        val candidates = exact?.let(::listOf)
            ?: catalogRepository.search(searchDescription).first()
        if (candidates.isEmpty()) return null

        val resolved = if (exact != null && !startsWith(splitWords(searchDescription), splitWords(exact.name))) {
            ResolvedLine(exact, exact.name, null, null)
        } else {
            resolveItem(
                item = item.copy(description = searchDescription),
                products = candidates,
                knownBrands = knownBrands,
                knownVariants = knownVariants,
            )
        }
        val product = resolved.product ?: candidates.firstOrNull { candidate ->
            startsWith(splitWords(searchDescription), splitWords(candidate.name))
        } ?: return null
        val level1Name = product.level1Name ?: return null
        val level2Name = product.level2Name ?: return null
        val level2Id = product.level2Id ?: return null
        val level3Name = product.level3Name ?: return null
        val variant = listOfNotNull(
            resolved.variant,
            "Bio".takeIf { hasBio },
        ).joinToString(" ").ifBlank { null }

        return PlannedImportItem(
            level2Id = level2Id,
            level2Name = level2Name,
            level3Id = product.level3Id,
            level3Name = level3Name,
            level1Name = level1Name,
            productId = product.id,
            productName = product.name,
            quantity = item.quantity,
            unit = item.unit ?: StandardUnit.PIECE,
            brand = resolved.brand,
            variant = variant,
            lineNumber = item.lineNumber,
        )
    }

    /**
     * Divide ciò che segue il nome prodotto in marca e variante.
     *
     * L'export scrive `prodotto marca variante quantità`: se la marca è nota,
     * viene riconosciuta anche se non è la prima parola. Tutto il resto diventa
     * variante, così il testo resta fedele al round-trip.
     */
    private fun splitAttributes(
        remainder: List<String>,
        knownBrands: List<String>,
    ): LineAttributes {
        if (remainder.isEmpty()) return LineAttributes(null, null)

        val brandMatch = findHint(remainder, knownBrands)
        if (brandMatch != null) {
            val withoutBrand = remainder.toMutableList().apply {
                repeat(brandMatch.wordCount) { removeAt(brandMatch.index) }
            }
            return LineAttributes(
                brand = brandMatch.value,
                variant = normalizeFreeText(withoutBrand.joinToString(" ")),
            )
        }

        return LineAttributes(
            brand = null,
            variant = normalizeFreeText(remainder.joinToString(" ")),
        )
    }

    /**
     * Per un prodotto non presente nel catalogo prova a usare marca/variante
     * note per isolare il nome. Es. "Spaghetti Barilla integrale" diventa
     * prodotto "Spaghetti", marca "Barilla", variante "Integrale".
     */
    private fun splitUnknownProduct(
        words: List<String>,
        knownBrands: List<String>,
        knownVariants: List<String>,
    ): ResolvedLine {
        val brandMatch = findHint(words, knownBrands, minIndex = 1)
        val variantMatch = findHint(words, knownVariants, minIndex = 1)

        // Il primo marcatore noto delimita il nome del prodotto; tutto ciò che
        // segue viene poi diviso in marca e variante. Se la variante precede la
        // marca (es. "Penne integrale Barilla") il nome resta "Penne".
        val firstMarker = listOfNotNull(brandMatch, variantMatch)
            .minByOrNull { it.index }

        if (firstMarker != null) {
            val productName = normalizeFreeText(
                words.take(firstMarker.index).joinToString(" ")
            )
            if (productName != null) {
                val attributes = splitAttributes(
                    remainder = words.drop(firstMarker.index),
                    knownBrands = knownBrands,
                )
                return ResolvedLine(
                    product = null,
                    productName = productName,
                    brand = attributes.brand,
                    variant = attributes.variant,
                )
            }
        }

        return ResolvedLine(
            product = null,
            productName = normalizeFreeText(words.joinToString(" ")) ?: words.first(),
            brand = null,
            variant = null,
        )
    }

    /**
     * Cerca una sequenza nota (marca o variante) nelle parole, rispettando i
     * confini di parola. La variante più lunga vince.
     */
    private fun findHint(
        words: List<String>,
        hints: List<String>,
        minIndex: Int = 0,
    ): HintMatch? {
        for (hint in hints) {
            val hintWords = splitWords(hint)
            if (hintWords.isEmpty() || hintWords.size > words.size) continue

            for (index in minIndex..words.size - hintWords.size) {
                if (words.subList(index, index + hintWords.size)
                        .map { ProductNameNormalizer.normalize(it) } ==
                    hintWords.map { ProductNameNormalizer.normalize(it) }
                ) {
                    return HintMatch(hint, index, hintWords.size)
                }
            }
        }
        return null
    }

    private fun startsWith(words: List<String>, prefix: List<String>): Boolean =
        words.size >= prefix.size &&
            words.subList(0, prefix.size)
                .map { ProductNameNormalizer.normalize(it) } ==
            prefix.map { ProductNameNormalizer.normalize(it) }

    private fun sameName(left: String, right: String): Boolean =
        ProductNameNormalizer.normalize(left) == ProductNameNormalizer.normalize(right)

    private fun splitWords(value: String): List<String> =
        value.trim().split(whitespace).filter { it.isNotBlank() }

    private fun normalizedWordCount(value: String): Int =
        splitWords(value).size

    private fun normalizeFreeText(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return ProductNameNormalizer.capitalizeFirstWord(trimmed)
    }

    private data class ResolvedLine(
        val product: CanonicalProduct?,
        val productName: String,
        val brand: String?,
        val variant: String?,
    )

    private data class LineAttributes(
        val brand: String?,
        val variant: String?,
    )

    private data class HintMatch(
        val value: String,
        val index: Int,
        val wordCount: Int,
    )
}
