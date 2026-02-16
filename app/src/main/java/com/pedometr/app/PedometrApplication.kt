package com.pedometr.app

import android.app.Application
import com.pedometr.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PedometrApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicjalizacja Koin
        startKoin {
            // Logger Koin (tylko w debug)
            androidLogger(Level.ERROR)
            
            // Context aplikacji
            androidContext(this@PedometrApplication)
            
            // Moduły DI
            modules(appModules)
        }
    }
}
