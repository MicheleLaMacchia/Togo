package it.togo.app.data.time

import it.togo.app.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Clock deterministico per i test dei repository (Story 8.5).
 *
 * `now` è mutabile per verificare che le transizioni usino l'istante iniettato
 * e che una seconda transizione idempotente non lo aggiorni.
 */
class FakeClock(var now: Long) : Clock {

    override fun nowMillis(): Long = now

    override fun today(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
}
