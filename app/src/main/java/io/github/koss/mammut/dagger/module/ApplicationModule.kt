package io.github.koss.mammut.dagger.module

import android.content.Context
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.base.themes.ThemeConfig
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.database.MammutDatabaseInitialiser
import io.github.koss.mammut.repo.PreferencesRepository
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
            ThemeEngine(object: ThemeConfig {
                override var currentThemeId: String?
                    get() = preferencesRepository.themeId
                    set(value) {
                        preferencesRepository.themeId = value
                    }
            })

    @Provides
    @ApplicationScope
    fun provideMammutDatabase(context: Context): MammutDatabase =
            MammutDatabaseInitialiser.initialise(context)

    @Provides
    @ApplicationScope
    fun provideNetworkIndicator(context: Context): NetworkIndicator =
            NetworkIndicator(context)
}