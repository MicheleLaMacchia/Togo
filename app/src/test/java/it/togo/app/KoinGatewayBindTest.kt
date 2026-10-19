package it.togo.app

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import it.togo.app.di.domainModule
import it.togo.app.data.voice.AndroidVoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionGateway

/**
 * Test Koin: verifica che il gateway vocale sia registrato sia come tipo concreto che come interfaccia.
 *
 * Fix 5: Koin bind gateway — single { AndroidVoiceRecognitionGateway() } bind VoiceRecognitionGateway::class
 */
class KoinGatewayBindTest {

    @Test
    fun `gateway risolve sia come tipo concreto che come interfaccia`() {
        // Arrange: igiene Koin (TogoApplication può aver già avviato una app nei test Robolectric)
        stopKoin()
        startKoin { modules(domainModule) }

        try {
            // Act
            val koin = org.koin.core.context.GlobalContext.get()
            val concrete = koin.get<AndroidVoiceRecognitionGateway>()
            val iface = koin.get<VoiceRecognitionGateway>()

            // Assert
            assertNotNull("Concrete gateway istanziato", concrete)
            assertNotNull("Interface gateway risolto", iface)
            assertSame("Stessa istanza per concrete e interface", concrete, iface)
        } finally {
            stopKoin()
        }
    }
}
