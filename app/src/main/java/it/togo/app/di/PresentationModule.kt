package it.togo.app.di

import it.togo.app.MainViewModel
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.usecase.ParseVoiceCommandUseCase
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.presentation.activelist.ActiveListViewModel
import it.togo.app.presentation.history.HistoryViewModel
import it.togo.app.presentation.rules.LearnedRulesViewModel
import it.togo.app.presentation.search.SearchViewModel
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import it.togo.app.presentation.voice.VoiceConfirmationViewModel
import it.togo.app.presentation.voice.VoiceResilienceViewModel
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

    // Epic 4 - Voice Confirmation (Story 4.4)
    viewModel { VoiceConfirmationViewModel(
        parseUseCase = get<ParseVoiceCommandUseCase>(),
    ) }

    // Epic 4 - Voice Resilience (Story 4.5)
    viewModel { VoiceResilienceViewModel(
        gateway = get<VoiceRecognitionGateway>(),
    ) }

    // Epic 5 - History (Story 5.1)
    viewModel { HistoryViewModel(
        historyRepository = get<it.togo.app.domain.repository.HistoryRepository>(),
        catalogRepository = get<CatalogRepository>(),
        shoppingListRepository = get<ShoppingListRepository>(),
    ) }

    // Epic 5 - Learned Rules (Story 5.2)
    viewModel { LearnedRulesViewModel(
        learnedRuleManager = get<it.togo.app.domain.usecase.LearnedRuleManager>(),
        catalogRepository = get<CatalogRepository>(),
        learnedRulesRepository = get<it.togo.app.domain.repository.LearnedRulesRepository>(),
    ) }

    // Future ViewModels will be added here (Epic 4, 5):
    // viewModel { VoiceInputViewModel(get(), get()) }
    // viewModel { ItemDetailViewModel(get(), get()) }
    // viewModel { HistoryViewModel(get(), get()) }
    // viewModel { LearnedRulesViewModel(get()) }
}