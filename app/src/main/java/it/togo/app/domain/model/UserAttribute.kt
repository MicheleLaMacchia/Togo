package it.togo.app.domain.model

/** Tipo di attributo libero ricordato per le voci della lista. */
enum class UserAttributeType {
    BRAND,
    VARIANT,
}

/**
 * Marca o variante creata dall'utente.
 *
 * È distinta dai valori predefiniti (es. le marche in
 * [ProductAttribute.defaultBrands]) perché vive nel registro persistente
 * `USER_ATTRIBUTE`.
 */
data class UserAttribute(
    val id: Long,
    val type: UserAttributeType,
    val value: String,
)

/** Esito della rinomina di una marca/variante. */
sealed interface RenameUserAttributeResult {
    data class Renamed(val attribute: UserAttribute) : RenameUserAttributeResult

    /** Esiste già un altro valore con lo stesso nome (case-insensitive). */
    data class Duplicate(val existing: UserAttribute) : RenameUserAttributeResult

    /** Valore vuoto o non valido: nessuna scrittura effettuata. */
    data object Invalid : RenameUserAttributeResult
}
