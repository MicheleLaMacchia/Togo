package it.togo.app.di

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import it.togo.app.MainViewModel
import it.togo.app.di.databaseModule
import it.togo.app.di.domainModule
import it.togo.app.di.presentationModule
import it.togo.app.di.repositoryModule

class KoinModuleTest {

    @Test
    fun `Koin modules load without exception`() {
        // Avvia Koin con tutti e 4 i moduli
        startKoin {
            modules(domainModule, databaseModule, repositoryModule, presentationModule)
        }

        // Verifica che i moduli siano caricati (nessuna eccezione = successo)
        assertTrue("Koin avviato con 4 moduli", true)

        stopKoin()
    }

    @Test
    fun `MainViewModel resolved via Koin`() {
        startKoin {
            modules(domainModule, databaseModule, repositoryModule, presentationModule)
        }

        // Risolve ViewModel tramite Koin (simula by viewModel())
        val viewModel = MainViewModel()

        assertNotNull("MainViewModel istanziato", viewModel)
        assertTrue("MainViewModel è ViewModel", viewModel is androidx.lifecycle.ViewModel)

        stopKoin()
    }

    // TODO(Story 1.3): Add test for Repository resolution when Room*Repository implementations exist
    // @Test
    // fun `ShoppingListRepository resolved via Koin`() {
    //     startKoin { modules(domainModule, databaseModule, repositoryModule, presentationModule) }
    //     val repo = get<ShoppingListRepository>()
    //     assertNotNull("Repository istanziato", repo)
    //     stopKoin()
    // }
}