package io.github.koss.mammut.feature.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.RouterTransaction
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.koss.mammut.R
import io.github.koss.mammut.component.retention.retained
import io.github.koss.mammut.dagger.MammutViewModelFactory
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.extension.observe
import io.github.koss.mammut.extension.startActivity
import io.github.koss.mammut.feature.instance.InstanceActivity
import io.github.koss.mammut.feature.instance.subfeature.navigation.BaseController
import io.github.koss.mammut.feature.instancebrowser.InstanceBrowserActivity
import io.github.koss.mammut.feature.settings.dagger.SettingsModule
import io.github.koss.mammut.feature.settings.dagger.SettingsScope
import io.github.koss.mammut.feature.settings.model.*
import io.github.koss.mammut.feature.themes.ThemeEngine
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_settings.*
import kotlinx.android.synthetic.main.section_settings_footer.view.*
import kotlinx.android.synthetic.main.section_settings_header.view.*
import kotlinx.android.synthetic.main.section_toggleable_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class SettingsController: BaseController() {

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
        (context as InstanceActivity)
                .component
                .plus(settingsModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context, viewModelFactory).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_settings, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)
        themeEngine.applyFontToCollapsingLayout(collapsingLayout)

        val adapter = FlexAdapter<SettingsItem>()
        settingsRecyclerView.adapter = adapter
        settingsRecyclerView.layoutManager = LinearLayoutManager(view!!.context, RecyclerView.VERTICAL, false)

        registerSettingsItems(adapter)

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
            with (view) {
                titleTextView.setText(sectionHeader.titleRes)
            }
        }

        // Clickable items
        adapter.register<ClickableItem>(layout = R.layout.section_clickable_item) { clickableItem, view, _ ->
            with (view) {
                titleTextView.setText(clickableItem.titleRes)

                onClick {
                    // Perform action
                    when (clickableItem.action) {
                        is NavigationAction -> router.pushController(RouterTransaction.with(clickableItem.action.controllerToPush()))
                        is ViewOssLicenses -> startActivity<OssLicensesMenuActivity>()
                        is ChangeInstance -> startActivity<InstanceBrowserActivity>(finishCurrent = true)
                        is LogOut -> viewModel.performAction(clickableItem.action).also {
                            startActivity<InstanceBrowserActivity>(finishCurrent = true)
                        }
                        else -> viewModel.performAction(clickableItem.action)
                    }
                }
            }
        }

        // Toggleable items
        adapter.register<ToggleableItem>(layout = R.layout.section_toggleable_item) { toggleableItem, view, _ ->
            with (view) {
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
            with (view) {
                @SuppressLint("SetTextI18n")
                buildVersionTextView.text = "Mammut build ${settingsFooter.appVersion}"
            }
        }
    }
}