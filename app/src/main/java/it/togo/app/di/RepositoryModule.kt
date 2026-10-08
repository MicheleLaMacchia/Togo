package it.togo.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import it.togo.app.data.repository.RoomShoppingListRepository
import it.togo.app.data.repository.RoomHistoryRepository
import it.togo.app.data.repository.RoomLearnedRulesRepository
import it.togo.app.data.repository.RoomCatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.HistoryRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.repository.CatalogRepository

val repositoryModule: Module = module {
    // Repository implementations - single instance per repository
    single<ShoppingListRepository> { RoomShoppingListRepository(get(), get()) }
    single<CatalogRepository> { RoomCatalogRepository(get()) }
    single<HistoryRepository> { RoomHistoryRepository(get()) }
    single<LearnedRulesRepository> { RoomLearnedRulesRepository(get()) }
}