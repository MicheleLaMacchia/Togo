package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "TAXONOMY_LEVEL_1",
    indices = [Index(value = ["sortOrder"], name = "idx_taxonomy_l1_sort")]
)
data class TaxonomyLevel1Entity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val sortOrder: Int
) {
    fun toDomain(): it.togo.app.domain.model.TaxonomyLevel.Level1 = it.togo.app.domain.model.TaxonomyLevel.Level1(
        id = id,
        name = name,
        sortOrder = sortOrder
    )
}

@Entity(
    tableName = "TAXONOMY_LEVEL_2",
    indices = [
        Index(value = ["level1_id"], name = "idx_taxonomy_l2_l1"),
        Index(value = ["sortOrder"], name = "idx_taxonomy_l2_sort")
    ]
)
data class TaxonomyLevel2Entity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "level1_id")
    val level1Id: Long,
    val name: String,
    val sortOrder: Int
) {
    fun toDomain(): it.togo.app.domain.model.TaxonomyLevel.Level2 = it.togo.app.domain.model.TaxonomyLevel.Level2(
        id = id,
        name = name,
        sortOrder = sortOrder,
        level1Id = level1Id
    )
}

@Entity(
    tableName = "TAXONOMY_LEVEL_3",
    indices = [
        Index(value = ["level2_id"], name = "idx_taxonomy_l3_l2"),
        Index(value = ["sortOrder"], name = "idx_taxonomy_l3_sort"),
        Index(value = ["is_user_defined"], name = "idx_taxonomy_l3_user_defined")
    ]
)
data class TaxonomyLevel3Entity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "level2_id")
    val level2Id: Long,
    val name: String,
    val sortOrder: Int,
    @ColumnInfo(name = "is_user_defined")
    val isUserDefined: Boolean = false
) {
    fun toDomain(): it.togo.app.domain.model.TaxonomyLevel.Level3 = it.togo.app.domain.model.TaxonomyLevel.Level3(
        id = id,
        name = name,
        sortOrder = sortOrder,
        level2Id = level2Id,
        isUserDefined = isUserDefined
    )
}

@Entity(
    tableName = "CANONICAL_PRODUCT",
    indices = [
        Index(value = ["level3_id"], name = "idx_canonical_product_l3"),
        Index(value = ["name"], name = "idx_canonical_product_name"),
        Index(value = ["is_user_defined"], name = "idx_canonical_product_user_defined")
    ]
)
data class CanonicalProductEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "level3_id")
    val level3Id: Long,
    val name: String,
    @ColumnInfo(name = "is_user_defined")
    val isUserDefined: Boolean = false
) {
    fun toDomain(): it.togo.app.domain.model.CanonicalProduct = it.togo.app.domain.model.CanonicalProduct(
        id = id,
        name = name,
        level3Id = level3Id,
        isUserDefined = isUserDefined
    )
}

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