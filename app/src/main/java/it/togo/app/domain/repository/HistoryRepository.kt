package it.togo.app.domain.repository

import it.togo.app.domain.model.HistoricalItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAll(): Flow<List<HistoricalItem>>
    suspend fun insert(item: HistoricalItem)
    suspend fun upsertHistorical(item: HistoricalItem)
    suspend fun getByProductId(productId: Long): HistoricalItem?
}