package it.togo.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import it.togo.app.di.databaseModule
import it.togo.app.di.domainModule
import it.togo.app.di.presentationModule
import it.togo.app.di.repositoryModule

class TogoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TogoApplication)
            modules(domainModule, databaseModule, repositoryModule, presentationModule)
        }
    }
}