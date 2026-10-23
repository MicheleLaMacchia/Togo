package it.togo.app.presentation.additem

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import it.togo.app.domain.category.CategoryCorrection
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.usecase.CorrectCategoryUseCase
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.CreateUserLevel3UseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelCategoryCorrectionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val product = CanonicalProduct(
        id = 1L,
        name = "Latte Intero",
        level3Id = 5L,
        level1Id = 1L,
        level1Name = "Freschi",
        level2Id = 10L,
        level2Name = "Latticini",
        level3Name = "Latte",
    )

    private val taxonomy = listOf(
        TaxonomyLevel.Level1(id = 1L, name = "Freschi", sortOrder = 1),
        TaxonomyLevel.Level1(id = 2L, name = "Dispensa", sortOrder = 2),
        TaxonomyLevel.Level2(id = 10L, name = "Latticini", sortOrder = 1, level1Id = 1L),
        TaxonomyLevel.Level2(id = 20L, name = "Caffè", sortOrder = 2, level1Id = 2L),
        TaxonomyLevel.Level3(id = 5L, name = "Latte", sortOrder = 1, level2Id = 10L),
        TaxonomyLevel.Level3(id = 12L, name = "Caffè macinato", sortOrder = 1, level2Id = 20L),
    )

    private fun createViewModel(
        useCase: CorrectCategoryUseCase,
    ): AddItemViewModel {
        val catalogRepository = mockk<CatalogRepository>(relaxed = true)
        every { catalogRepository.getTaxonomy() } returns flowOf(taxonomy)

        return AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = mockk<ShoppingListRepository>(relaxed = true),
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
            createLevel3UseCase = CreateUserLevel3UseCase(catalogRepository),
            correctCategoryUseCase = useCase,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `correzione categoria apre il prompt e salva la regola solo su conferma`() =
        runTest(testDispatcher) {
            val useCase = mockk<CorrectCategoryUseCase>()
            val correction = CategoryCorrection(
                originalLevel3Id = 5L,
                correctedLevel3Id = 12L,
                product = product,
            )
            coEvery { useCase.invoke(any(), any()) } returns correction
            every { useCase.canShowRememberPrompt(any(), any()) } returns true
            coEvery { useCase.confirmRemember(any(), any()) } returns LearnedRule(
                id = "rule-1",
                userExpression = "caffe",
                productId = 1L,
                level3Id = 12L,
            )

            val viewModel = createViewModel(useCase)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
            viewModel.onEvent(AddItemUiEvent.ShowCategoryCorrection)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isCategoryCorrectionVisible)
            assertNotNull(viewModel.uiState.value.categoryCorrectionLevel3Id)

            viewModel.onEvent(
                AddItemUiEvent.CategoryCorrectionExpressionChanged("caffe"),
            )
            viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel1Selected(2L))
            viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel2Selected(20L))
            viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel3Selected(12L))
            viewModel.onEvent(AddItemUiEvent.ApplyCategoryCorrection)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isCategoryCorrectionVisible)
            assertNotNull(viewModel.uiState.value.pendingCategoryCorrection)
            assertEquals(
                "Dispensa > Caffè > Caffè macinato",
                viewModel.uiState.value.correctedCategoryPath,
            )
            coVerify(exactly = 0) { useCase.confirmRemember(any(), any()) }

            viewModel.onEvent(AddItemUiEvent.ConfirmRememberCategory)
            advanceUntilIdle()

            coVerify(exactly = 1) { useCase.confirmRemember(any(), any()) }
            assertNull(viewModel.uiState.value.pendingCategoryCorrection)
            assertEquals(
                "Regola appresa salvata",
                viewModel.uiState.value.categoryCorrectionMessage,
            )
        }

    @Test
    fun `decline non crea la regola`() = runTest(testDispatcher) {
        val useCase = mockk<CorrectCategoryUseCase>()
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            product = product,
        )
        coEvery { useCase.invoke(any(), any()) } returns correction
        every { useCase.canShowRememberPrompt(any(), any()) } returns true
        every { useCase.declineRemember() } returns true

        val viewModel = createViewModel(useCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        viewModel.onEvent(AddItemUiEvent.ShowCategoryCorrection)
        viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel1Selected(2L))
        viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel2Selected(20L))
        viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel3Selected(12L))
        viewModel.onEvent(AddItemUiEvent.ApplyCategoryCorrection)
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.DismissRememberCategory)
        advanceUntilIdle()

        coVerify(exactly = 0) { useCase.confirmRemember(any(), any()) }
        assertEquals(
            "Nessuna regola salvata",
            viewModel.uiState.value.categoryCorrectionMessage,
        )
    }
}
