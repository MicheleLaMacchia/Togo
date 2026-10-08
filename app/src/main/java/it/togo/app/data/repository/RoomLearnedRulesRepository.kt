package it.togo.app.data.repository

import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.entity.LearnedRuleEntity
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.LearnedRulesRepository
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
            learnedRulesDao.insert(rule.toEntity())
        }
    }

    override suspend fun update(rule: LearnedRule) {
        withContext(Dispatchers.IO) {
            learnedRulesDao.update(rule.toEntity())
        }
    }

    override suspend fun delete(ruleId: String) {
        withContext(Dispatchers.IO) {
            learnedRulesDao.deleteById(ruleId)
        }
    }

    override suspend fun getByExpression(expression: String): LearnedRule? =
        withContext(Dispatchers.IO) {
            learnedRulesDao.getByExpression(expression)?.toDomain()
        }
}