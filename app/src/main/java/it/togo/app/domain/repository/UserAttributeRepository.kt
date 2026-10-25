package it.togo.app.domain.repository

import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.RenameUserAttributeResult
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Registro delle marche/varianti aggiunte dall'utente.
 *
 * I valori predefiniti non stanno qui: restano nel codice
 * ([it.togo.app.domain.model.ProductAttribute]). Questo repository serve sia ai
 * suggerimenti del form "Aggiungi voce" sia alla gestione nelle Impostazioni.
 */
interface UserAttributeRepository {

    fun observe(type: UserAttributeType): Flow<List<UserAttribute>>

    /**
     * Suggerimenti pronti per il form: solo i valori user-defined.
     * La UI aggiunge i predefiniti.
     */
    fun observeSuggestions(): Flow<AttributeSuggestions> = flowOf(AttributeSuggestions())

    /**
     * Ricorda un valore digitato dall'utente. I valori vuoti e le marche
     * predefinite vengono ignorati; i duplicati case-insensitive non vengono
     * reinseriti.
     */
    suspend fun remember(type: UserAttributeType, rawValue: String)

    /** Rinomina un valore, rifiutando i duplicati dello stesso tipo. */
    suspend fun rename(
        id: Long,
        type: UserAttributeType,
        rawValue: String,
    ): RenameUserAttributeResult

    suspend fun delete(id: Long): Boolean
}
