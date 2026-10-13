package it.togo.app.di

import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {
    // Domain UseCases - Epic 3 (Catalogo & Risoluzione)
    single { CategoryAssigner(get(), get()) }
    single { CategoryCorrector(get()) }
    single { AssignCanonicalCategoryUseCase(get()) }
    single { CorrectCategoryUseCase(get(), get()) }
    single { LearnedRuleManager(get()) }

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