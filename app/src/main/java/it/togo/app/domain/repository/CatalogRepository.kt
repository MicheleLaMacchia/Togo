package it.togo.app.domain.repository

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun search(query: String): Flow<List<CanonicalProduct>>
    suspend fun getById(id: Long): CanonicalProduct?
    fun getTaxonomy(): Flow<List<TaxonomyLevel>>
}