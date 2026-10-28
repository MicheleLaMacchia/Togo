package it.togo.app.data.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SystemClockTest {

    @Test
    fun `nowMillis is the current epoch time`() {
        val before = System.currentTimeMillis()
        val actual = SystemClock().nowMillis()
        val after = System.currentTimeMillis()

        assertTrue(
            "nowMillis deve essere compreso tra le letture di sistema",
            actual in before..after,
        )
    }

    @Test
    fun `today uses the requested zone`() {
        val zone = ZoneId.of("Europe/Rome")
        val clock = SystemClock()

        val before = LocalDate.now(zone)
        val actual = clock.today(zone)
        val after = LocalDate.now(zone)

        assertTrue(
            "today deve coincidere con il giorno di sistema nel fuso richiesto",
            actual == before || actual == after,
        )
    }

    @Test
    fun `SystemZoneIdProvider returns the device zone`() {
        assertEquals(ZoneId.systemDefault(), SystemZoneIdProvider().current())
    }
}
