package it.togo.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClockTest {

    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private val utc: ZoneId = ZoneId.of("UTC")

    /**
     * Fake clock deterministico: stesso contratto di [Clock], ma l'istante è
     * fissato dal test.
     */
    private class FakeClock(private val instant: Instant) : Clock {
        override fun nowMillis(): Long = instant.toEpochMilli()

        override fun today(zoneId: ZoneId): LocalDate =
            instant.atZone(zoneId).toLocalDate()
    }

    private fun isOverdue(dueDate: LocalDate, clock: Clock, zoneId: ZoneId): Boolean =
        dueDate.isBefore(clock.today(zoneId))

    @Test
    fun `nowMillis returns the fixed instant`() {
        val instant = Instant.parse("2026-11-02T12:00:00Z")
        val clock = FakeClock(instant)

        assertEquals(instant.toEpochMilli(), clock.nowMillis())
    }

    @Test
    fun `today returns the same day in Rome and UTC away from midnight`() {
        val clock = FakeClock(Instant.parse("2026-11-02T12:00:00Z"))

        assertEquals(LocalDate.of(2026, 11, 2), clock.today(rome))
        assertEquals(LocalDate.of(2026, 11, 2), clock.today(utc))
    }

    @Test
    fun `today changes day between Rome and UTC near midnight`() {
        // 23:30 UTC = 00:30 del giorno successivo a Roma (CET, UTC+1).
        val clock = FakeClock(Instant.parse("2026-11-02T23:30:00Z"))

        assertEquals(LocalDate.of(2026, 11, 2), clock.today(utc))
        assertEquals(LocalDate.of(2026, 11, 3), clock.today(rome))
    }

    @Test
    fun `overdue uses the injected zone and not the machine default`() {
        val dueDate = LocalDate.of(2026, 11, 2)
        val clock = FakeClock(Instant.parse("2026-11-02T23:30:00Z"))

        assertFalse("A UTC il 2 novembre non è ancora scaduto", isOverdue(dueDate, clock, utc))
        assertTrue("A Roma è già il 3 novembre", isOverdue(dueDate, clock, rome))
    }

    @Test
    fun `LocalDate ISO round trip does not shift across time zones`() {
        val dueDate = LocalDate.of(2026, 11, 2)
        val persisted = DateTimeFormatter.ISO_LOCAL_DATE.format(dueDate)

        assertEquals("2026-11-02", persisted)

        for (zone in listOf(rome, utc)) {
            val reread = LocalDate.parse(persisted, DateTimeFormatter.ISO_LOCAL_DATE)
            assertEquals("Il giorno persistito non deve cambiare in $zone", dueDate, reread)
            assertEquals(
                "La mezzanotte locale resta lo stesso giorno in $zone",
                dueDate,
                reread.atStartOfDay(zone).toLocalDate(),
            )
        }
    }
}
