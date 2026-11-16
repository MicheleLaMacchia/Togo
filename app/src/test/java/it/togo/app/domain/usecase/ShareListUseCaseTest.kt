package it.togo.app.domain.usecase

import android.content.Intent
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareListUseCaseTest {

    @Test
    fun `spesa vuota con azioni attive condivide il blocco da fare`() = runTest {
        val actions = listOf(
            ActionItem(text = "Chiamare il medico"),
            ActionItem(text = "Ritirare il pacco"),
            ActionItem(text = "Azione gia spuntata", isChecked = true),
        )

        val intent = ShareListUseCase(
            shoppingListRepository = FakeShoppingListRepository(actions),
            catalogRepository = FakeCatalogRepository,
        )(ShareListUseCase.Part.ACTIONS)

        assertNotNull(intent)
        assertEquals(
            "[DA FARE]\n+ ALTRO\n- A TEMPO PERSO\n  Chiamare il medico\n  Ritirare il pacco",
            intent?.getStringExtra(Intent.EXTRA_TEXT),
        )
        assertEquals("Cose da fare TOGO", intent?.getStringExtra(Intent.EXTRA_SUBJECT))
    }

    @Test
    fun `solo azioni senza azioni attive restituisce null`() = runTest {
        val intent = ShareListUseCase(
            shoppingListRepository = FakeShoppingListRepository(
                listOf(ActionItem(text = "Gia spuntata", isChecked = true)),
            ),
            catalogRepository = FakeCatalogRepository,
        )(ShareListUseCase.Part.ACTIONS)

        assertNull(intent)
    }

    @Test
    fun `invocazione predefinita continua a condividere la spesa`() = runTest {
        val shoppingItem = ShoppingItem(
            productId = 101,
            quantity = 0.0,
            unit = StandardUnit.PIECE,
        )
        val intent = ShareListUseCase(
            shoppingListRepository = FakeShoppingListRepository(items = listOf(shoppingItem)),
            catalogRepository = FakeCatalogRepository,
        )()

        assertNotNull(intent)
        assertEquals(
            "[FRUTTA]\n+ FRESCA\n- FRUTTA\n  Mele",
            intent?.getStringExtra(Intent.EXTRA_TEXT),
        )
        assertEquals("Lista spesa TOGO", intent?.getStringExtra(Intent.EXTRA_SUBJECT))
    }

    private class FakeShoppingListRepository(
        private val actions: List<ActionItem> = emptyList(),
        private val items: List<ShoppingItem> = emptyList(),
    ) : ShoppingListRepository {
        override fun getAllItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())
        override fun getActiveItems(): Flow<List<ShoppingItem>> = flowOf(items)
        override fun getCheckedItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())
        override suspend fun insert(item: ShoppingItem) = Unit
        override suspend fun update(item: ShoppingItem) = Unit
        override suspend fun delete(itemId: String) = Unit
        override suspend fun getByProductId(productId: Long): ShoppingItem? = null
        override suspend fun checkout(checkedItemIds: List<String>): Boolean = true
        override fun getActions(): Flow<List<ActionItem>> = flowOf(actions)
    }

    private object FakeCatalogRepository : CatalogRepository {
        override fun search(query: String): Flow<List<CanonicalProduct>> = flowOf(emptyList())
        override suspend fun getById(id: Long): CanonicalProduct? =
            if (id == 101L) CanonicalProduct(id = 101, name = "Mele", level3Id = 3) else null

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = flowOf(
            listOf(
                TaxonomyLevel.Level1(id = 1, name = "Frutta", sortOrder = 1),
                TaxonomyLevel.Level2(id = 2, name = "Fresca", sortOrder = 1, level1Id = 1),
                TaxonomyLevel.Level3(id = 3, name = "Frutta", sortOrder = 1, level2Id = 2),
            ),
        )
    }
}
