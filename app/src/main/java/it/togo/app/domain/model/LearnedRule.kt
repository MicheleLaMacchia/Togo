package it.togo.app.domain.model

import java.util.UUID

data class LearnedRule(
    val id: String = UUID.randomUUID().toString(),
    val userExpression: String,
    val productId: Long,
    val level3Id: Long,
    val lastAppliedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun deactivate(): LearnedRule = copy(isActive = false)
    fun updateApplicationTime(): LearnedRule = copy(lastAppliedAt = System.currentTimeMillis())

    fun toEntity(): it.togo.app.data.database.entity.LearnedRuleEntity = it.togo.app.data.database.entity.LearnedRuleEntity(
        id = id,
        userExpression = userExpression,
        productId = productId,
        level3Id = level3Id,
        lastAppliedAt = lastAppliedAt,
        isActive = isActive
    )
}