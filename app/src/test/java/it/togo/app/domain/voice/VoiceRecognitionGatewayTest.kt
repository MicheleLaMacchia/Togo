package it.togo.app.domain.voice

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
 * Test JVM per VoiceRecognitionGateway (con mock implementation).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceRecognitionGatewayTest {

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
    fun `startListening transita a Listening`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.startListening()

        assertEquals(VoiceRecognitionState.Listening, gateway.state.value)
        assertTrue(gateway.state.value.isListening)
    }

    @Test
    fun `stopListening torna a Idle`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.startListening()
        gateway.stopListening()

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `reset torna a Idle da qualsiasi stato`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.startListening()
        gateway.reset()

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `startListening due volte non duplica`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.startListening()
        gateway.startListening()

        assertEquals(1, gateway.startCount)
    }

    @Test
    fun `stopListening senza start e no-op`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.stopListening()

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `simula Success aggiorna stato`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.simulateResult(VoiceRecognitionResult.Success("latte"))

        assertEquals("latte", gateway.state.value.resultText)
        assertTrue(gateway.state.value.isResult)
    }

    @Test
    fun `simula Error aggiorna stato`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.simulateResult(VoiceRecognitionResult.Error(ErrorCode.NETWORK_ERROR, "Errore"))

        assertTrue(gateway.state.value.isError)
    }

    @Test
    fun `simula Cancelled torna a Idle`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.simulateResult(VoiceRecognitionResult.Cancelled)

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test(timeout = 100)
    fun `operazioni gateway completate sotto 100ms`() = runTest(testDispatcher) {
        val gateway = FakeVoiceRecognitionGateway()

        gateway.startListening()
        gateway.stopListening()
        gateway.reset()
    }
}

/**
 * Fake implementation per test JVM.
 */
private class FakeVoiceRecognitionGateway : VoiceRecognitionGateway {
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: MutableStateFlow<VoiceRecognitionState> = _state

    var startCount = 0
        private set

    override suspend fun startListening() {
        if (_state.value.isListening) return
        startCount++
        _state.value = VoiceRecognitionState.Listening
    }

    override fun stopListening() {
        if (!_state.value.isListening) return
        _state.value = VoiceRecognitionState.Idle
    }

    override fun reset() {
        _state.value = VoiceRecognitionState.Idle
    }

    fun simulateResult(result: VoiceRecognitionResult) {
        _state.value = when (result) {
            is VoiceRecognitionResult.Success -> VoiceRecognitionState.Result(result.text)
            is VoiceRecognitionResult.Error -> VoiceRecognitionState.Error(result.code, result.message)
            VoiceRecognitionResult.Cancelled -> VoiceRecognitionState.Idle
            VoiceRecognitionResult.NoSpeech -> VoiceRecognitionState.Error(ErrorCode.AUDIO_ERROR, "Nessun parlato")
        }
    }
}