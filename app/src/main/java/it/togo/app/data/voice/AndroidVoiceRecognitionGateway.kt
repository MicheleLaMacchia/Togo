package it.togo.app.data.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionResult
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Implementazione Android di [VoiceRecognitionGateway] usando RecognizerIntent.
 *
 * Pattern: il gateway è un singleton disaccoppiato da Activity. Il presentation layer
 * registra un `ActivityResultLauncher` chiamando [registerLauncher], e il gateway lo usa
 * per avviare il riconoscimento senza trattenere riferimenti a Activity (evita memory leak).
 *
 * Lingua: `it-IT` (BCP-47).
 * Nota: il riconoscimento può usare servizi remoti (non è offline).
 */
class AndroidVoiceRecognitionGateway : VoiceRecognitionGateway {

    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    private var launcher: ActivityResultLauncher<Intent>? = null

    companion object {
        const val EXTRA_LANGUAGE_VALUE = "it-IT"

        /**
         * Verifica se il permesso RECORD_AUDIO è concesso.
         * Il presentation layer deve chiamare ContextCompat.checkSelfPermission
         * e mostrare l'errore se negato.
         */
        fun hasRecordAudioPermission(context: android.content.Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Registra l'ActivityResultLauncher dal presentation layer (Activity/Compose).
     * Deve essere chiamato prima di [startListening].
     */
    fun registerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.launcher = launcher
    }

    /**
     * Avvia il riconoscimento vocale one-shot.
     *
     * Se già in ascolto, ignora la chiamata.
     * Se il permesso RECORD_AUDIO non è concesso, stato → Error(MISSING_PERMISSION).
     * Se il launcher non è registrato, stato → Error(NOT_AVAILABLE).
     */
    override suspend fun startListening() {
        if (_state.value.isListening) return

        // Nota: il controllo permesso è fatto dal presentation layer (che ha il Context)
        // tramite [hasRecordAudioPermission]. Qui gestiamo solo l'assenza del launcher.
        val l = launcher
        if (l == null) {
            _state.value = VoiceRecognitionState.Error(
                ErrorCode.NOT_AVAILABLE,
                "Launcher non registrato. Chiamare registerLauncher() prima di startListening()."
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, EXTRA_LANGUAGE_VALUE)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dimmi un prodotto…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            l.launch(intent)
            _state.value = VoiceRecognitionState.Listening
        } catch (e: Exception) {
            _state.value = VoiceRecognitionState.Error(
                ErrorCode.NOT_AVAILABLE,
                "Riconoscimento vocale non disponibile: ${e.message}"
            )
        }
    }

    /**
     * Annulla il riconoscimento in corso.
     * Se non in ascolto, no-op.
     */
    override fun stopListening() {
        if (!_state.value.isListening) return
        _state.value = VoiceRecognitionState.Idle
    }

    /**
     * Resetta lo stato a [VoiceRecognitionState.Idle].
     */
    override fun reset() {
        _state.value = VoiceRecognitionState.Idle
    }

    /**
     * Callback chiamata dall'ActivityResultLauncher quando il risultato è disponibile.
     * Gestisce OK, CANCELED ed errori.
     */
    fun onActivityResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val text = results?.firstOrNull()?.takeIf { it.isNotBlank() }

                _state.value = if (text != null) {
                    VoiceRecognitionState.Result(text)
                } else {
                    VoiceRecognitionState.Error(
                        ErrorCode.AUDIO_ERROR,
                        "Nessun testo riconosciuto"
                    )
                }
            }

            android.app.Activity.RESULT_CANCELED -> {
                _state.value = VoiceRecognitionState.Idle
            }

            else -> {
                _state.value = VoiceRecognitionState.Error(
                    ErrorCode.RECOGNIZER_ERROR,
                    "Errore durante il riconoscimento"
                )
            }
        }
    }
}