package it.togo.app.presentation.voice

import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test JVM per VoiceBottomSheetViewModel.
 * Verifica MVI: gateway state → UI state, azioni utente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceBottomSheetViewModelTest {

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
    fun `stato iniziale Hidden`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
        assertFalse(vm.uiState.value.isVisible)
    }

    @Test
    fun `gateway Listening trasmette a UI Listening`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Listening)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(VoiceBottomSheetUiState.Listening, vm.uiState.value)
        assertTrue(vm.uiState.value.isVisible)
    }

    @Test
    fun `gateway Result trasmette a UI Ready con testo`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is VoiceBottomSheetUiState.Ready)
        assertEquals("latte", (state as VoiceBottomSheetUiState.Ready).recognizedText)
        assertEquals("latte", state.editedText)
        assertFalse(state.isEditing)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `gateway Error trasmette a UI Error`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore di rete"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is VoiceBottomSheetUiState.Error)
        assertEquals("NETWORK_ERROR", (state as VoiceBottomSheetUiState.Error).code)
        assertEquals("Errore di rete", state.message)
    }

    @Test
    fun `gateway Idle nasconde bottom sheet`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()
        gateway.emit(VoiceRecognitionState.Idle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
    }

    @Test
    fun `editText aggiorna editedText e isEditing`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.editText("latte intero")

        val state = vm.uiState.value
        assertTrue(state is VoiceBottomSheetUiState.Ready)
        assertEquals("latte intero", (state as VoiceBottomSheetUiState.Ready).editedText)
        assertTrue(state.isEditing)
    }

    @Test
    fun `confirm chiude bottom sheet e invoca callback`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)
        var confirmedText: String? = null
        vm.onTextConfirmed = { confirmedText = it }

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.confirm()

        assertEquals("latte", confirmedText)
        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
    }

    @Test
    fun `retry resetta gateway e riavvia ascolto`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.retry()

        assertEquals(1, gateway.resetCount)
        assertEquals(1, gateway.startCount)
    }

    @Test
    fun `cancel chiude bottom sheet e resetta gateway`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Listening)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.cancel()

        assertEquals(1, gateway.resetCount)
        assertEquals(VoiceBottomSheetUiState.Hidden, vm.uiState.value)
    }

    @Test
    fun `Ready con testo vuoto non puo confermare`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.editText("")

        val state = vm.uiState.value
        assertTrue(state is VoiceBottomSheetUiState.Ready)
        assertFalse((state as VoiceBottomSheetUiState.Ready).canConfirm)
    }

    @Test(timeout = 100)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val gateway = FakeGateway()
        val vm = VoiceBottomSheetViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Result("latte"))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.editText("latte intero")
        vm.confirm()
    }
}

/**
 * Fake gateway per test JVM.
 */
private class FakeGateway : it.togo.app.domain.voice.VoiceRecognitionGateway {
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: MutableStateFlow<VoiceRecognitionState> = _state

    var resetCount = 0
        private set
    var startCount = 0
        private set

    override suspend fun startListening() {
        startCount++
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