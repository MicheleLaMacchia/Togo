package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "LEARNED_RULE",
    indices = [
        Index(value = ["userExpression"], name = "idx_learned_rule_expression", unique = true),
        Index(value = ["productId"], name = "idx_learned_rule_product_id"),
        Index(value = ["isActive"], name = "idx_learned_rule_active")
    ]
)
data class LearnedRuleEntity(
    @PrimaryKey
    val id: String,
    val userExpression: String,
    val productId: Long,
    val level3Id: Long,
    val lastAppliedAt: Long,
    val isActive: Boolean
) {
    fun toDomain(): it.togo.app.domain.model.LearnedRule = it.togo.app.domain.model.LearnedRule(
        id = id,
        userExpression = userExpression,
        productId = productId,
        level3Id = level3Id,
        lastAppliedAt = lastAppliedAt,
        isActive = isActive
    )
}