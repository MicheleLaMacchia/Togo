package it.togo.app.di

import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {
    // Domain UseCases will be added here in future stories (Epic 2, 3, 4, 5)
    // Example:
    // single { ProcessItemInputUseCase(get()) }
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