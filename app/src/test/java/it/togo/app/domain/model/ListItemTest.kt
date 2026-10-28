package it.togo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ListItemTest {

    private fun validItem(): ListItem = ListItem.create(
        id = "item-1",
        productId = 42L,
        quantity = 2.0,
        unit = "kg",
        createdAt = 1_000L,
    )

    @Test
    fun `factory trims unit and applies status defaults`() {
        val item = ListItem.create(
            id = "item-1",
            productId = 42L,
            quantity = 2.0,
            unit = "  kg  ",
            createdAt = 1_000L,
        )

        assertEquals("kg", item.unit)
        assertEquals(ListItemStatus.TO_BUY, item.status)
        assertEquals(0, item.purchaseCount)
        assertEquals(1_000L, item.statusChangedAt)
        assertEquals(1_000L, item.updatedAt)
        assertEquals(1_000L, item.createdAt)
    }

    @Test
    fun `blank unit is rejected`() {
        for (unit in listOf("", "   ", "\t\n")) {
            assertThrows(IllegalArgumentException::class.java) {
                ListItem.create(
                    id = "item-1",
                    productId = 42L,
                    quantity = 2.0,
                    unit = unit,
                    createdAt = 1_000L,
                )
            }
        }
    }

    @Test
    fun `invalid quantities are rejected`() {
        val invalidQuantities = listOf(
            0.0,
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
        )

        for (quantity in invalidQuantities) {
            assertThrows("quantity=$quantity", IllegalArgumentException::class.java) {
                ListItem.create(
                    id = "item-1",
                    productId = 42L,
                    quantity = quantity,
                    unit = "kg",
                    createdAt = 1_000L,
                )
            }
        }
    }

    @Test
    fun `purchase count must be consistent with status`() {
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.create(
                id = "item-1",
                productId = 42L,
                quantity = 2.0,
                unit = "kg",
                createdAt = 1_000L,
                status = ListItemStatus.TO_BUY,
                purchaseCount = 1,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ListItem.create(
                id = "item-1",
                productId = 42L,
                quantity = 2.0,
                unit = "kg",
                createdAt = 1_000L,
                status = ListItemStatus.DELETED,
                purchaseCount = 1,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ListItem.create(
                id = "item-1",
                productId = 42L,
                quantity = 2.0,
                unit = "kg",
                createdAt = 1_000L,
                status = ListItemStatus.PURCHASED,
                purchaseCount = 0,
            )
        }
    }

    @Test
    fun `blank id is rejected`() {
        for (id in listOf("", "   ")) {
            assertThrows(IllegalArgumentException::class.java) {
                ListItem.create(
                    id = id,
                    productId = 42L,
                    quantity = 2.0,
                    unit = "kg",
                    createdAt = 1_000L,
                )
            }
        }
    }

    @Test
    fun `non positive product id is rejected`() {
        for (productId in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                ListItem.create(
                    id = "item-1",
                    productId = productId,
                    quantity = 2.0,
                    unit = "kg",
                    createdAt = 1_000L,
                )
            }
        }
    }

    @Test
    fun `transition matrix exposes only the allowed edges`() {
        assertTrue(ListItemStatus.TO_BUY.canTransitionTo(ListItemStatus.PURCHASED))
        assertTrue(ListItemStatus.TO_BUY.canTransitionTo(ListItemStatus.DELETED))
        assertTrue(ListItemStatus.DELETED.canTransitionTo(ListItemStatus.TO_BUY))

        assertFalse(ListItemStatus.TO_BUY.canTransitionTo(ListItemStatus.TO_BUY))
        assertFalse(ListItemStatus.DELETED.canTransitionTo(ListItemStatus.DELETED))
        assertFalse(ListItemStatus.DELETED.canTransitionTo(ListItemStatus.PURCHASED))
        assertFalse(ListItemStatus.PURCHASED.canTransitionTo(ListItemStatus.TO_BUY))
        assertFalse(ListItemStatus.PURCHASED.canTransitionTo(ListItemStatus.DELETED))
        assertFalse(ListItemStatus.PURCHASED.canTransitionTo(ListItemStatus.PURCHASED))
    }

    @Test
    fun `TO_BUY to PURCHASED updates status count and timestamps`() {
        val original = validItem()

        val purchased = original.transitionTo(ListItemStatus.PURCHASED, at = 2_000L)

        assertEquals(ListItemStatus.PURCHASED, purchased.status)
        assertEquals(1, purchased.purchaseCount)
        assertEquals(2_000L, purchased.statusChangedAt)
        assertEquals(2_000L, purchased.updatedAt)
        assertEquals(original.id, purchased.id)
        assertEquals(original.createdAt, purchased.createdAt)
    }

    @Test
    fun `TO_BUY to DELETED updates status count and timestamps`() {
        val original = validItem()

        val deleted = original.transitionTo(ListItemStatus.DELETED, at = 2_000L)

        assertEquals(ListItemStatus.DELETED, deleted.status)
        assertEquals(0, deleted.purchaseCount)
        assertEquals(2_000L, deleted.statusChangedAt)
        assertEquals(2_000L, deleted.updatedAt)
        assertEquals(original.id, deleted.id)
        assertEquals(original.createdAt, deleted.createdAt)
    }

    @Test
    fun `DELETED to TO_BUY updates status count and timestamps`() {
        val deleted = ListItem.create(
            id = "item-1",
            productId = 42L,
            quantity = 2.0,
            unit = "kg",
            createdAt = 1_000L,
            status = ListItemStatus.DELETED,
        )

        val restored = deleted.transitionTo(ListItemStatus.TO_BUY, at = 2_000L)

        assertEquals(ListItemStatus.TO_BUY, restored.status)
        assertEquals(0, restored.purchaseCount)
        assertEquals(2_000L, restored.statusChangedAt)
        assertEquals(2_000L, restored.updatedAt)
        assertEquals(deleted.id, restored.id)
        assertEquals(deleted.createdAt, restored.createdAt)
    }

    @Test
    fun `PURCHASED cannot be reopened or deleted`() {
        val purchased = validItem().transitionTo(ListItemStatus.PURCHASED, at = 2_000L)

        assertThrows(IllegalArgumentException::class.java) {
            purchased.transitionTo(ListItemStatus.TO_BUY, at = 3_000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            purchased.transitionTo(ListItemStatus.DELETED, at = 3_000L)
        }

        assertEquals(ListItemStatus.PURCHASED, purchased.status)
        assertEquals(2_000L, purchased.statusChangedAt)
        assertEquals(2_000L, purchased.updatedAt)
    }

    @Test
    fun `same status transition is idempotent and keeps timestamps`() {
        val toBuy = validItem()
        assertSame(toBuy, toBuy.transitionTo(ListItemStatus.TO_BUY, at = 9_000L))

        val purchased = toBuy.transitionTo(ListItemStatus.PURCHASED, at = 2_000L)
        assertSame(purchased, purchased.transitionTo(ListItemStatus.PURCHASED, at = 9_000L))
        assertEquals(2_000L, purchased.statusChangedAt)
        assertEquals(2_000L, purchased.updatedAt)

        val deleted = toBuy.transitionTo(ListItemStatus.DELETED, at = 2_000L)
        assertSame(deleted, deleted.transitionTo(ListItemStatus.DELETED, at = 9_000L))
        assertEquals(2_000L, deleted.statusChangedAt)
        assertEquals(2_000L, deleted.updatedAt)
    }
}
