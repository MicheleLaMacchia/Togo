package it.togo.app.domain.repository

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
