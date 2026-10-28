package it.togo.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Verifica che `java.time` sia utilizzabile con la configurazione API 24
 * (minSdk), inclusa nel set Robolectric preparato dal build.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class JavaTimeApi24Test {

    @Test
    fun `LocalDate and ZoneId are available on API 24`() {
        val dueDate = LocalDate.parse("2026-11-02", DateTimeFormatter.ISO_LOCAL_DATE)
        val rome = ZoneId.of("Europe/Rome")
        val utc = ZoneId.of("UTC")
        val instant = Instant.parse("2026-11-02T23:30:00Z")

        assertEquals(LocalDate.of(2026, 11, 2), dueDate)
        assertEquals(LocalDate.of(2026, 11, 2), instant.atZone(utc).toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 3), instant.atZone(rome).toLocalDate())
    }
}
