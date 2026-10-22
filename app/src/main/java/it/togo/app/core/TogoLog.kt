package it.togo.app.core

import android.util.Log

/**
 * Logger unico dell'app.
 *
 * Tutte le righe usano lo stesso tag [TAG]: basta filtrare con
 * `adb logcat -s Togo:*` per vedere solo gli eventi applicativi, senza
 * il rumore dei log di sistema (MIUI/Compose/IME).
 *
 * Volutamente sottile: nessuna dipendenza aggiuntiva, così resta usabile
 * anche dai test JVM (Robolectric shadow di `android.util.Log`).
 */
object TogoLog {

    const val TAG: String = "Togo"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }
}
