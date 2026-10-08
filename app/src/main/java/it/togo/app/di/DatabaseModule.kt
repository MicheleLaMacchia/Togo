package it.togo.app.di

import android.content.Context
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val databaseModule: Module = module {
    // Room Database - single instance
    single<Context> { androidContext() }
    // TODO(Story 1.3): Uncomment when TogoDatabase and DAOs are created
    // single { TogoDatabase.build(it) }
    
    // DAOs - factory (new instance per use, or single if preferred)
    // TODO(Story 1.3): Uncomment when DAOs exist
    // factory { get<TogoDatabase>().shoppingItemDao() }
    // factory { get<TogoDatabase>().catalogDao() }
    // factory { get<TogoDatabase>().historyDao() }
    // factory { get<TogoDatabase>().learnedRulesDao() }
}