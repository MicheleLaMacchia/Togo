package it.togo.app.domain.model

/**
 * Valori di marca e variante già usati dall'utente (lista attiva + storico).
 *
 * Servono a popolare i suggerimenti del form "Aggiungi voce": un valore nuovo
 * digitato una volta resta disponibile anche nelle voci successive.
 */
data class AttributeSuggestions(
    val brands: List<String> = emptyList(),
    val variants: List<String> = emptyList(),
)

/**
 * Marca, variante e conservazione di una voce, usati per precompilare il form
 * quando si riaggiunge un prodotto già conosciuto.
 */
data class ItemAttributes(
    val brand: String? = null,
    val variant: String? = null,
    val condition: String? = null,
) {
    /** True se non c'è nessun attributo da proporre. */
    val isEmpty: Boolean
        get() = brand.isNullOrBlank() && variant.isNullOrBlank() && condition.isNullOrBlank()
}
