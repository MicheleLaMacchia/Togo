package it.togo.app.data.repository

import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomCatalogRepository(
    private val catalogDao: CatalogDao
) : CatalogRepository {

    override fun search(query: String): Flow<List<CanonicalProduct>> =
        catalogDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getById(id: Long): CanonicalProduct? =
        withContext(Dispatchers.IO) {
            catalogDao.getById(id)?.toDomain()
        }

    override fun getTaxonomy(): Flow<List<TaxonomyLevel>> =
        catalogDao.getFullTaxonomy().map { results ->
            results.flatMap { join ->
                val level1 = join.level1.toDomain()
                val level2 = join.level2?.toDomain()
                val level3 = join.level3?.toDomain()

                val list = mutableListOf<TaxonomyLevel>(level1)
                level2?.let { list.add(it) }
                level3?.let { list.add(it) }
                list
            }
        }
}