package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import it.togo.app.domain.product.ProductUnitResolver

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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "level3_id")
    val level3Id: Long,
    val name: String,
    @ColumnInfo(name = "is_user_defined")
    val isUserDefined: Boolean = false
) {
    fun toDomain(): it.togo.app.domain.model.CanonicalProduct {
        val product = it.togo.app.domain.model.CanonicalProduct(
            id = id,
            name = name,
            level3Id = level3Id,
            isUserDefined = isUserDefined
        )
        return product.copy(
            compatibleUnits = ProductUnitResolver.forProduct(product),
        )
    }
}

/**
 * Proiezione di lettura di un prodotto canonico con la sua tassonomia.
 *
 * Room la usa come tipo di ritorno delle query che fanno JOIN sui tre livelli:
 * in questo modo il dominio riceve già `level3Name`/`level2Name`/`level1Name`
 * e può risolvere le unità compatibili senza query aggiuntive.
 *
 * Non è una `@Entity`: serve solo per il mapping del cursor, quindi non viene
 * creata né aggiornata da Room.
 */
data class CanonicalProductRow(
    val id: Long,
    @ColumnInfo(name = "level3_id")
    val level3Id: Long,
    val name: String,
    @ColumnInfo(name = "is_user_defined")
    val isUserDefined: Boolean,
    @ColumnInfo(name = "level3Name")
    val level3Name: String?,
    @ColumnInfo(name = "level2Name")
    val level2Name: String?,
    @ColumnInfo(name = "level2Id")
    val level2Id: Long?,
    @ColumnInfo(name = "level1Name")
    val level1Name: String?,
    @ColumnInfo(name = "level1Id")
    val level1Id: Long?,
) {
    fun toDomain(): it.togo.app.domain.model.CanonicalProduct {
        val product = it.togo.app.domain.model.CanonicalProduct(
            id = id,
            name = name,
            level3Id = level3Id,
            isUserDefined = isUserDefined,
            level3Name = level3Name,
            level2Name = level2Name,
            level2Id = level2Id,
            level1Name = level1Name,
            level1Id = level1Id,
        )
        return product.copy(
            compatibleUnits = ProductUnitResolver.forProduct(product),
        )
    }
}

/**
 * Proiezione di lettura di un Livello 3 con il percorso tassonomico e il numero
 * di prodotti che contiene.
 *
 * Usata dalla ricerca per categorie: le categorie vuote vengono scartate lato
 * repository, così l'utente non vede risultati su cui non può fare nulla.
 */
data class TaxonomyLevel3PathRow(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "level2Name")
    val level2Name: String?,
    @ColumnInfo(name = "level1Name")
    val level1Name: String?,
    @ColumnInfo(name = "productCount")
    val productCount: Int,
) {
    fun toCategoryResult(): it.togo.app.domain.model.CatalogCategoryResult =
        it.togo.app.domain.model.CatalogCategoryResult(
            id = id,
            name = name,
            level1Name = level1Name,
            level2Name = level2Name,
            productCount = productCount,
        )
}

