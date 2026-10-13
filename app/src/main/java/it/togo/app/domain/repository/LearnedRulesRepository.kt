package it.togo.app.domain.repository

import it.togo.app.domain.model.LearnedRule
import kotlinx.coroutines.flow.Flow

interface LearnedRulesRepository {
    fun getAll(): Flow<List<LearnedRule>>
    suspend fun insert(rule: LearnedRule)
    suspend fun update(rule: LearnedRule)
    suspend fun delete(ruleId: String)
    suspend fun getByExpression(expression: String): LearnedRule?
    suspend fun getById(ruleId: String): LearnedRule?
    suspend fun getByExpressionAndProductId(expression: String, productId: Long): LearnedRule?
    suspend fun upsertAtomic(rule: LearnedRule): LearnedRule
}