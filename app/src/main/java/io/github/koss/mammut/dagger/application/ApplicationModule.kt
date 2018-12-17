package io.github.koss.mammut.dagger.application

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.feature.themes.ThemeEngine
import io.github.koss.mammut.data.repo.PreferencesRepository
import io.github.koss.mammut.feature.network.NetworkIndicator

@Module(includes = [ ApplicationViewModelModule::class ])
class ApplicationModule(private val appContext: Context) {


    @Provides
    @ApplicationScope
    fun provideApplicationContext(): Context = appContext

    @Provides
    @ApplicationScope
    fun providePreferencesRepository(context: Context): PreferencesRepository =
            PreferencesRepository(context)

    @Provides
    @ApplicationScope
    fun provideThemeEngine(preferencesRepository: PreferencesRepository): ThemeEngine =
            ThemeEngine(preferencesRepository)

    @Provides
    @ApplicationScope
    fun provideMammutDatabase(context: Context): MammutDatabase =
            Room.databaseBuilder(context, MammutDatabase::class.java, "mammut-db").build()

    @Provides
    @ApplicationScope
    fun provideNetworkIndicator(context: Context): NetworkIndicator =
            NetworkIndicator(context)
}