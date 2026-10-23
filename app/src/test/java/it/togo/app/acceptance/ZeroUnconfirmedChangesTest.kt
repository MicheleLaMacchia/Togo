package it.togo.app.acceptance

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.presentation.activelist.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Accettazione SM-C2 (Story 6.4): zero modifiche non confermate.
 *
 * Comportamento noto e tracciato in `deferred-work.md`: il testo vocale
 * confermato arriva come `UiEvent.VoiceCommand`, ma `ActiveListViewModel`
 * ignora l'evento. Il test verifica il comportamento attuale (zero scritture),
 * non un percorso di conferma end-to-end: quando l'inserimento vocale sarà
 * cablato, questo test andrà sostituito da un test che distingua testo
 * confermato e non confermato.
 *
 * Robolectric è necessario perché `ActiveListViewModel.onEvent` usa
 * [it.togo.app.core.TogoLog], che a sua volta usa `android.util.Log`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZeroUnconfirmedChangesTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ignored voice command produces no repository writes and no list change`() =
        runTest(testDispatcher) {
            val item = ShoppingItem(
                id = "item-1",
                productId = 1L,
                quantity = 1.0,
                unit = StandardUnit.LITER,
            )
            val shopping = RecordingShoppingListRepository(listOf(item))
            val catalog = FakeCatalogRepository()
            val viewModel = ActiveListViewModel(shopping, catalog)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            advanceUntilIdle()

            val before = viewModel.uiState.value
            assertEquals("la lista iniziale deve avere una voce", 1, before.activeCount)
            assertEquals(0, shopping.writeCount)

            viewModel.onEvent(UiEvent.VoiceCommand("latte 1 l"))
            advanceUntilIdle()

            assertEquals(
                "VoiceCommand non deve scrivere su ShoppingListRepository",
                0,
                shopping.writeCount,
            )
            assertEquals("la lista non deve cambiare", listOf(item), shopping.items.value)
            assertEquals("lo stato UI non deve cambiare", before, viewModel.uiState.value)
        }

    @Test
    fun `only confirm emits recognized text`() {
        val voiceSource = readProjectFile(
            "app/src/main/java/it/togo/app/presentation/voice/VoiceBottomSheetViewModel.kt"
        )
        val confirmBody = methodBody(voiceSource, "fun confirm()")
        assertTrue(
            "solo confirm() deve invocare onTextConfirmed",
            confirmBody.contains("onTextConfirmed?.invoke"),
        )

        for (method in listOf("fun startListening()", "fun editText(", "fun cancel()", "fun retry()")) {
            val body = methodBody(voiceSource, method)
            assertFalse(
                "$method non deve emettere testo confermato",
                body.contains("onTextConfirmed?.invoke"),
            )
        }

        val emissions = Regex("""onTextConfirmed\?\.invoke""").findAll(voiceSource).count()
        assertEquals("deve esistere un solo punto di emissione del testo", 1, emissions)
    }

    private fun methodBody(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("anchor mancante nel sorgente: $signature", start >= 0)
        val nextMember = source.indexOf("\n    /**", start)
        return if (nextMember >= 0) source.substring(start, nextMember)
        else source.substring(start)
    }

    private fun readProjectFile(relativePath: String): String =
        projectFile(relativePath).readText()

    private class RecordingShoppingListRepository(
        initialItems: List<ShoppingItem>,
    ) : ShoppingListRepository {
        val items = MutableStateFlow(initialItems)
        var writeCount = 0
            private set

        override fun getAllItems(): Flow<List<ShoppingItem>> = items
        override fun getActiveItems(): Flow<List<ShoppingItem>> = items
        override fun getCheckedItems(): Flow<List<ShoppingItem>> = items

        override suspend fun insert(item: ShoppingItem) {
            writeCount++
            items.value = items.value + item
        }

        override suspend fun update(item: ShoppingItem) {
            writeCount++
            items.value = items.value.map { if (it.id == item.id) item else it }
        }

        override suspend fun delete(itemId: String) {
            writeCount++
            items.value = items.value.filterNot { it.id == itemId }
        }

        override suspend fun getByProductId(productId: Long): ShoppingItem? =
            items.value.firstOrNull { it.productId == productId }

        override suspend fun checkout(checkedItemIds: List<String>): Boolean {
            writeCount++
            return true
        }
    }

    private class FakeCatalogRepository : CatalogRepository {
        private val product = CanonicalProduct(id = 1L, name = "Latte", level3Id = 3L)
        private val taxonomy = listOf(
            TaxonomyLevel.Level1(id = 1L, name = "Latticini", sortOrder = 1),
            TaxonomyLevel.Level2(id = 2L, name = "Latte", sortOrder = 1, level1Id = 1L),
            TaxonomyLevel.Level3(id = 3L, name = "Latte UHT", sortOrder = 1, level2Id = 2L),
        )

        override fun search(query: String): Flow<List<CanonicalProduct>> =
            flowOf(listOf(product).filter { it.name.contains(query, ignoreCase = true) })

        override suspend fun getById(id: Long): CanonicalProduct? =
            product.takeIf { it.id == id }

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = flowOf(taxonomy)
    }
}
