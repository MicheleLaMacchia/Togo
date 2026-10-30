package it.togo.app.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.dao.ListItemDao
import it.togo.app.data.database.dao.TaskCategoryDao
import it.togo.app.data.database.dao.TodoTaskDao
import it.togo.app.data.database.dao.UserAttributeDao

val databaseModule: Module = module {
    // Room Database - single instance
    single { TogoDatabase.build(androidContext()) }

    // DAOs - factory (new instance per use)
    // LIST_ITEM remains the single source for shopping rows and history.
    factory { get<TogoDatabase>().catalogDao() }
    factory { get<TogoDatabase>().listItemDao() }
    factory<TodoTaskDao> { get<TogoDatabase>().todoTaskDao() }
    factory<TaskCategoryDao> { get<TogoDatabase>().taskCategoryDao() }
    factory { get<TogoDatabase>().learnedRulesDao() }
    factory { get<TogoDatabase>().userAttributeDao() }
}
