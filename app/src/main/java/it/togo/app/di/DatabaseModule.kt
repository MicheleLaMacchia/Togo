package it.togo.app.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.dao.ShoppingItemDao

val databaseModule: Module = module {
    // Room Database - single instance
    single<Context> { androidContext() }
    single { TogoDatabase.build(it) }

    // DAOs - factory (new instance per use)
    factory { get<TogoDatabase>().shoppingItemDao() }
    factory { get<TogoDatabase>().catalogDao() }
    factory { get<TogoDatabase>().historyDao() }
    factory { get<TogoDatabase>().learnedRulesDao() }
}