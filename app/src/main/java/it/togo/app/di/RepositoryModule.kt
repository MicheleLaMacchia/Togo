package it.togo.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import it.togo.app.data.repository.ListItemHistoryRepositoryAdapter
import it.togo.app.data.repository.RoomListItemRepository
import it.togo.app.data.repository.RoomTodoRepository
import it.togo.app.data.repository.RoomShoppingListRepository
import it.togo.app.data.repository.RoomLearnedRulesRepository
import it.togo.app.data.repository.RoomCatalogRepository
import it.togo.app.data.repository.RoomUserAttributeRepository
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.domain.repository.TodoRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.HistoryRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.UserAttributeRepository

val repositoryModule: Module = module {
    // Repository implementations - single instance per repository
    single<ListItemRepository> { RoomListItemRepository(get(), get(), get()) }
    single<TodoRepository> { RoomTodoRepository(get(), get(), get(), get()) }
    single<ShoppingListRepository> { RoomShoppingListRepository(get()) }
    single<CatalogRepository> { RoomCatalogRepository(get()) }
    single<HistoryRepository> { ListItemHistoryRepositoryAdapter(get()) }
    single<LearnedRulesRepository> { RoomLearnedRulesRepository(get()) }
    single<UserAttributeRepository> { RoomUserAttributeRepository(get()) }
}
