package io.github.koss.mammut.feature.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import com.google.android.material.card.MaterialCardView
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.R
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.themes.Theme
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.retained
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.databinding.*
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.settings.dagger.SettingsComponent
import io.github.koss.mammut.feature.settings.dagger.SettingsModule
import io.github.koss.mammut.feature.settings.dagger.SettingsScope
import io.github.koss.mammut.feature.settings.model.ClickableItem
import io.github.koss.mammut.feature.settings.model.SectionHeader
import io.github.koss.mammut.feature.settings.model.SettingsItem
import io.github.koss.mammut.feature.settings.model.ToggleableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsFragment: Fragment(R.layout.settings_fragment) {

    private val binding by viewLifecycleLazy { SettingsFragmentBinding.bind(requireView()) }

    private lateinit var viewModel: SettingsViewModel

    @Inject
    @SettingsScope
    lateinit var viewModelFactory: MammutViewModelFactory

    @Inject
    @ApplicationScope
    lateinit var themeEngine: ThemeEngine

    private val component: SettingsComponent by retained {
        requireActivity().applicationComponent
                .plus(SettingsModule())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[SettingsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        themeEngine.applyFontToCollapsingLayout(binding.collapsingLayout)

        // Setup settings
        val adapter = FlexAdapter<SettingsItem>()
        binding.settingsRecyclerView.adapter = adapter
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        // Setup close button
        binding.toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp)
        binding.toolbar.navigationIcon?.setTint(requireContext().colorAttr(R.attr.colorOnSurface))

        // Setup insets
        binding.collapsingLayout.doOnApplyWindowInsets { layout, insets, _ ->
            layout.updatePadding(top = insets.systemWindowInsetTop)
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        registerSettingsItems(adapter)

        // Setup themes
        val themeAdapter = FlexAdapter<Theme>()
        binding.themeRecyclerView.adapter = themeAdapter
        binding.themeRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)

        setupThemesAdapter(themeAdapter)

        // Scroll to selected theme
        binding.themeRecyclerView.scrollToPosition(themeEngine.allThemes.indexOf(themeEngine.currentTheme))

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
            with(SectionSettingsHeaderBinding.bind(view)) {
                titleTextView.setText(sectionHeader.titleRes)
            }
        }

        // Clickable items
        adapter.register<ClickableItem>(layout = R.layout.section_clickable_item) { clickableItem, view, _ ->
            with(SectionClickableItemBinding.bind(view)) {
                titleTextView.setText(clickableItem.titleRes)

                root.setOnClickListener {
                    // Perform action
                    viewModel.performAction(clickableItem.action)
                }
            }
        }

        // Toggleable items
        adapter.register<ToggleableItem>(layout = R.layout.section_toggleable_item) { toggleableItem, view, _ ->
            with(SectionToggleableItemBinding.bind(view)) {
                root.isEnabled = toggleableItem.isEnabled

                toggleableTitleTextView.setText(toggleableItem.titleRes)
                toggleableTitleTextView.isEnabled = toggleableItem.isEnabled

                if (toggleableItem.subtitleRes == 0) {
                    toggleableSubtitleTextView.isVisible = false
                } else {
                    toggleableSubtitleTextView.isVisible = true
                    toggleableSubtitleTextView.setText(toggleableItem.subtitleRes)
                    toggleableSubtitleTextView.isEnabled = toggleableItem.isEnabled
                }

                toggleableSwitch.setOnCheckedChangeListener(null)
                toggleableSwitch.isEnabled = toggleableItem.isEnabled

                if (toggleableSwitch.isChecked != toggleableItem.isSet) {
                    toggleableSwitch.isChecked = toggleableItem.isSet
                }

                root.setOnClickListener {
                    toggleableSwitch.toggle()
                }

                toggleableSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != toggleableItem.isSet) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(250)
                            withContext(Dispatchers.Main) {
                                viewModel.performAction(toggleableItem.action)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupThemesAdapter(adapter: FlexAdapter<Theme>) {
        adapter.register<Theme>(layout = R.layout.card_theme) { theme, view, _ ->
            val resolvedTheme = view.context.theme.apply { applyStyle(theme.styleRes, true) }

            val binding = CardThemeBinding.bind(view)

            binding.cardView.setCardBackgroundColor(view.context.colorAttr(R.attr.colorAccent, resolvedTheme))

            binding.themeNameTextView.text = theme.themeName

            (view as MaterialCardView).apply {
                strokeColor = if (theme == themeEngine.currentTheme) requireContext().colorAttr(R.attr.colorAccent) else ContextCompat.getColor(context, android.R.color.transparent)
                setOnClickListener {
                    viewModel.onThemeChanged(theme)
                }
            }

            view.context.theme.apply { applyStyle(themeEngine.currentTheme.styleRes, true) }
        }

        adapter.resetItems(themeEngine.allThemes)
    }

}