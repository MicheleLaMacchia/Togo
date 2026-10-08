package it.togo.app.di

import it.togo.app.MainViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val presentationModule: Module = module {
    // ViewModels - factory (new instance per navigation/owner)
    viewModel { MainViewModel() }
    
    // Future ViewModels will be added here (Epic 2, 4, 5):
    // viewModel { ActiveListViewModel(get(), get()) }
    // viewModel { VoiceInputViewModel(get(), get()) }
    // viewModel { ItemDetailViewModel(get(), get()) }
    // viewModel { HistoryViewModel(get(), get()) }
    // viewModel { LearnedRulesViewModel(get()) }
}