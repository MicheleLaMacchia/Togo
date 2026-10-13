package it.togo.app.di

import it.togo.app.MainViewModel
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.presentation.search.SearchViewModel
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val presentationModule: Module = module {
    // ViewModels - factory (new instance per navigation/owner)
    viewModel { MainViewModel() }
    
    // Epic 2 - Lista Attiva
    viewModel { ActiveListViewModel(
        shoppingRepository = get<ShoppingListRepository>(),
        catalogRepository = get<CatalogRepository>(),
    ) }
    
    // Epic 3 - Ricerca
    viewModel { SearchViewModel(
        catalogRepository = get<CatalogRepository>(),
    ) }
    
    // Epic 4 - Voice Recognition
    viewModel { VoiceBottomSheetViewModel(
        gateway = get<VoiceRecognitionGateway>(),
    ) }

    // Future ViewModels will be added here (Epic 4, 5):
    // viewModel { VoiceInputViewModel(get(), get()) }
    // viewModel { ItemDetailViewModel(get(), get()) }
    // viewModel { HistoryViewModel(get(), get()) }
    // viewModel { LearnedRulesViewModel(get()) }
}