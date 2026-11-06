package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.togo.app.data.database.entity.ActionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionItemDao {

    /** Lettura reattiva della lista azioni; l'ordinamento per gruppi è nel domain. */
    @Query("SELECT * FROM ACTION_ITEM ORDER BY created_at ASC")
    fun getAll(): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM ACTION_ITEM WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ActionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ActionItemEntity>): List<Long>

    @Update
    suspend fun update(item: ActionItemEntity): Int

    @Delete
    suspend fun delete(item: ActionItemEntity): Int

    @Query("DELETE FROM ACTION_ITEM WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE ACTION_ITEM SET stato_spunta = :checked, updated_at = :updatedAt WHERE id = :id")
    suspend fun setChecked(id: String, checked: Boolean, updatedAt: Long): Int

    /** Elimina solo le azioni spuntate (CAP-4, "Concludi attività"). */
    @Query("DELETE FROM ACTION_ITEM WHERE stato_spunta = 1")
    suspend fun deleteChecked(): Int

    /**
     * Deduplica per testo + ambito + urgenza.
     *
     * [testoNormalizzato] deve arrivare dal domain
     * ([it.togo.app.domain.action.ActionTextNormalizer.normalize]): il data
     * layer non applica normalizzazioni. L'urgenza e l'ambito sono confrontati
     * esattamente perché sono valori di un elenco chiuso.
     *
     * Le azioni spuntate contano come presenti: il filtro su `stato_spunta` è
     * volutamente assente.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM ACTION_ITEM " +
            "WHERE testo_normalizzato = :testoNormalizzato " +
            "AND ambito = :ambito AND urgenza = :urgenza)"
    )
    suspend fun checkDuplicate(
        testoNormalizzato: String,
        ambito: String,
        urgenza: String,
    ): Boolean

    /**
     * Come [checkDuplicate], ma esclude l'azione che si sta modificando
     * (l'azione non è duplicata di sé stessa).
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM ACTION_ITEM " +
            "WHERE testo_normalizzato = :testoNormalizzato " +
            "AND ambito = :ambito AND urgenza = :urgenza AND id != :excludeId)"
    )
    suspend fun checkDuplicateExcluding(
        testoNormalizzato: String,
        ambito: String,
        urgenza: String,
        excludeId: String,
    ): Boolean
}
