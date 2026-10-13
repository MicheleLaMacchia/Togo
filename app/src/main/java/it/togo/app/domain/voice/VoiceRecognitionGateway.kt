package it.togo.app.domain.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * Gateway per il riconoscimento vocale (astrazione dominio).
 *
 * Il presentation layer usa questa interfaccia per avviare/ascoltare
 * il riconoscimento vocale senza conoscere l'implementazione Android.
 *
 * Implementazione Android: [AndroidVoiceRecognitionGateway] (RecognizerIntent).
 */
interface VoiceRecognitionGateway {

    /**
     * Stato corrente del riconoscimento (osservabile da UI).
     */
    val state: StateFlow<VoiceRecognitionState>

    /**
     * Avvia il riconoscimento vocale one-shot.
     *
     * Se già in ascolto, ignora la chiamata.
     * Lancia il microfono e attende il risultato (o errore/annullamento).
     */
    suspend fun startListening()

    /**
     * Annulla il riconoscimento in corso.
     * Se non in ascolto, no-op.
     */
    fun stopListening()

    /**
     * Resetta lo stato a [VoiceRecognitionState.Idle].
     * Chiamare dopo aver consumato il risultato o errore.
     */
    fun reset()
}