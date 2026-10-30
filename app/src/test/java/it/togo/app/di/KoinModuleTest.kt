package it.togo.app.di

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import it.togo.app.MainViewModel
import it.togo.app.di.databaseModule
import it.togo.app.di.domainModule
import it.togo.app.di.presentationModule
import it.togo.app.di.repositoryModule
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.domain.repository.TodoRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KoinModuleTest {

    @Before
    fun setup() {
        // Robolectric fornisce un Context Android reale per databaseModule
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(domainModule, databaseModule, repositoryModule, presentationModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Koin modules load without exception`() {
        // Se arriviamo qui, startKoin non ha lanciato eccezioni
        assertTrue("Koin avviato con 4 moduli", true)
    }

    @Test
    fun `MainViewModel resolved via Koin`() {
        val viewModel = GlobalContext.get().get<MainViewModel>()

        assertNotNull("MainViewModel istanziato", viewModel)
        assertTrue("MainViewModel è ViewModel", viewModel is androidx.lifecycle.ViewModel)
    }

    @Test
    fun `LearnedRulesViewModel e registrato nel modulo Koin`() {
        val vm = GlobalContext.get().get<it.togo.app.presentation.rules.LearnedRulesViewModel>()

        assertNotNull("LearnedRulesViewModel risolto via Koin", vm)
        assertTrue("LearnedRulesViewModel è ViewModel", vm is androidx.lifecycle.ViewModel)
    }

    @Test
    fun `AddItemViewModel e registrato nel modulo Koin`() {
        val vm = GlobalContext.get().get<it.togo.app.presentation.additem.AddItemViewModel>()

        assertNotNull("AddItemViewModel risolto via Koin", vm)
        assertTrue("AddItemViewModel è ViewModel", vm is androidx.lifecycle.ViewModel)
    }

    @Test
    fun `SearchViewModel e registrato nel modulo Koin`() {
        val vm = GlobalContext.get().get<it.togo.app.presentation.search.SearchViewModel>()

        assertNotNull("SearchViewModel risolto via Koin", vm)
    }

    @Test
    fun `ListItemRepository e risolto via Koin`() {
        val koin = GlobalContext.get()

        val listItemRepository = koin.get<ListItemRepository>()

        assertNotNull("ListItemRepository istanziato", listItemRepository)
    }

    @Test
    fun `TodoRepository e risolto via Koin`() {
        val repository = GlobalContext.get().get<TodoRepository>()

        assertNotNull("TodoRepository istanziato", repository)
    }

}
