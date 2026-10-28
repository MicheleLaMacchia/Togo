package it.togo.app.di

import it.togo.app.data.time.SystemClock
import it.togo.app.data.time.SystemZoneIdProvider
import it.togo.app.domain.time.Clock
import it.togo.app.domain.time.ZoneIdProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.dsl.koinApplication

class TimeBindingsTest {

    @Test
    fun `Koin provides Clock and ZoneIdProvider`() {
        val koin = koinApplication {
            modules(domainModule)
        }.koin

        try {
            val clock = koin.get<Clock>()
            val zoneIdProvider = koin.get<ZoneIdProvider>()

            assertNotNull(clock)
            assertNotNull(zoneIdProvider)
            assertTrue("Clock deve usare l'implementazione di sistema", clock is SystemClock)
            assertTrue(
                "ZoneIdProvider deve usare l'implementazione di sistema",
                zoneIdProvider is SystemZoneIdProvider,
            )
        } finally {
            koin.close()
        }
    }
}
