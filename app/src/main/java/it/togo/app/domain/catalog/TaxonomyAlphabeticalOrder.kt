package it.togo.app.domain.catalog

/** Ordinamento alfabetico stabile e senza distinzione tra maiuscole e accenti. */
object TaxonomyAlphabeticalOrder {
    fun <T> comparator(name: (T) -> String): Comparator<T> =
        compareBy<T> { ProductNameNormalizer.normalize(name(it)) }
            .thenBy(name)
}
