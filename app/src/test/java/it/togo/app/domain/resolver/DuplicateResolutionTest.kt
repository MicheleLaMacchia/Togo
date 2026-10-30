package it.togo.app.domain.resolver

import it.togo.app.domain.model.ResolutionOption
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.StandardUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Test JVM per la logica di risoluzione duplicati (Story 3.5).
 *
 * Verifica le operazioni di somma, sostituzione, separazione e annullamento.
 */
class DuplicateResolutionTest {

    // Helper per creare un ListItem di test
    private fun createItem(
        productId: Long = 1,
        quantity: Double = 1.0,
        unit: StandardUnit = StandardUnit.LITER
    ) = ListItem.create(
        id = UUID.randomUUID().toString(),
        productId = productId,
        quantity = quantity,
        unit = unit.name,
        createdAt = 1_000L,
    )

    // ========== SUM ==========

    @Test
    fun `sum option adds quantities correctly`() {
        val existing = createItem(quantity = 1.0, unit = StandardUnit.LITER)
        val newItem = createItem(quantity = 2.0, unit = StandardUnit.LITER)
        val result = applyResolution(existing, newItem, ResolutionOption.SUM)
        assertEquals(3.0, result.quantity, 0.001)
        assertEquals(StandardUnit.LITER.name, result.unit)
    }

    @Test
    fun `sum option with different unit types does not apply`() {
        val existing = createItem(quantity = 1.0, unit = StandardUnit.LITER)
        val newItem = createItem(quantity = 500.0, unit = StandardUnit.MILLILITER)
        // La somma con unità incompatibili dovrebbe restituire l'item originale
        val result = applyResolution(existing, newItem, ResolutionOption.SUM)
        // Non avendo logica di conversione, la somma con unità diverse è un no-op
        assertEquals(existing.quantity, result.quantity, 0.001)
    }

    @Test
    fun `sum option with same product and same unit succeeds`() {
        val existing = createItem(productId = 42, quantity = 3.0, unit = StandardUnit.KILOGRAM)
        val newItem = createItem(productId = 42, quantity = 2.0, unit = StandardUnit.KILOGRAM)
        val result = applyResolution(existing, newItem, ResolutionOption.SUM)
        assertEquals(5.0, result.quantity, 0.001)
    }

    // ========== REPLACE ==========

    @Test
    fun `replace option substitutes quantity`() {
        val existing = createItem(quantity = 5.0)
        val newItem = createItem(quantity = 2.0)
        val result = applyResolution(existing, newItem, ResolutionOption.REPLACE)
        assertEquals(2.0, result.quantity, 0.001)
    }

    @Test
    fun `replace option keeps existing id`() {
        val existing = createItem(quantity = 5.0)
        val newItem = createItem(quantity = 2.0)
        val result = applyResolution(existing, newItem, ResolutionOption.REPLACE)
        assertEquals(existing.id, result.id)
    }

    // ========== SEPARATE ==========

    @Test
    fun `separate option returns new item with new id`() {
        val existing = createItem(quantity = 1.0)
        val newItem = createItem(quantity = 2.0)
        val result = applyResolution(existing, newItem, ResolutionOption.SEPARATE)
        assertTrue("ID should be different", result.id != existing.id)
        assertEquals(newItem.quantity, result.quantity, 0.001)
    }

    // ========== CANCEL ==========

    @Test
    fun `cancel option returns existing item unchanged`() {
        val existing = createItem(quantity = 1.0)
        val newItem = createItem(quantity = 2.0)
        val result = applyResolution(existing, newItem, ResolutionOption.CANCEL)
        assertEquals(existing.id, result.id)
        assertEquals(existing.quantity, result.quantity, 0.001)
    }

    // ========== Helper function ==========

    /**
     * Applica la risoluzione duplicati.
     * Questa è una semplificazione della logica che sarà implementata nel ViewModel.
     */
    private fun applyResolution(
        existing: ListItem,
        newItem: ListItem,
        option: ResolutionOption
    ): ListItem {
        return when (option) {
            ResolutionOption.SUM -> {
                if (existing.unit == newItem.unit) {
                    existing.copy(quantity = existing.quantity + newItem.quantity)
                } else {
                    existing
                }
            }
            ResolutionOption.REPLACE -> existing.copy(quantity = newItem.quantity)
            ResolutionOption.SEPARATE -> newItem.copy(id = UUID.randomUUID().toString())
            ResolutionOption.CANCEL -> existing
        }
    }
}
