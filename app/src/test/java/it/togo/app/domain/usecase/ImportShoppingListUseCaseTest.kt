package it.togo.app.domain.usecase

import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ProductAttribute
import it.togo.app.domain.model.RenameUserAttributeResult
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.UserAttributeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportShoppingListUseCaseTest {

    private val level1 = TaxonomyLevel.Level1(id = 1, name = "PASTA, RISO E CEREALI", sortOrder = 1)
    private val level2 = TaxonomyLevel.Level2(
        id = 10,
        name = "PASTA SECCA",
        sortOrder = 1,
        level1Id = level1.id,
    )
    private val level3 = TaxonomyLevel.Level3(
        id = 100,
        name = "SPAGHETTI",
        sortOrder = 1,
        level2Id = level2.id,
    )

    private val spaghetti = CanonicalProduct(
        id = 1,
        name = "Spaghetti",
        level3Id = level3.id,
    )

    private val textWithSpaghetti = """
        [PASTA, RISO E CEREALI]
        + PASTA SECCA
        - SPAGHETTI
          Spaghetti Barilla integrale 2 pz
    """.trimIndent()

    private fun createUseCase(
        catalog: FakeCatalogRepository? = null,
        shopping: FakeShoppingListRepository? = null,
        attributes: FakeUserAttributeRepository? = null,
    ) = ImportShoppingListUseCase(
        catalog ?: FakeCatalogRepository(),
        shopping ?: FakeShoppingListRepository(),
        attributes ?: FakeUserAttributeRepository(),
    )

    @Test
    fun `preview risolve prodotto esistente e marca e variante`() = runTest {
        val catalog = FakeCatalogRepository(
            initialProducts = listOf(spaghetti),
        )
        val useCase = createUseCase(catalog = catalog)

        val result = useCase.preview(textWithSpaghetti)

        assertTrue(result is ImportPreviewResult.Ready)
        val plan = (result as ImportPreviewResult.Ready).plan
        assertEquals(1, plan.productCount)
        assertEquals(0, plan.newProductCount)
        assertEquals(0, plan.newLevel3Count)
        assertEquals(spaghetti.id, plan.items.single().productId)
        assertEquals("Barilla", plan.items.single().brand)
        assertEquals("Integrale", plan.items.single().variant)
        assertEquals(StandardUnit.PIECE, plan.items.single().unit)
    }

    @Test
    fun `categoria di primo livello sconosciuta viene rifiutata`() = runTest {
        val text = """
            [CATEGORIA INESISTENTE]
            + SOTTOCATEGORIA
            - PRODOTTI
              Qualcosa 1 pz
        """.trimIndent()

        val result = createUseCase().preview(text)

        assertTrue(result is ImportPreviewResult.Error)
        assertTrue((result as ImportPreviewResult.Error).message.contains("primo livello"))
    }

    @Test
    fun `sottocategoria sconosciuta viene rifiutata`() = runTest {
        val text = """
            [PASTA, RISO E CEREALI]
            + SOTTOCATEGORIA INESISTENTE
            - PRODOTTI
              Qualcosa 1 pz
        """.trimIndent()

        val result = createUseCase().preview(text)

        assertTrue(result is ImportPreviewResult.Error)
        assertTrue((result as ImportPreviewResult.Error).message.contains("SOTTOCATEGORIA INESISTENTE"))
    }

    @Test
    fun `execute crea categoria e prodotto mancanti e inserisce la voce`() = runTest {
        val catalog = FakeCatalogRepository()
        val shopping = FakeShoppingListRepository()
        val text = """
            [PASTA, RISO E CEREALI]
            + PASTA SECCA
            - PENNE
              Penne Barilla 2 pz
        """.trimIndent()

        val useCase = createUseCase(catalog = catalog, shopping = shopping)
        val preview = useCase.preview(text) as ImportPreviewResult.Ready
        val result = useCase.execute(preview.plan)

        assertTrue(result is ImportExecutionResult.Success)
        val success = result as ImportExecutionResult.Success
        assertEquals(1, success.createdCategories)
        assertEquals(1, success.createdProducts)
        assertEquals(1, catalog.products.size)
        assertEquals("Penne", catalog.products.single().name)
        assertEquals("Penne", catalog.level3s.first { it.id == catalog.products.single().level3Id }.name)
        assertEquals(1, shopping.items.size)
        assertEquals(2.0, shopping.items.single().quantity, 0.0)
        assertEquals(StandardUnit.PIECE, shopping.items.single().unit)
        assertEquals("Barilla", shopping.items.single().brand)
    }

    @Test
    fun `execute sostituisce la quantita della voce gia in lista`() = runTest {
        val catalog = FakeCatalogRepository(initialProducts = listOf(spaghetti))
        val existing = ShoppingItem(
            id = "existing",
            productId = spaghetti.id,
            quantity = 1.0,
            unit = StandardUnit.PIECE,
        )
        val shopping = FakeShoppingListRepository(mutableListOf(existing))

        val useCase = createUseCase(catalog = catalog, shopping = shopping)
        val preview = useCase.preview(textWithSpaghetti) as ImportPreviewResult.Ready
        val result = useCase.execute(preview.plan)

        assertTrue(result is ImportExecutionResult.Success)
        val success = result as ImportExecutionResult.Success
        assertEquals(1, shopping.items.size)
        assertEquals(2.0, shopping.items.single().quantity, 0.0)
        assertEquals(1, success.updatedExisting)
    }

    @Test
    fun `execute conserva la marca e la variante della riga esportata`() = runTest {
        val catalog = FakeCatalogRepository(initialProducts = listOf(spaghetti))
        val shopping = FakeShoppingListRepository()

        val useCase = createUseCase(catalog = catalog, shopping = shopping)
        val preview = useCase.preview(textWithSpaghetti) as ImportPreviewResult.Ready
        useCase.execute(preview.plan)

        val item = shopping.items.single()
        assertEquals("Barilla", item.brand)
        assertEquals("Integrale", item.variant)
    }

    @Test
    fun `prodotto nuovo riconosce variante prima della marca`() = runTest {
        val catalog = FakeCatalogRepository()
        val shopping = FakeShoppingListRepository()
        val text = """
            [PASTA, RISO E CEREALI]
            + PASTA SECCA
            - PENNE
              Penne integrale Barilla 2 pz
        """.trimIndent()

        val useCase = createUseCase(catalog = catalog, shopping = shopping)
        val preview = useCase.preview(text) as ImportPreviewResult.Ready
        val planned = preview.plan.items.single()

        assertEquals("Penne", planned.productName)
        assertEquals("Barilla", planned.brand)
        assertEquals("Integrale", planned.variant)

        useCase.execute(preview.plan)
        assertEquals("Penne", catalog.products.single().name)
        assertEquals("Barilla", shopping.items.single().brand)
        assertEquals("Integrale", shopping.items.single().variant)
    }

    @Test
    fun `prodotto nuovo senza marca mantiene il nome intero`() = runTest {
        val catalog = FakeCatalogRepository()
        val shopping = FakeShoppingListRepository()
        val text = """
            [PASTA, RISO E CEREALI]
            + PASTA SECCA
            - PENNE
              Penne rigate 2 pz
        """.trimIndent()

        val useCase = createUseCase(catalog = catalog, shopping = shopping)
        val preview = useCase.preview(text) as ImportPreviewResult.Ready
        useCase.execute(preview.plan)

        assertNotNull(catalog.products.singleOrNull())
        assertEquals("Penne rigate", catalog.products.single().name)
    }

    private inner class FakeCatalogRepository(
        initialProducts: List<CanonicalProduct> = emptyList(),
    ) : CatalogRepository {

        val level3s = mutableListOf(level3)
        val products = initialProducts.toMutableList()
        private var nextProductId = 1_000L
        private var nextLevel3Id = 1_000L

        override fun search(query: String): Flow<List<CanonicalProduct>> =
            flowOf(products.filter { it.name.contains(query, ignoreCase = true) })

        override suspend fun getById(id: Long): CanonicalProduct? =
            products.firstOrNull { it.id == id }

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> =
            flowOf(listOf(level1, level2) + level3s)

        override suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct> =
            products.filter { it.level3Id == level3Id }

        override suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct {
            val product = CanonicalProduct(
                id = nextProductId++,
                name = name,
                level3Id = level3Id,
                isUserDefined = true,
            )
            products += product
            return product
        }

        override suspend fun getLevel3ByLevel2(level2Id: Long): List<TaxonomyLevel.Level3> =
            level3s.filter { it.level2Id == level2Id }

        override suspend fun insertLevel3(name: String, level2Id: Long): TaxonomyLevel.Level3 {
            val level3 = TaxonomyLevel.Level3(
                id = nextLevel3Id++,
                name = name,
                sortOrder = level3s.size + 1,
                level2Id = level2Id,
                isUserDefined = true,
            )
            level3s += level3
            return level3
        }
    }

    private class FakeShoppingListRepository(
        initialItems: MutableList<ShoppingItem> = mutableListOf(),
    ) : ShoppingListRepository {

        val items = initialItems

        override fun getAllItems(): Flow<List<ShoppingItem>> = flowOf(items.toList())

        override fun getActiveItems(): Flow<List<ShoppingItem>> =
            flowOf(items.filter { !it.isChecked })

        override fun getCheckedItems(): Flow<List<ShoppingItem>> =
            flowOf(items.filter { it.isChecked })

        override suspend fun insert(item: ShoppingItem) {
            items += item
        }

        override suspend fun update(item: ShoppingItem) {
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) items[index] = item
        }

        override suspend fun delete(itemId: String) {
            items.removeAll { it.id == itemId }
        }

        override suspend fun getByProductId(productId: Long): ShoppingItem? =
            items.firstOrNull { it.productId == productId && !it.isChecked }

        override suspend fun checkout(checkedItemIds: List<String>): Boolean {
            items.removeAll { it.id in checkedItemIds }
            return true
        }

        override fun getAttributeSuggestions(): Flow<AttributeSuggestions> =
            flowOf(
                AttributeSuggestions(
                    brands = ProductAttribute.defaultBrands,
                    variants = listOf("Integrale"),
                )
            )
    }

    private class FakeUserAttributeRepository : UserAttributeRepository {
        override fun observe(type: UserAttributeType): Flow<List<UserAttribute>> =
            flowOf(emptyList())

        override suspend fun remember(type: UserAttributeType, rawValue: String) = Unit

        override suspend fun rename(
            id: Long,
            type: UserAttributeType,
            rawValue: String,
        ): RenameUserAttributeResult = RenameUserAttributeResult.Invalid

        override suspend fun delete(id: Long): Boolean = false
    }
}
