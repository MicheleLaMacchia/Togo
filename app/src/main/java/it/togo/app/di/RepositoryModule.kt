package it.togo.app.di

import org.koin.core.module.Module
import org.koin.dsl.module

val repositoryModule: Module = module {
    // Repository implementations will be added in Story 1.3 (Database Room e Schema Entità Core)
    // They depend on DAOs from databaseModule and implement interfaces from domainModule
    
    // Example (to be uncommented/implemented in Story 1.3):
    // single<ShoppingListRepository> { RoomShoppingListRepository(get(), get()) }
    // single<CatalogRepository> { RoomCatalogRepository(get()) }
    // single<HistoryRepository> { RoomHistoryRepository(get()) }
    // single<LearnedRulesRepository> { RoomLearnedRulesRepository(get()) }
}