package it.togo.app.data.repository

import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.entity.LearnedRuleEntity
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.text.normalizeExpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomLearnedRulesRepository(
    private val learnedRulesDao: LearnedRulesDao
) : LearnedRulesRepository {

    override fun getAll(): Flow<List<LearnedRule>> =
        learnedRulesDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insert(rule: LearnedRule) {
        withContext(Dispatchers.IO) {
            learnedRulesDao.insert(rule.normalized().toEntity())
        }
    }

    override suspend fun update(rule: LearnedRule) {
        withContext(Dispatchers.IO) {
            learnedRulesDao.update(rule.normalized().toEntity())
        }
    }

    override suspend fun delete(ruleId: String) {
        withContext(Dispatchers.IO) {
            learnedRulesDao.deleteById(ruleId)
        }
    }

    override suspend fun getByExpression(expression: String): LearnedRule? =
        withContext(Dispatchers.IO) {
            learnedRulesDao.getByExpression(normalizeExpression(expression))?.toDomain()
        }

    override suspend fun getById(ruleId: String): LearnedRule? =
        withContext(Dispatchers.IO) {
            learnedRulesDao.getById(ruleId)?.toDomain()
        }

    override suspend fun getByExpressionAndProductId(expression: String, productId: Long): LearnedRule? =
        withContext(Dispatchers.IO) {
            learnedRulesDao.getByExpressionAndProductId(
                normalizeExpression(expression),
                productId,
            )?.toDomain()
        }

    override suspend fun upsertAtomic(rule: LearnedRule): LearnedRule =
        withContext(Dispatchers.IO) {
            val normalizedRule = rule.normalized()
            val entity = normalizedRule.toEntity()
            val existing = learnedRulesDao.getByExpressionAndProductId(entity.userExpression, entity.productId)
            if (existing != null) {
                val updated = entity.copy(id = existing.id)
                learnedRulesDao.update(updated)
                updated.toDomain()
            } else {
                learnedRulesDao.insert(entity)
                normalizedRule
            }
        }
}

private fun LearnedRule.normalized(): LearnedRule {
    val normalizedExpression = normalizeExpression(userExpression)
    return if (normalizedExpression == userExpression) {
        this
    } else {
        copy(userExpression = normalizedExpression)
    }
}