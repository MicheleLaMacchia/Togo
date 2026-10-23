package it.togo.app.domain.product

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.StandardUnit
import java.text.Normalizer
import java.util.Locale

/**
 * Risolve le unità di misura compatibili con un prodotto canonico.
 *
 * La tassonomia del catalogo è il segnale principale: il Livello 2 identifica
 * la famiglia merceologica (es. "Pasta e riso" -> kg/g), mentre alcuni
 * Livelli 3 hanno esigenze diverse dal resto della famiglia (es. "Yogurt"
 * dentro "Latte e yogurt" -> g, "Oli" dentro "Oli e condimenti" -> L).
 *
 * Il nome del prodotto è usato solo come fallback per i prodotti creati
 * dall'utente in categorie non ancora riconosciute o per i repository fake
 * dei test. In assenza di informazioni la scelta resta [StandardUnit.PIECE],
 * così non si perde il comportamento precedente.
 *
 * L'ordine della lista è significativo: il primo elemento è l'unità
 * proposta di default quando si seleziona il prodotto.
 */
object ProductUnitResolver {

    private val weightBulk = listOf(
        StandardUnit.KILOGRAM,
        StandardUnit.GRAM,
        StandardUnit.PACKAGE,
    )

    private val weightSmall = listOf(
        StandardUnit.GRAM,
        StandardUnit.KILOGRAM,
        StandardUnit.PACKAGE,
    )

    private val volume = listOf(
        StandardUnit.LITER,
        StandardUnit.MILLILITER,
        StandardUnit.BOTTLE,
        StandardUnit.PACKAGE,
    )

    private val volumeBottle = listOf(
        StandardUnit.BOTTLE,
        StandardUnit.LITER,
        StandardUnit.MILLILITER,
    )

    private val volumeCan = listOf(
        StandardUnit.BOTTLE,
        StandardUnit.CAN,
        StandardUnit.LITER,
        StandardUnit.MILLILITER,
        StandardUnit.PACKAGE,
    )

    private val pieces = listOf(
        StandardUnit.PIECE,
        StandardUnit.PACKAGE,
    )

    private val jars = listOf(
        StandardUnit.JAR,
        StandardUnit.GRAM,
        StandardUnit.KILOGRAM,
        StandardUnit.PACKAGE,
    )

    private val cans = listOf(
        StandardUnit.CAN,
        StandardUnit.GRAM,
        StandardUnit.KILOGRAM,
        StandardUnit.JAR,
        StandardUnit.PACKAGE,
    )

    private val boxes = listOf(
        StandardUnit.PIECE,
        StandardUnit.PACKAGE,
        StandardUnit.BOX,
    )

    private val rolls = listOf(
        StandardUnit.ROLL,
        StandardUnit.PACKAGE,
    )

    /**
     * Unità compatibili per [product], con l'unità consigliata in prima posizione.
     * La lista non è mai vuota.
     */
    fun forProduct(product: CanonicalProduct): List<StandardUnit> = resolve(
        level1Name = product.level1Name,
        level2Name = product.level2Name,
        level3Name = product.level3Name,
        productName = product.name,
    )

