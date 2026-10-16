package it.togo.app.presentation.voice

import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.parser.VoiceCommandParser
import it.togo.app.domain.usecase.ParseVoiceCommandUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test JVM per VoiceConfirmationViewModel.
 * Verifica MVI: parsing, conferma, annulla, modifica testo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceConfirmationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): VoiceConfirmationViewModel {
        val parser = VoiceCommandParser()
        val useCase = ParseVoiceCommandUseCase(parser)
        return VoiceConfirmationViewModel(useCase)
    }

    @Test
    fun `stato iniziale Hidden`() = runTest(testDispatcher) {
        val vm = createViewModel()

        assertEquals(VoiceConfirmationUiState.Hidden, vm.uiState.value)
        assertFalse(vm.uiState.value.isVisible)
    }

    @Test
    fun `showForRecognition esegue parsing e mostra Ready`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("2 litri di latte")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is VoiceConfirmationUiState.Ready)
        assertEquals("latte", (state as VoiceConfirmationUiState.Ready).parsedCommand.productName)
        assertEquals(2.0, state.parsedCommand.quantity!!, 0.001)
        assertEquals(StandardUnit.LITER, state.parsedCommand.unit)
    }

    @Test
    fun `confirm invoca callback e chiude`() = runTest(testDispatcher) {
        val vm = createViewModel()
        var confirmed: ParsedCommand? = null
        vm.onCommandConfirmed = { confirmed = it }

        vm.showForRecognition("latte")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.confirm()

        assertNotNull(confirmed)
        assertEquals("latte", confirmed!!.productName)
        assertEquals(VoiceConfirmationUiState.Hidden, vm.uiState.value)
    }

    @Test
    fun `cancel chiude bottom sheet`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("latte")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.cancel()

        assertEquals(VoiceConfirmationUiState.Hidden, vm.uiState.value)
    }

    @Test
    fun `editText re-parsing con nuovo testo e verifica displayCommand`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("latte")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.editText("2 litri di latte intero")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is VoiceConfirmationUiState.Ready)
        // Verifica che displayCommand contenga la scomposizione corretta
        assertEquals("latte intero", (state as VoiceConfirmationUiState.Ready).displayCommand.productName)
        assertEquals(2.0, state.displayCommand.quantity!!, 0.001)
        assertEquals(StandardUnit.LITER, state.displayCommand.unit)
    }

    @Test
    fun `needsClarification mostra avviso`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("2 latte")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is VoiceConfirmationUiState.Ready)
        assertTrue((state as VoiceConfirmationUiState.Ready).parsedCommand.needsClarification)
    }

    @Test
    fun `conferma disabilitata se prodotto vuoto`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        if (state is VoiceConfirmationUiState.Ready) {
            assertFalse(state.canConfirm)
        }
    }

    @Test
    fun `toggleEdit alterna modalità modifica`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("latte")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleEdit()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is VoiceConfirmationUiState.Ready)
        assertTrue((state as VoiceConfirmationUiState.Ready).isEditing)
    }

    @Test
    fun `reset torna a Hidden`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("latte")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.reset()

        assertEquals(VoiceConfirmationUiState.Hidden, vm.uiState.value)
    }

    @Test(timeout = 100)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.showForRecognition("2 litri di latte")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.confirm()
    }
}