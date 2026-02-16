package com.pedometr.app.di

import androidx.room.Room
import com.pedometr.app.data.StepDatabase
import com.pedometr.app.data.StepRepository
import com.pedometr.app.viewmodel.StepViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Moduł Koin dla warstwy danych
 */
val databaseModule = module {
    
    // Single instance bazy danych (Singleton)
    single {
        Room.databaseBuilder(
            androidContext(),
            StepDatabase::class.java,
            "step_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    // DAO z bazy danych
    single { get<StepDatabase>().stepDao() }
    
    // Repository jako Singleton
    single { StepRepository(get()) }
}

/**
 * Moduł Koin dla ViewModeli
 */
val viewModelModule = module {
    
    // ViewModel z automatycznym wstrzykiwaniem zależności
    viewModel { StepViewModel(get()) }
}

/**
 * Lista wszystkich modułów aplikacji
 */
val appModules = listOf(
    databaseModule,
    viewModelModule
)

