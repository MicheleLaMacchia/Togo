package it.togo.app.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import it.togo.app.domain.parser.VoiceCommandParser
import it.togo.app.domain.usecase.ParseVoiceCommandUseCase
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifica il ciclo di vita del testo riconosciuto (Story 6.4).
 *
 * Il testo deve essere consumato una sola volta, non deve riapparire dopo un
 * nuovo ascolto e deve essere eliminato quando il ViewModel viene distrutto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceTextLifecycleTest {

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
    fun `cancel clears recognized text and resets gateway`() = runTest(testDispatcher) {
        val gateway = FakeVoiceGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte 1 l"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value is VoiceBottomSheetUiState.Ready)

        vm.cancel()
        advanceUntilIdle()

        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
        assertEquals("il gateway deve essere resettato", 1, gateway.resetCount)
    }

    @Test
    fun `confirm delivers text once then clears state`() = runTest(testDispatcher) {
        val gateway = FakeVoiceGateway()
        val vm = VoiceBottomSheetViewModel(gateway)
        var received: String? = null
        var invocations = 0
        vm.onTextConfirmed = { text ->
            received = text
            invocations++
        }

        gateway.emit(VoiceRecognitionState.Result("latte 1 l"))
        advanceUntilIdle()

        vm.confirm()
        vm.confirm()
        advanceUntilIdle()

        assertEquals("latte 1 l", received)
        assertEquals("il testo deve essere consegnato una sola volta", 1, invocations)
        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
        assertEquals(1, gateway.resetCount)
    }

    @Test
    fun `starting a new listening does not reuse the previous text`() = runTest(testDispatcher) {
        val gateway = FakeVoiceGateway()
        val vm = VoiceBottomSheetViewModel(gateway)
        var invocations = 0
        vm.onTextConfirmed = { invocations++ }

        gateway.emit(VoiceRecognitionState.Result("latte 1 l"))
        advanceUntilIdle()
        vm.editText("latte 1 l")
        advanceUntilIdle()

        vm.startListening()
        advanceUntilIdle()

        assertEquals(VoiceBottomSheetUiState.Listening, vm.uiState.value)
        assertFalse(vm.uiState.value is VoiceBottomSheetUiState.Ready)
        assertEquals("startListening non deve confermare nulla", 0, invocations)
    }

    @Test
    fun `editText alone does not confirm the text`() = runTest(testDispatcher) {
        val gateway = FakeVoiceGateway()
        val vm = VoiceBottomSheetViewModel(gateway)
        var invocations = 0
        vm.onTextConfirmed = { invocations++ }

        gateway.emit(VoiceRecognitionState.Result("latte"))
        advanceUntilIdle()
        vm.editText("latte 1 l")
        advanceUntilIdle()

        assertEquals("latte 1 l", (vm.uiState.value as VoiceBottomSheetUiState.Ready).editedText)
        assertEquals("editText non deve confermare nulla", 0, invocations)
    }

    @Test
    fun `onCleared resets gateway and releases the confirmation callback`() = runTest(testDispatcher) {
        val gateway = FakeVoiceGateway()
        val vm = VoiceBottomSheetViewModel(gateway)
        vm.onTextConfirmed = { }
        gateway.emit(VoiceRecognitionState.Result("latte 1 l"))
        advanceUntilIdle()

        val store = storeFor(vm)
        store.clear()
        advanceUntilIdle()

        assertEquals("onCleared deve resettare il gateway", 1, gateway.resetCount)
        assertNull("onCleared deve rilasciare il callback", vm.onTextConfirmed)
        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
    }

    @Test
    fun `confirmation view model clears state and callback on cleared`() = runTest(testDispatcher) {
        val vm = VoiceConfirmationViewModel(ParseVoiceCommandUseCase(VoiceCommandParser()))
        vm.onCommandConfirmed = { }
        vm.showForRecognition("latte")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is VoiceConfirmationUiState.Ready)

        val store = storeFor(vm)
        store.clear()
        advanceUntilIdle()

        assertEquals(VoiceConfirmationUiState.Hidden, vm.uiState.value)
        assertNull(vm.onCommandConfirmed)
    }
}

private fun <T : ViewModel> storeFor(viewModel: T): ViewModelStore {
    val store = ViewModelStore()
    val provider = ViewModelProvider(
        store,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = viewModel as VM
        },
    )
    provider[viewModel::class.java]
    return store
}

private class FakeVoiceGateway : VoiceRecognitionGateway {
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: StateFlow<VoiceRecognitionState> = _state

    var resetCount = 0
        private set

    override suspend fun startListening() {
        _state.value = VoiceRecognitionState.Listening
    }

    override fun stopListening() {
        _state.value = VoiceRecognitionState.Idle
    }

    override fun reset() {
        resetCount++
        _state.value = VoiceRecognitionState.Idle
    }

    fun emit(state: VoiceRecognitionState) {
        _state.value = state
    }
}
