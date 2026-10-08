package it.togo.app.data.repository

import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.domain.model.HistoricalItem
import it.togo.app.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomHistoryRepository(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAll(): Flow<List<HistoricalItem>> =
        historyDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insert(item: HistoricalItem) {
        withContext(Dispatchers.IO) {
            historyDao.insert(item.toEntity())
        }
    }

    override suspend fun upsertHistorical(item: HistoricalItem) {
        withContext(Dispatchers.IO) {
            historyDao.upsertHistorical(item.toEntity())
        }
    }

    override suspend fun getByProductId(productId: Long): HistoricalItem? =
        withContext(Dispatchers.IO) {
            historyDao.getByProductId(productId)?.toDomain()
        }
}