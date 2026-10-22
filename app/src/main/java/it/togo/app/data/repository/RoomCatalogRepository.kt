package it.togo.app.data.repository

import it.togo.app.data.database.CatalogQueries
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.SynonymEntity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    override suspend fun getByExactSynonym(term: String): CanonicalProduct? {
        val normalized = CatalogQueries.normalize(term)
        if (normalized.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            catalogDao.getByExactSynonym(normalized)
                .singleOrNull()
                ?.toDomain()
        }
    }

    override suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct> =
        withContext(Dispatchers.IO) {
            catalogDao.getByLevel3(level3Id).map { it.toDomain() }
        }

    override suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            val entity = CanonicalProductEntity(
                id = 0,
                level3Id = level3Id,
                name = trimmed,
                isUserDefined = true,
            )
            val id = catalogDao.insertProduct(entity)

            // La ricerca (`CatalogQueries.SEARCH`) normalizza solo la query e
            // confronta il nome grezzo: senza questo sinonimo accent-fold un
            // prodotto come "Caffè" non sarebbe trovato digitando "caffe".
            val folded = CatalogQueries.normalize(trimmed)
            if (folded.isNotEmpty()) {
                catalogDao.insertSynonym(
                    SynonymEntity(id = 0, productId = id, term = folded)
                )
            }

            entity.copy(id = id).toDomain()
        }

    override suspend fun getLevel3ByLevel2(level2Id: Long): List<TaxonomyLevel.Level3> =
        withContext(Dispatchers.IO) {
            catalogDao.getLevel3ByLevel2(level2Id).first().map { it.toDomain() }
        }

    override suspend fun insertLevel3(name: String, level2Id: Long): TaxonomyLevel.Level3 =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            val sortOrder = catalogDao.nextLevel3SortOrder(level2Id)
            val entity = TaxonomyLevel3Entity(
                id = 0,
                level2Id = level2Id,
                name = trimmed,
                sortOrder = sortOrder,
                isUserDefined = true,
            )
            val id = catalogDao.insertLevel3(entity)
            entity.copy(id = id).toDomain()
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
