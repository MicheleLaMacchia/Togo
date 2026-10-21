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
 *
 * Privacy: TOGO non registra audio, non crea file o buffer PCM, non scrive il
 * testo in log e non dichiara il permesso `INTERNET`. Il testo riconosciuto
 * vive solo in [state] per l'operazione corrente e viene azzerato da [reset].
 * Il riconoscimento è delegato a `RecognizerIntent`: il servizio dell'OS può
 * usare la rete, ma nessun dato viene inviato direttamente dall'app.
 *
 * Nota: il riconoscimento può quindi usare servizi remoti (non è offline).
 */
class AndroidVoiceRecognitionGateway : VoiceRecognitionGateway {

    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    private var launcher: ActivityResultLauncher<Intent>? = null

    /**
     * Finestra in cui un risultato del recognizer è ancora accettabile.
     *
     * `reset()`/`stopListening()` la chiudono: un risultato tardivo che arriva
     * dopo l'annullamento non deve riportare il testo nello stato condiviso.
     */
    private var acceptResult = false

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
     * Rilascia il launcher quando la schermata che lo ha registrato esce dalla
     * composizione, evitando che il singleton trattenga un launcher obsoleto.
     */
    fun unregisterLauncher() {
        // Non chiude la finestra del risultato: la rotazione ricrea il launcher
        // mentre l'operazione vocale può essere ancora in corso. La chiusura è
        // responsabilità di reset()/stopListening()/onCleared().
        launcher = null
    }

    /** Visibile ai test per verificare il ciclo di registrazione del launcher. */
    internal val isLauncherRegistered: Boolean
        get() = launcher != null

    /**
     * Avvia il riconoscimento vocale one-shot.
     *
     * Se già in ascolto, ignora la chiamata.
     * Se il permesso RECORD_AUDIO non è concesso, stato → Error(MISSING_PERMISSION).
     * Se il launcher non è registrato, stato → Error(NOT_AVAILABLE).
     */
    override suspend fun startListening() {
        if (_state.value.isListening || _state.value is VoiceRecognitionState.Processing) return

        // Nota: il controllo permesso è fatto dal presentation layer (che ha il Context)
        // tramite [hasRecordAudioPermission]. Qui gestiamo solo l'assenza del launcher.
        val l = launcher
        if (l == null) {
            acceptResult = false
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
            acceptResult = true
            l.launch(intent)
            _state.value = VoiceRecognitionState.Listening
        } catch (e: Exception) {
            acceptResult = false
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
        acceptResult = false
        _state.value = VoiceRecognitionState.Idle
    }

    /**
     * Resetta lo stato a [VoiceRecognitionState.Idle].
     */
    override fun reset() {
        acceptResult = false
        _state.value = VoiceRecognitionState.Idle
    }

    /**
     * Callback chiamata dall'ActivityResultLauncher quando il risultato è disponibile.
     * Gestisce OK, CANCELED ed errori.
     */
    fun onActivityResult(resultCode: Int, data: Intent?) {
        // Un risultato arrivato dopo cancel/reset/stop non appartiene più
        // all'operazione corrente: non deve riaprire il testo riconosciuto.
        if (!acceptResult) return
        acceptResult = false

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