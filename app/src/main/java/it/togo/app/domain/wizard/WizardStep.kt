package it.togo.app.domain.wizard

/**
 * Step del wizard di categorizzazione prodotto.
 *
 * Ordine sequenziale: SELECT_L1 → SELECT_L2 → SELECT_L3 → ENTER_DETAILS → CONFIRM
 */
enum class WizardStep {
    SELECT_L1,       // Scelta Livello 1 (es. "Alimentari", "Non alimentari")
    SELECT_L2,       // Scelta Livello 2 figlio di L1 (es. "Bevande", "Latticini")
    SELECT_L3,       // Scelta Livello 3 figlio di L2 (es. "Latte UHT", "+ Nuovo...")
    ENTER_DETAILS,   // Nome prodotto, unità, attributi
    CONFIRM          // Riepilogo + conferma creazione
}