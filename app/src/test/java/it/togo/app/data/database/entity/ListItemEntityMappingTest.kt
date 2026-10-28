package it.togo.app.data.database.entity

import it.togo.app.domain.model.ListItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ListItemEntityMappingTest {

    private fun fullEntity(): ListItemEntity = ListItemEntity(
        id = "item-1",
        productId = 42L,
        quantity = 1.5,
        unit = "kg",
        brand = "Brand",
        variant = "Variante",
        condition = "Conservazione",
        note = "Nota con\nseconda riga",
        status = "PURCHASED",
        purchaseCount = 3,
        createdAt = 1_000L,
        statusChangedAt = 2_000L,
        updatedAt = 3_000L,
    )

    @Test
    fun `entity to domain to entity preserves every field`() {
        val entity = fullEntity()

        val domain = entity.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals("item-1", domain.id)
        assertEquals(42L, domain.productId)
        assertEquals(1.5, domain.quantity, 0.0)
        assertEquals("kg", domain.unit)
        assertEquals("Brand", domain.brand)
        assertEquals("Variante", domain.variant)
        assertEquals("Conservazione", domain.condition)
        assertEquals("Nota con\nseconda riga", domain.note)
        assertEquals(ListItemStatus.PURCHASED, domain.status)
        assertEquals(3, domain.purchaseCount)
        assertEquals(1_000L, domain.createdAt)
        assertEquals(2_000L, domain.statusChangedAt)
        assertEquals(3_000L, domain.updatedAt)
        assertEquals(entity, roundTrip)
    }

    @Test
    fun `nullable attributes remain null through the mapping`() {
        val entity = fullEntity().copy(
            brand = null,
            variant = null,
            condition = null,
            note = null,
            status = "TO_BUY",
            purchaseCount = 0,
        )

        val domain = entity.toDomain()

        assertNull(domain.brand)
        assertNull(domain.variant)
        assertNull(domain.condition)
        assertNull(domain.note)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `invalid status string fails explicitly`() {
        val entity = fullEntity().copy(status = "BOUGHT")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }

        assertTrue(
            "Il messaggio deve riportare lo stato non valido",
            exception.message.orEmpty().contains("BOUGHT"),
        )
    }
}
