package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "SYNONYM",
    indices = [
        Index(value = ["product_id"], name = "idx_synonym_product_id"),
        Index(value = ["term"], name = "idx_synonym_term")
    ]
)
data class SynonymEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val term: String
)
