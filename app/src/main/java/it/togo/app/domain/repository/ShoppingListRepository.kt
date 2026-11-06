package it.togo.app.domain.repository

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.ItemAttributes
import it.togo.app.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ShoppingListRepository {
    /** Tutte le voci in un'unica emissione, per evitare stati transitori tra query. */
    fun getAllItems(): Flow<List<ShoppingItem>>
    fun getActiveItems(): Flow<List<ShoppingItem>>
    fun getCheckedItems(): Flow<List<ShoppingItem>>
    suspend fun insert(item: ShoppingItem)
    suspend fun update(item: ShoppingItem)
    suspend fun delete(itemId: String)
    suspend fun getByProductId(productId: Long): ShoppingItem?
    suspend fun checkout(checkedItemIds: List<String>): Boolean

    // --- Azioni "Cose da fare" (Epic 8) ---

    /**
     * Tutte le azioni, attive e spuntate, come unico flusso reattivo.
     * Il raggruppamento e l'ordinamento sono nel domain/presentazione.
     */
    fun getActions(): Flow<List<ActionItem>> = flowOf(emptyList())

    /** Inserisce una nuova azione; la scrittura è atomica e su thread IO. */
    suspend fun addAction(action: ActionItem) = Unit

    /** Aggiorna testo, ambito e urgenza di un'azione esistente. */
    suspend fun updateAction(action: ActionItem) = Unit

    /** Elimina una singola azione. */
    suspend fun deleteAction(actionId: String) = Unit

    /** Spunta o deseleziona un'azione senza eliminarla. */
    suspend fun checkOffAction(actionId: String, checked: Boolean) = Unit

    /**
     * Deduplica azioni: testo normalizzato + ambito + urgenza.
     *
     * [normalizedText] arriva dal domain ([it.togo.app.domain.action.ActionTextNormalizer]):
     * il data layer non applica normalizzazioni. Le azioni spuntate contano
     * come presenti. [excludeActionId] esclude l'azione in modifica.
     *
     * Il default false mantiene compatibili i repository fake dei test.
     */
    suspend fun checkDuplicateAction(
        normalizedText: String,
        scope: ActionScope,
        urgency: ActionUrgency,
        excludeActionId: String? = null,
    ): Boolean = false

    /**
     * Marche e varianti già usate dall'utente (lista attiva + storico).
     * Il default vuoto mantiene compatibili i repository fake dei test.
     */
    fun getAttributeSuggestions(): Flow<AttributeSuggestions> = flowOf(AttributeSuggestions())

    /**
     * Ultimi attributi usati per [productId] (voce attiva più recente,
     * altrimenti acquisto più recente nello storico): servono a precompilare
     * marca/variante alla riapertura.
     * Il default null mantiene compatibili i repository fake dei test.
     */
    suspend fun getLastAttributes(productId: Long): ItemAttributes? = null
}
