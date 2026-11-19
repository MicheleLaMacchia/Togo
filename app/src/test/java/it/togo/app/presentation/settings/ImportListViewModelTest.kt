package it.togo.app.presentation.settings

import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.UserAttributeRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportListViewModelTest {

    private fun newViewModel() = ImportListViewModel(
        catalogRepository = mockk(relaxed = true),
        shoppingListRepository = mockk(relaxed = true),
        userAttributeRepository = mockk(relaxed = true),
    )

    @Test
    fun `svuota cancella il testo inserito`() {
        val viewModel = newViewModel()
        viewModel.onTextChange("testo incollato")
        viewModel.clearText()

        assertEquals("", viewModel.uiState.value.text)
    }
}
