package it.togo.app.presentation

import it.togo.app.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class FixedClock(private val now: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = now
    override fun today(zoneId: ZoneId): LocalDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
}
