package io.github.koss.mammut.dagger.module

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.base.themes.ThemeConfig
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.database.MammutDatabaseInitialiser
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.feed.ui.view.NetworkIndicator

@Module
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

                override val darkModeFollowSystem: Boolean
                    get() = if (Build.VERSION.SDK_INT >= 29) preferencesRepository.darkModeFollowSystem else false

                override val darkModeEnabled: Boolean
                    get() = preferencesRepository.darkModeOverrideEnabled
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