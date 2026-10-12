package it.togo.app.domain.model

/**
 * Opzioni di risoluzione quando un prodotto è già presente nella lista.
 */
enum class ResolutionOption {
    /** Somma la nuova quantità a quella esistente (solo se unità compatibili) */
    SUM,

    /** Sostituisce la quantità esistente con la nuova */
    REPLACE,

    /** Crea una nuova voce separata nella lista */
    SEPARATE,

    /** Annulla l'operazione, la lista resta invariata */
    CANCEL
}