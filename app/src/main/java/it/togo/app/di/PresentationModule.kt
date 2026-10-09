package it.togo.app.di

import it.togo.app.MainViewModel
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.CatalogRepository
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
    
    // Future ViewModels will be added here (Epic 4, 5):
    // viewModel { VoiceInputViewModel(get(), get()) }
    // viewModel { ItemDetailViewModel(get(), get()) }
    // viewModel { HistoryViewModel(get(), get()) }
    // viewModel { LearnedRulesViewModel(get()) }
}