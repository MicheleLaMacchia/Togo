package it.togo.app.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceErrorRecovery
import it.togo.app.domain.voice.VoiceFallbackHandler
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.domain.voice.VoiceRecoveryAction
import it.togo.app.domain.voice.VoiceResiliencePolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel per la resilienza vocale.
 *
 * Orchestra retry con backoff e fallback a input manuale.
 * Osserva il gateway e applica la politica di resilienza.
 */
class VoiceResilienceViewModel(
    private val gateway: VoiceRecognitionGateway,
    private val policy: VoiceResiliencePolicy = VoiceResiliencePolicy.DEFAULT
) : ViewModel() {

    private val fallbackHandler = VoiceFallbackHandler(policy)

    private val _recoveryAction = MutableStateFlow<VoiceRecoveryAction>(VoiceRecoveryAction.NoAction)
    val recoveryAction: StateFlow<VoiceRecoveryAction> = _recoveryAction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isFallbackActive = MutableStateFlow(false)
    val isFallbackActive: StateFlow<Boolean> = _isFallbackActive.asStateFlow()

    /** Callback per aprire input manuale */
    var onFallbackToManualInput: (() -> Unit)? = null

    /** Callback per aprire impostazioni di sistema */
    var onOpenSettings: (() -> Unit)? = null

    /**
     * Retry programmato. Va annullato su cancel/onCleared: altrimenti il
     * microfono potrebbe riaprirsi dopo che l'utente ha annullato.
     */
    private var retryJob: Job? = null

    init {
        viewModelScope.launch {
            gateway.state.collect { state ->
                handleState(state)
            }
        }
    }

    private fun handleState(state: VoiceRecognitionState) {
        when (state) {
            is VoiceRecognitionState.Error -> handleError(state.code)
            is VoiceRecognitionState.Result -> {
                fallbackHandler.recordSuccess()
                _isFallbackActive.value = false
                _recoveryAction.value = VoiceRecoveryAction.NoAction
                _errorMessage.value = null
            }
            else -> { /* Listening, Processing, Idle: nessuna azione */ }
        }
    }

    private fun handleError(code: ErrorCode) {
        _errorMessage.value = VoiceErrorRecovery.getUserMessage(code)

        val shouldFallback = fallbackHandler.recordFailure(code)
        val action = VoiceErrorRecovery.getRecoveryAction(
            code = code,
            attempt = fallbackHandler.getConsecutiveFailures(),
            maxAttempts = policy.maxAttempts
        )

        _recoveryAction.value = action

        if (shouldFallback || action == VoiceRecoveryAction.UseManualInput) {
            _isFallbackActive.value = true
        }
    }

    /**
     * Esegue l'azione di recupero selezionata.
     */
    fun executeRecovery(action: VoiceRecoveryAction) {
        when (action) {
            VoiceRecoveryAction.Retry -> {
                _recoveryAction.value = VoiceRecoveryAction.NoAction
                val delayMs = getNextRetryDelayMs()
                retryJob?.cancel()
                retryJob = viewModelScope.launch {
                    delay(delayMs)
                    gateway.reset()
                    gateway.startListening()
                }
            }
            VoiceRecoveryAction.UseManualInput -> {
                _isFallbackActive.value = true
                onFallbackToManualInput?.invoke()
            }
            VoiceRecoveryAction.OpenSettings -> {
                onOpenSettings?.invoke()
            }
            VoiceRecoveryAction.NoAction -> { /* nessuna azione */ }
        }
    }

    /**
     * Avvia il riconoscimento vocale.
     */
    fun startListening() {
        viewModelScope.launch {
            gateway.startListening()
        }
    }

    /**
     * Annulla e resetta lo stato di resilienza.
     */
    fun cancel() {
        retryJob?.cancel()
        retryJob = null
        gateway.reset()
        fallbackHandler.reset()
        _isFallbackActive.value = false
        _recoveryAction.value = VoiceRecoveryAction.NoAction
        _errorMessage.value = null
    }

    override fun onCleared() {
        retryJob?.cancel()
        retryJob = null
        gateway.reset()
        onFallbackToManualInput = null
        onOpenSettings = null
        super.onCleared()
    }

    /**
     * Delay di backoff per il prossimo tentativo.
     */
    fun getNextRetryDelayMs(): Long = fallbackHandler.getNextRetryDelayMs()
}