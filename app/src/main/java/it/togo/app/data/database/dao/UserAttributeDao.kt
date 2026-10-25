package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.togo.app.data.database.entity.UserAttributeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAttributeDao {

    /** Valori di un tipo, in ordine alfabetico case-insensitive. */
    @Query("SELECT * FROM USER_ATTRIBUTE WHERE type = :type ORDER BY value COLLATE NOCASE")
    fun getByType(type: String): Flow<List<UserAttributeEntity>>

    /**
     * Cerca un duplicato dello stesso tipo ignorando [excludeId] (la riga che
     * si sta rinominando). Il confronto usa la forma normalizzata, quindi
     * "Coop" e "coop" sono lo stesso valore.
     */
    @Query(
        "SELECT * FROM USER_ATTRIBUTE " +
            "WHERE type = :type AND normalized = :normalized AND id != :excludeId LIMIT 1"
    )
    suspend fun findDuplicate(
        type: String,
        normalized: String,
        excludeId: Long,
    ): UserAttributeEntity?

    /** Inserisce senza duplicare: se il valore esiste già non fa nulla. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: UserAttributeEntity): Long

    @Query("UPDATE USER_ATTRIBUTE SET value = :value, normalized = :normalized WHERE id = :id")
    suspend fun updateValue(id: Long, value: String, normalized: String): Int

    @Query("DELETE FROM USER_ATTRIBUTE WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
