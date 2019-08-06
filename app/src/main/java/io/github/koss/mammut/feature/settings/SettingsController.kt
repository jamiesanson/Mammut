package io.github.koss.mammut.feature.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import com.google.android.material.card.MaterialCardView
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.base.util.retained
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.themes.Theme
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.settings.dagger.SettingsModule
import io.github.koss.mammut.feature.settings.dagger.SettingsScope
import io.github.koss.mammut.feature.settings.model.*
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.card_theme.view.*
import kotlinx.android.synthetic.main.controller_settings.*
import kotlinx.android.synthetic.main.section_settings_footer.view.*
import kotlinx.android.synthetic.main.section_settings_header.view.*
import kotlinx.android.synthetic.main.section_toggleable_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick
import javax.inject.Inject

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class SettingsController : BaseController() {

    private lateinit var viewModel: SettingsViewModel

    @Inject
    @SettingsScope
    lateinit var viewModelFactory: MammutViewModelFactory

    @Inject
    @ApplicationScope
    lateinit var themeEngine: ThemeEngine

    private val settingsModule: SettingsModule by retained {
        SettingsModule()
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as AppCompatActivity).applicationComponent
                .plus(settingsModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context, viewModelFactory).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_settings, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)
        themeEngine.applyFontToCollapsingLayout(collapsingLayout)

        // Setup settings
        val adapter = FlexAdapter<SettingsItem>()
        settingsRecyclerView.adapter = adapter
        settingsRecyclerView.layoutManager = LinearLayoutManager(view!!.context, RecyclerView.VERTICAL, false)

        // Setup close button
        toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp)
        toolbar.navigationIcon?.setTint(view!!.colorAttr(R.attr.colorControlNormal))

        toolbar.setNavigationOnClickListener {
            router.popCurrentController()
        }

        registerSettingsItems(adapter)

        // Setup themes
        val themeAdapter = FlexAdapter<Theme>()
        themeRecyclerView.adapter = themeAdapter
        themeRecyclerView.layoutManager = LinearLayoutManager(view!!.context, RecyclerView.HORIZONTAL, false)

        setupThemesAdapter(themeAdapter)

        // Scroll to selected theme
        themeRecyclerView.scrollToPosition(themeEngine.allThemes.indexOf(themeEngine.currentTheme))

        viewModel.settingsItems.observe(this) {
            adapter.resetItems(it)
        }

        viewModel.restartApp.observe(this) {
            it.getContentIfNotHandled()?.run {
                activity?.recreate()
            }
        }
    }

    private fun registerSettingsItems(adapter: FlexAdapter<SettingsItem>) {
        // Headers
        adapter.register<SectionHeader>(layout = R.layout.section_settings_header) { sectionHeader, view, _ ->
            with(view) {
                titleTextView.setText(sectionHeader.titleRes)
            }
        }

        // Clickable items
        adapter.register<ClickableItem>(layout = R.layout.section_clickable_item) { clickableItem, view, _ ->
            with(view) {
                titleTextView.setText(clickableItem.titleRes)

                onClick {
                    // Perform action
                    viewModel.performAction(clickableItem.action)
                }
            }
        }

        // Toggleable items
        adapter.register<ToggleableItem>(layout = R.layout.section_toggleable_item) { toggleableItem, view, _ ->
            with(view) {
                toggleableTitleTextView.setText(toggleableItem.titleRes)
                if (toggleableItem.subtitleRes == 0) {
                    toggleableSubtitleTextView.isVisible = false
                } else {
                    toggleableSubtitleTextView.isVisible = true
                    toggleableSubtitleTextView.setText(toggleableItem.subtitleRes)
                }

                toggleableSwitch.setOnCheckedChangeListener(null)

                if (toggleableSwitch.isChecked != toggleableItem.isSet) {
                    toggleableSwitch.isChecked = toggleableItem.isSet
                }

                onClick {
                    toggleableSwitch.toggle()
                }

                toggleableSwitch.onCheckedChange { _, isChecked ->
                    if (isChecked != toggleableItem.isSet) {
                        launch {
                            delay(250)
                            withContext(Dispatchers.Main) {
                                viewModel.performAction(toggleableItem.action)
                            }
                        }
                    }
                }
            }
        }

        // Footer
        adapter.register<SettingsFooter>(layout = R.layout.section_settings_footer) { settingsFooter, view, _ ->
            with(view) {
                @SuppressLint("SetTextI18n")
                buildVersionTextView.text = "Mammut build ${settingsFooter.appVersion}"
            }
        }
    }

    private fun setupThemesAdapter(adapter: FlexAdapter<Theme>) {
        adapter.register<Theme>(layout = R.layout.card_theme) { theme, view, _ ->
            val resolvedTheme = view.context.theme.apply { applyStyle(theme.styleRes, true) }

            view.cardView.accentColorView.setBackgroundColor(resolvedTheme.color(R.attr.colorAccent))
            view.cardView.setCardBackgroundColor(resolvedTheme.color(R.attr.colorPrimary))

            view.themeNameTextView.text = theme.themeName

            (view as MaterialCardView).apply {
                strokeColor = if (theme == themeEngine.currentTheme) colorAttr(R.attr.colorAccent) else ContextCompat.getColor(context, android.R.color.transparent)
                onClick {
                    viewModel.onThemeChanged(theme)
                }
            }

            view.context.theme.apply { applyStyle(themeEngine.currentTheme.styleRes, true) }
        }

        adapter.resetItems(themeEngine.allThemes)
    }

}