package it.togo.app.presentation.voice

import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.domain.voice.VoiceRecoveryAction
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
 * Test JVM per VoiceResilienceViewModel.
 * Verifica gestione errori, retry con backoff e fallback.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceResilienceViewModelTest {

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
    fun `stato iniziale NoAction e nessun errore`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        assertEquals(VoiceRecoveryAction.NoAction, vm.recoveryAction.value)
        assertNull(vm.errorMessage.value)
        assertFalse(vm.isFallbackActive.value)
    }

    @Test
    fun `errore NETWORK_ERROR tentativo 1 ritorna Retry`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore di rete"))
        Thread.sleep(200)

        assertEquals(VoiceRecoveryAction.Retry, vm.recoveryAction.value)
        assertNotNull(vm.errorMessage.value)
        assertFalse(vm.isFallbackActive.value)
    }

    @Test
    fun `errore NETWORK_ERROR tentativo 3 ritorna UseManualInput`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)
        Thread.sleep(200) // avvia collect

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)

        assertEquals(VoiceRecoveryAction.UseManualInput, vm.recoveryAction.value)
        assertTrue(vm.isFallbackActive.value)
    }

    @Test
    fun `errore MISSING_PERMISSION ritorna OpenSettings`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.MISSING_PERMISSION, "Permesso negato"))
        Thread.sleep(200)

        assertEquals(VoiceRecoveryAction.OpenSettings, vm.recoveryAction.value)
    }

    @Test
    fun `errore NOT_AVAILABLE ritorna UseManualInput`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NOT_AVAILABLE, "Non disponibile"))
        Thread.sleep(200)

        assertEquals(VoiceRecoveryAction.UseManualInput, vm.recoveryAction.value)
        assertTrue(vm.isFallbackActive.value)
    }

    @Test
    fun `successo resetta stato resilienza`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        gateway.emit(VoiceRecognitionState.Result("latte"))
        Thread.sleep(200)

        assertEquals(VoiceRecoveryAction.NoAction, vm.recoveryAction.value)
        assertFalse(vm.isFallbackActive.value)
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun `executeRecovery Retry avvia gateway`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        vm.executeRecovery(VoiceRecoveryAction.Retry)
        Thread.sleep(200)

        assertEquals(1, gateway.startCount)
    }

    @Test
    fun `executeRecovery UseManualInput invoca callback`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)
        var fallbackCalled = false
        vm.onFallbackToManualInput = { fallbackCalled = true }

        vm.executeRecovery(VoiceRecoveryAction.UseManualInput)

        assertTrue(fallbackCalled)
    }

    @Test
    fun `executeRecovery OpenSettings invoca callback`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)
        var settingsCalled = false
        vm.onOpenSettings = { settingsCalled = true }

        vm.executeRecovery(VoiceRecoveryAction.OpenSettings)

        assertTrue(settingsCalled)
    }

    @Test
    fun `cancel resetta tutto`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        vm.cancel()

        assertEquals(VoiceRecoveryAction.NoAction, vm.recoveryAction.value)
        assertFalse(vm.isFallbackActive.value)
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun `backoff delay aumenta con fallimenti`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)
        Thread.sleep(200) // avvia collect

        // Prima di errori: consecutiveFailures=0 → delay 0
        assertEquals(0L, vm.getNextRetryDelayMs())

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        // Dopo 1° errore: consecutiveFailures=1 → delay 500
        assertEquals(500L, vm.getNextRetryDelayMs())

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        // Dopo 2° errore: consecutiveFailures=2 → delay 1000
        assertEquals(1000L, vm.getNextRetryDelayMs())
    }

    @Test(timeout = 100)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val gateway = FakeResilienceGateway()
        val vm = VoiceResilienceViewModel(gateway)

        gateway.emit(VoiceRecognitionState.Error(ErrorCode.NETWORK_ERROR, "Errore"))
        Thread.sleep(200)
        vm.executeRecovery(VoiceRecoveryAction.Retry)
    }
}

/**
 * Fake gateway per test JVM.
 */
private class FakeResilienceGateway : VoiceRecognitionGateway {
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: MutableStateFlow<VoiceRecognitionState> = _state

    var startCount = 0
        private set
    var resetCount = 0
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