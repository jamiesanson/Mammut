package io.github.jamiesanson.mammut.dagger.application

import android.content.Context
import dagger.Module
import dagger.Provides
import io.github.jamiesanson.mammut.feature.themes.ThemeEngine
import io.github.jamiesanson.mammut.repo.PreferencesRepository

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
            ThemeEngine(preferencesRepository)
}