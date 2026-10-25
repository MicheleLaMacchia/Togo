package it.togo.app.data.repository

import it.togo.app.data.database.dao.UserAttributeDao
import it.togo.app.data.database.entity.UserAttributeEntity
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.ProductAttribute
import it.togo.app.domain.model.RenameUserAttributeResult
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.domain.repository.UserAttributeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementazione Room del registro marche/varianti user-defined.
 *
 * La forma salvata è sempre "capitalized" ("barilla" -> "Barilla") e il
 * confronto usa [ProductNameNormalizer.normalize], così maiuscole e accenti
 * non creano duplicati.
 */
class RoomUserAttributeRepository(
    private val userAttributeDao: UserAttributeDao,
) : UserAttributeRepository {

    override fun observe(type: UserAttributeType): Flow<List<UserAttribute>> =
        userAttributeDao.getByType(type.name).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeSuggestions(): Flow<AttributeSuggestions> =
        combine(
            observe(UserAttributeType.BRAND),
            observe(UserAttributeType.VARIANT),
        ) { brands, variants ->
            AttributeSuggestions(
                brands = brands.map { it.value },
                variants = variants.map { it.value },
            )
        }

    override suspend fun remember(type: UserAttributeType, rawValue: String) {
        withContext(Dispatchers.IO) {
            val value = normalizedDisplayValue(rawValue) ?: return@withContext

            // Una marca scelta dai valori predefiniti non è "creata
            // dall'utente": non deve comparire nelle Impostazioni.
            if (type == UserAttributeType.BRAND &&
                ProductAttribute.defaultBrands.any { it.equals(value, ignoreCase = true) }
            ) {
                return@withContext
            }

            val normalized = ProductNameNormalizer.normalize(value)
            if (normalized.isEmpty()) return@withContext

            userAttributeDao.insertIgnore(
                UserAttributeEntity(
                    type = type.name,
                    value = value,
                    normalized = normalized,
                )
            )
        }
    }

    override suspend fun rename(
        id: Long,
        type: UserAttributeType,
        rawValue: String,
    ): RenameUserAttributeResult = withContext(Dispatchers.IO) {
        val value = normalizedDisplayValue(rawValue)
            ?: return@withContext RenameUserAttributeResult.Invalid
        val normalized = ProductNameNormalizer.normalize(value)
        if (normalized.isEmpty()) return@withContext RenameUserAttributeResult.Invalid

        val duplicate = userAttributeDao.findDuplicate(type.name, normalized, id)
        if (duplicate != null) {
            return@withContext RenameUserAttributeResult.Duplicate(duplicate.toDomain())
        }

        val updated = userAttributeDao.updateValue(id, value, normalized)
        if (updated > 0) {
            RenameUserAttributeResult.Renamed(
                UserAttribute(id = id, type = type, value = value)
            )
        } else {
            RenameUserAttributeResult.Invalid
        }
    }

    override suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        userAttributeDao.deleteById(id) > 0
    }

    /** Forma visualizzata, o null se il valore è vuoto. */
    private fun normalizedDisplayValue(rawValue: String): String? =
        ProductNameNormalizer.capitalizeFirstWord(rawValue).ifBlank { null }
}