    internal fun resolve(
        level1Name: String?,
        level2Name: String?,
        level3Name: String?,
        productName: String,
    ): List<StandardUnit> {
        val level1 = normalize(level1Name)
        val level2 = normalize(level2Name)
        val level3 = normalize(level3Name)
        val name = normalize(productName)

        // 1. Eccezioni puntuali dentro una famiglia merceologica.
        when (level3) {
            "uova", "uova fresche" -> return pieces
            "latte fresco", "alternative vegetali" -> return volume
            "yogurt", "yogurt speciali" -> return weightSmall
            "frutta secca", "verdura confezionata" -> return weightSmall
            "oli", "aceti" -> return volumeBottle
            "sale e spezie" -> return weightSmall
            "pesce in scatola" -> return cans
            "pesce affumicato" -> return weightSmall
            "sott'oli e sottaceti" -> return jars
            "cucina monouso" -> return listOf(
                StandardUnit.PIECE,
                StandardUnit.PACKAGE,
                StandardUnit.ROLL,
                StandardUnit.BOX,
            )
            "illuminazione e batterie", "piccola ferramenta" -> return boxes
            "igiene dentale" -> return listOf(
                StandardUnit.PIECE,
                StandardUnit.GRAM,
                StandardUnit.PACKAGE,
            )
            "cura del corpo" -> return listOf(
                StandardUnit.MILLILITER,
                StandardUnit.LITER,
                StandardUnit.GRAM,
                StandardUnit.PIECE,
                StandardUnit.PACKAGE,
            )
            "cura dei capelli" -> return listOf(
                StandardUnit.MILLILITER,
                StandardUnit.LITER,
                StandardUnit.PIECE,
                StandardUnit.PACKAGE,
            )
            "rasatura e depilazione" -> return listOf(
                StandardUnit.PIECE,
                StandardUnit.GRAM,
                StandardUnit.PACKAGE,
            )
            "pannolini e salviette" -> return pieces
            "cura bambini" -> return listOf(
                StandardUnit.MILLILITER,
                StandardUnit.LITER,
                StandardUnit.GRAM,
                StandardUnit.PIECE,
                StandardUnit.PACKAGE,
            )
            "alimentazione animali" -> return weightSmall
            "cura e giochi animali" -> return pieces
        }

        // 2. Famiglia merceologica (Livello 2).
        when (level2) {
            "frutta", "verdura", "erbe e contorni", "pane",
            "carne bovina", "carne suina", "pollame", "salumi freschi",
            "pesce fresco", "crostacei e molluschi", "pasta e riso" -> return weightBulk

            "panificati", "dolci da forno" -> return pieces

            "pesce confezionato", "formaggi", "burro e panna", "surgelati",
            "snack e cracker", "dolci e cioccolato", "caffe e te",
            "legumi e farine" -> return weightSmall

            "acqua e minerali", "succhi e nettari" -> return volume
            "bibite" -> return volume
            "birra" -> return volumeCan
            "vino" -> return volumeBottle
            "latte e yogurt" -> return volume

            "oli e condimenti" -> return weightSmall
            "conserve" -> return cans
            "salse e pesti" -> return jars

            "igiene persona" -> return listOf(
                StandardUnit.PIECE,
                StandardUnit.MILLILITER,
                StandardUnit.LITER,
                StandardUnit.GRAM,
                StandardUnit.PACKAGE,
            )
            "pulizia e lavatrice" -> return listOf(
                StandardUnit.LITER,
                StandardUnit.MILLILITER,
                StandardUnit.GRAM,
                StandardUnit.KILOGRAM,
                StandardUnit.PIECE,
                StandardUnit.PACKAGE,
            )
            "cura della casa" -> return boxes + StandardUnit.ROLL
            "cartoleria" -> return boxes
            "articoli per animali" -> return weightSmall
            "bambini e neonati" -> return pieces + listOf(
                StandardUnit.MILLILITER,
                StandardUnit.LITER,
                StandardUnit.GRAM,
            )

            // Famiglie usate dai test o da tassonomie locali.
            "bevande" -> return volume
            "cereali" -> return weightSmall
        }

        // 3. Ripiego sul Livello 1, utile quando il Livello 2 è sconosciuto.
        when (level1) {
            "ortofrutta", "panetteria", "carne", "pesce" -> return weightBulk
            "bevande" -> return volume
            "banco frigo", "dispensa" -> return weightSmall
            "non alimentare" -> return pieces
        }

        // 4. Fallback sul nome, per prodotti user-defined in categorie non note.
        return when {
            name.contains("bottiglia") -> volumeBottle
            name.contains("barattolo") || name.contains("vasetto") -> jars
            name.contains("lattina") -> cans
            name.contains("scatola") || name.contains("confezione") -> boxes
            name.contains("rotolo") || name.contains("carta igienica") -> rolls

            containsAny(name, weightKeywords) -> weightSmall
            containsAny(name, volumeKeywords) -> volume
            containsAny(name, pieceKeywords) -> pieces
            else -> listOf(StandardUnit.PIECE)
        }
    }

    private val weightKeywords = listOf(
        "pane", "pasta", "spaghetti", "penne", "fusilli", "rigatoni", "farfalle",
        "orecchiette", "tagliatelle", "lasagne", "gnocchi", "riso", "farina",
        "carne", "manzo", "maiale", "pollo", "tacchino", "pesce", "salmone",
        "tonno", "sgombro", "sardine", "formaggio", "mozzarella", "parmigiano",
        "grana", "pecorino", "ricotta", "burro", "panna", "mela", "pera",
        "banana", "arancia", "limone", "kiwi", "fragola", "pomodoro", "patata",
        "cipolla", "zucca", "zucchero", "sale", "caffe", "biscotti",
        "cioccolato", "mandorle", "noci", "nocciole", "legumi", "fagioli",
        "ceci", "lenticchie",
    )

    private val volumeKeywords = listOf(
        "latte", "acqua", "succo", "spremuta", "nettare", "olio", "aceto",
        "vino", "birra", "bibita", "cola", "aranciata", "limonata", "te freddo",
        "detersivo", "ammorbidente", "candeggina", "sgrassatore", "shampoo",
        "bagnoschiuma", "gel doccia", "collutorio", "crema solare", "lozione",
        "sapone liquido",
    )

    private val pieceKeywords = listOf(
        "uova", "uovo", "pannolino", "salviette", "spazzolino", "batterie",
        "lampadine", "candele", "guanti", "sacchi", "fazzoletti", "tovaglioli",
        "piatti", "bicchieri", "posate",
    )

    private fun containsAny(value: String, keywords: List<String>): Boolean =
        keywords.any { value.contains(it) }

    private fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(
            raw.trim().lowercase(Locale.ROOT),
            Normalizer.Form.NFKD,
        )
        return buildString(decomposed.length) {
            for (ch in decomposed) {
                val type = Character.getType(ch)
                if (type != Character.NON_SPACING_MARK.toInt() &&
                    type != Character.COMBINING_SPACING_MARK.toInt() &&
                    type != Character.ENCLOSING_MARK.toInt()
                ) {
                    append(ch)
                }
            }
        }
    }
}
