package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Categoria Task a due livelli.
 *
 * Il seed stabile degli ID è nella migrazione `5→6`; la tabella viene
 * registrata in `TogoDatabase` nella Story 8.4.
 */
@Entity(
    tableName = "TASK_CATEGORY",
    foreignKeys = [
        ForeignKey(
            entity = TaskCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["parentId"], name = "idx_task_category_parent"),
        Index(value = ["sortOrder"], name = "idx_task_category_sort"),
    ],
)
data class TaskCategoryEntity(
    @PrimaryKey
    val id: Long,
    val parentId: Long? = null,
    val name: String,
    val level: Int,
    val sortOrder: Int,
)
