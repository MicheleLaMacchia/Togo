package it.togo.app.di

import it.togo.app.data.voice.AndroidVoiceRecognitionGateway
import it.togo.app.domain.category.CategoryAssigner
import it.togo.app.domain.category.CategoryCorrector
import it.togo.app.domain.parser.VoiceCommandParser
import it.togo.app.domain.usecase.AssignCanonicalCategoryUseCase
import it.togo.app.domain.usecase.CorrectCategoryUseCase
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.LearnedRuleManager
import it.togo.app.domain.usecase.ParseVoiceCommandUseCase
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.wizard.CategorySelectionWizard
import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {
    // Domain UseCases - Epic 3 (Catalogo & Risoluzione)
    single { CategoryAssigner(get(), get()) }
    single { CategoryCorrector(get()) }
    single { AssignCanonicalCategoryUseCase(get()) }
    single { CorrectCategoryUseCase(get(), get()) }
    single { LearnedRuleManager(get()) }

    // Domain Wizard (Story 3.7) - Categorizzazione prodotto sconosciuto
    single { CategorySelectionWizard(get()) }
    single { CreateCanonicalProductUseCase(get()) }

    // Voice Recognition (Story 4.1) - Gateway disaccoppiato da Activity
    single<VoiceRecognitionGateway> { AndroidVoiceRecognitionGateway() }

    // Voice Parsing (Story 4.3) - Parser comandi vocali
    single { VoiceCommandParser() }
    single { ParseVoiceCommandUseCase(get()) }

    // Domain UseCases - Epic 2 (Lista Attiva) - placeholder
    // factory { AddShoppingItemUseCase(get()) }
    // factory { CheckOffItemUseCase(get()) }
    // factory { CheckoutShoppingListUseCase(get()) }
    // factory { ShareActiveListUseCase(get()) }
    // factory { DetectDuplicateUseCase(get()) }
    // factory { ResolveDuplicateUseCase(get()) }

    // Domain services (pure Kotlin, no Android dependencies)
    // single { VoiceCommandParser() }
    // single { UnitNormalizer() }
}