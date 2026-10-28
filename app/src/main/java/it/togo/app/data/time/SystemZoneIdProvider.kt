package it.togo.app.data.time

import it.togo.app.domain.time.ZoneIdProvider
import java.time.ZoneId

/**
 * Implementazione di produzione di [ZoneIdProvider].
 *
 * Restituisce il fuso configurato sul dispositivo, inclusi i cambi automatici
 * gestiti dal sistema operativo.
 */
class SystemZoneIdProvider : ZoneIdProvider {

    override fun current(): ZoneId = ZoneId.systemDefault()
}
