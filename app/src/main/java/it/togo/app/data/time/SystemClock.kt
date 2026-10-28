package it.togo.app.data.time

import it.togo.app.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Implementazione di produzione di [Clock].
 *
 * L'unico punto del nuovo codice autorizzato a leggere direttamente l'orologio
 * di sistema è [nowMillis].
 */
class SystemClock : Clock {

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun today(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(nowMillis()).atZone(zoneId).toLocalDate()
}
