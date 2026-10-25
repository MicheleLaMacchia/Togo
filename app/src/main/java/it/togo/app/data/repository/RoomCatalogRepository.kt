package it.togo.app.data.repository

import it.togo.app.data.database.CatalogQueries
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.SynonymEntity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.CatalogCategoryResult
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

    override fun searchCategories(query: String): Flow<List<CatalogCategoryResult>> {
        // Il confronto è accent-fold in Kotlin: "caffe" deve trovare "Caffè".
        // La tassonomia ha poche decine di righe, quindi filtrare qui è
        // equivalente a una query SQL ma senza perdere gli accenti.
        val normalized = CatalogQueries.normalize(query)
        if (normalized.isEmpty()) {
            return flowOf(emptyList())
        }
        return catalogDao.getLevel3WithPath().map { rows ->
            rows.asSequence()
                .filter { it.productCount > 0 }
                .map { row -> row to CatalogQueries.normalize(row.name) }
                .filter { (_, name) -> name.contains(normalized) }
                // Le categorie che iniziano con il termine vengono prima
                // (es. "pasta" -> "Pasta secca", non "Formaggi a pasta morbida").
                // A parità di gruppo resta l'ordine tassonomico della query.
                .sortedBy { (_, name) -> if (name.startsWith(normalized)) 0 else 1 }
                .map { (row, _) -> row.toCategoryResult() }
                .toList()
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

            // Rilegge il prodotto con la tassonomia: così il dominio riceve
            // i nomi di L1/L2/L3 e può assegnare le unità compatibili anche
            // per i prodotti appena creati dall'utente.
            catalogDao.getById(id)?.toDomain()
                ?: entity.copy(id = id).toDomain()
        }

    override fun getUserProducts(): Flow<List<CanonicalProduct>> =
        catalogDao.getUserDefinedProducts().map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun updateUserProduct(
        id: Long,
        name: String,
        level3Id: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext false

        val updated = catalogDao.updateUserProduct(id, trimmed, level3Id) > 0
        if (updated) {
            // Il sinonimo accent-fold seguiva il vecchio nome: va rigenerato,
            // altrimenti "caffe" continuerebbe a trovare il nome precedente.
            catalogDao.deleteSynonymsForProduct(id)
            val folded = CatalogQueries.normalize(trimmed)
            if (folded.isNotEmpty()) {
                catalogDao.insertSynonym(
                    SynonymEntity(id = 0, productId = id, term = folded)
                )
            }
        }
        updated
    }

    override suspend fun deleteUserProduct(id: Long): Boolean =
        withContext(Dispatchers.IO) {
            val deleted = catalogDao.deleteUserProduct(id) > 0
            if (deleted) {
                catalogDao.deleteSynonymsForProduct(id)
            }
            deleted
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
