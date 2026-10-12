package it.togo.app.domain.model

/**
 * Definisce quali attributi sono pertinenti per un Livello 3 del catalogo.
 *
 * Ogni L3 dichiara quali attributi supporta:
 * - Marca: dropdown con valori comuni + free text
 * - Variante: free text (es. integrale, biologico, senza lattosio)
 * - Conservazione: dropdown valori controllati (fresco, surgelato, sottovuoto, ecc.)
 */
data class ProductAttribute(
    val level3Id: Long,
    val hasBrand: Boolean = false,
    val hasVariant: Boolean = false,
    val hasCondition: Boolean = false,
) {
    /** Lista di valori controllati per la conservazione */
    val conditionOptions: List<String>
        get() = if (hasCondition) listOf(
            "fresco", "surgelato", "sottovuoto", "sottolio",
            "affettato", "macinato", "grattugiato", "pronto"
        ) else emptyList()

    /** Lista di valori comuni per la marca (selezionabili da tendina) */
    val brandSuggestions: List<String>
        get() = if (hasBrand) listOf(
            "Coop", "Conad", "Esselunga", "Carrefour", "Decathlon",
            "Findus", "Barilla", "Ferrero", "Nestlé", "Danone",
            "Parmalat", "Galbani", "Santal", "Levissima", "San Benedetto"
        ) else emptyList()

    /** Verifica se almeno un attributo è supportato */
    val hasAnyAttribute: Boolean
        get() = hasBrand || hasVariant || hasCondition

    companion object {
        /**
         * Factory per creare attributi in base al L3.
         * Regole di pertinenza:
         * - Freschi (latte, carne, pesce, verdura): conservazione + marca
         * - Secchi (pasta, riso, cereali): marca + variante
         * - Surgelati: conservazione + marca
         * - Bevande: marca + variante
         * - Non alimentari: marca + variante
         */
        fun forLevel3(level3Id: Long, level3Name: String): ProductAttribute {
            val lower = level3Name.lowercase()
            return when {
                // Freschi: conservazione + marca
                lower.contains("latte") || lower.contains("yogurt") ||
                lower.contains("formaggi") || lower.contains("burro") ||
                lower.contains("panna") || lower.contains("carne") ||
                lower.contains("pesce") || lower.contains("uova") ||
                lower.contains("verdura") || lower.contains("frutta") ||
                lower.contains("insalate") || lower.contains("gastronomia") ->
                    ProductAttribute(level3Id, hasBrand = true, hasCondition = true)

                // Surgelati: conservazione + marca
                lower.contains("surgelati") || lower.contains("gelati") ->
                    ProductAttribute(level3Id, hasBrand = true, hasCondition = true)

                // Secchi: marca + variante
                lower.contains("pasta") || lower.contains("riso") ||
                lower.contains("cereali") || lower.contains("legumi") ||
                lower.contains("farine") || lower.contains("zucchero") ||
                lower.contains("spezie") || lower.contains("miele") ||
                lower.contains("marmellate") || lower.contains("biscotti") ||
                lower.contains("cioccolato") || lower.contains("merendine") ->
                    ProductAttribute(level3Id, hasBrand = true, hasVariant = true)

                // Bevande: marca + variante
                lower.contains("acqua") || lower.contains("bibite") ||
                lower.contains("succhi") || lower.contains("birra") ||
                lower.contains("vino") || lower.contains("caffè") ||
                lower.contains("tè") || lower.contains("infusi") ->
                    ProductAttribute(level3Id, hasBrand = true, hasVariant = true)

                // Condimenti: marca + variante
                lower.contains("salse") || lower.contains("condimenti") ||
                lower.contains("oli") || lower.contains("aceti") ->
                    ProductAttribute(level3Id, hasBrand = true, hasVariant = true)

                // Non alimentari: marca + variante
                lower.contains("pulizia") || lower.contains("detersivi") ||
                lower.contains("igiene") || lower.contains("cosmetica") ||
                lower.contains("casa") || lower.contains("animali") ||
                lower.contains("cancelleria") || lower.contains("salute") ->
                    ProductAttribute(level3Id, hasBrand = true, hasVariant = true)

                // Default: solo marca
                else -> ProductAttribute(level3Id, hasBrand = true)
            }
        }
    }
}