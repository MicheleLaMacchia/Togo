package it.togo.app.data.repository

import it.togo.app.data.database.CatalogQueries
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomCatalogRepository(
    private val catalogDao: CatalogDao
) : CatalogRepository {

    override fun search(query: String): Flow<List<CanonicalProduct>> {
        // Normalizzazione accent-fold + escaping LIKE; query vuota => nessun risultato
        // (evita che una query vuota o con jolly LIKE restituisca l'intero catalogo).
        val normalized = CatalogQueries.escapeLike(CatalogQueries.normalize(query))
        if (normalized.isEmpty()) {
            return flowOf(emptyList())
        }
        return catalogDao.search(normalized).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): CanonicalProduct? =
        withContext(Dispatchers.IO) {
            catalogDao.getById(id)?.toDomain()
        }

    /**
     * Tassonomia piatta e ordinata per corsia: combina i tre livelli,
     * rispettando l'ordine L1 -> L2 -> L3 (AD-3: catalogo preinstallato).
     */
    override fun getTaxonomy(): Flow<List<TaxonomyLevel>> =
        combine(
            catalogDao.getLevel1(),
            catalogDao.getLevel2(),
            catalogDao.getLevel3()
        ) { level1s, level2s, level3s ->
            val result = mutableListOf<TaxonomyLevel>()
            level1s.forEach { l1 ->
                result.add(l1.toDomain())
                level2s.filter { it.level1Id == l1.id }.forEach { l2 ->
                    result.add(l2.toDomain())
                    level3s.filter { it.level2Id == l2.id }.forEach { l3 ->
                        result.add(l3.toDomain())
                    }
                }
            }
            result
        }
}
