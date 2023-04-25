package io.github.koss.mammut.toot

import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.sys1yagi.mastodon4j.api.entity.Emoji
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.data.extensions.fullAcct
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.toot.dagger.ComposeTootComponent
import io.github.koss.mammut.toot.dagger.ComposeTootModule
import io.github.koss.mammut.toot.dagger.ComposeTootScope
import io.github.koss.mammut.toot.databinding.ComposeTootFragmentBinding
import io.github.koss.mammut.toot.emoji.EmojiAdapter
import io.github.koss.mammut.toot.model.SubmissionState
import io.github.koss.mammut.toot.model.TootModel
import io.github.koss.mammut.toot.model.iconRes
import io.github.koss.mammut.toot.view.update
import javax.inject.Inject

const val MAX_TOOT_LENGTH = 500

class ComposeTootFragment : Fragment(R.layout.compose_toot_fragment) {

    private val binding by viewLifecycleLazy { ComposeTootFragmentBinding.bind(requireView()) }

    private lateinit var viewModel: ComposeTootViewModel

    @Inject
    @ComposeTootScope
    lateinit var factory: MammutViewModelFactory

    private val args by navArgs<ComposeTootFragmentArgs>()

    /**
     * List of bottom menu items and their content views
     */
    private val bottomMenuItems: List<Pair<ImageView, View>>
        get() = listOf(
            binding.insertEmojiButton to binding.emojiListRecyclerView,
            binding.privacyButton to binding.privacyLayout
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findSubcomponentFactory()
            .buildSubcomponent<ComposeTootModule, ComposeTootComponent>(
                ComposeTootModule(
                    accessToken = args.accessToken,
                    instanceName = args.instanceName
                )
            )
            .inject(this)

        viewModel = ViewModelProvider(context as FragmentActivity, factory)[args.accessToken, ComposeTootViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupInputEditText()
        setupTootButton()
        setupEmojis()
        setupBottomMenu()
        setupPrivacySelector()
        setupContentWarnings()
        setupProfileCell(args.account)

        binding.appBarLayout.doOnApplyWindowInsets { layout, insets, _ ->
            layout.updatePadding(top = insets.systemWindowInsetTop)

            if (insets.systemWindowInsetBottom != 0) {
                binding.bottomContentLayout.updatePadding(bottom = insets.systemWindowInsetBottom)
            }
        }

        viewModel.initialise(null, textHeight = binding.inputEditText.lineHeight)

        viewModel.model.observe(viewLifecycleOwner, ::onModelChanged)
        viewModel.submissionState.observe(viewLifecycleOwner, ::onSubmissionStateChanged)
        viewModel.availableEmojis.observe(viewLifecycleOwner, ::onEmojisRetrieved)
        viewModel.renderedStatus.observe(viewLifecycleOwner, ::onInputTextChanged)
        viewModel.renderedContentWarning.observe(viewLifecycleOwner, ::onContentWarningChanged)
    }

    private fun onModelChanged(model: TootModel?) {
        model ?: return

        binding.privacyLayout.selectedVisibility = model.visibility
        binding.privacyButton.setImageResource(model.visibility.iconRes)

        if (model.spoilerText != null && !binding.contentWarningLayout.isVisible) {
            // Show content warning if it's in the model but not user visible. Only makes sense if
            // showing for the first time, i.e initialised from a draft.
            binding.contentWarningButton.isSelected = true
            binding.contentWarningLayout.isVisible = true
        }
    }

    private fun onInputTextChanged(inputText: Spannable) =
        binding.inputEditText.update(inputText)

    private fun onContentWarningChanged(contentWarning: Spannable) =
        binding.contentWarningEditText.update(contentWarning)

    private fun onSubmissionStateChanged(submissionState: SubmissionState?) {
        submissionState ?: return

        TransitionManager.beginDelayedTransition(view as ViewGroup)
        binding.submissionLoadingLayout.isVisible = submissionState.isSubmitting

        when {
            submissionState.hasSubmitted -> {
                // Close and toast
                Toast.makeText(requireContext(), R.string.toot_posted, Toast.LENGTH_SHORT).show()

                close()
            }

            submissionState.error != null -> {
                // Show error
                Snackbar.make(requireView(), submissionState.error, Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun onEmojisRetrieved(emojis: List<Emoji>?) {
        (binding.emojiListRecyclerView.adapter as? EmojiAdapter)?.submitList(emojis)
    }

    private fun setupTootButton() {
        updateTootButton()
        binding.tootButton.setOnClickListener {
            viewModel.onSendTootClicked()
        }
    }

    private fun setupInputEditText() {
        binding.inputEditText.doAfterTextChanged { text ->
            val length = text?.length ?: 0

            // Ensure counter is displayed at 90% of the MAX_TOOT_LENGTH
            binding.inputTextInputLayout.isCounterEnabled =
                length.toFloat() / MAX_TOOT_LENGTH.toFloat() >= 0.9

            updateTootButton()
            viewModel.onStatusChanged(text?.toString() ?: "")
        }
    }

    private fun setupToolbar() {
        if (binding.toolbar.menu.isNotEmpty()) return

        // Inflate delete and send items
        val colorControlNormal = binding.toolbar.context.colorAttr(R.attr.colorOnSurface)
        binding.toolbar.inflateMenu(R.menu.menu_compose)
        binding.toolbar.menu.children
            .forEach {
                it.icon?.setTint(colorControlNormal)
                it.icon?.setTintMode(PorterDuff.Mode.SRC_IN)
            }
        binding.toolbar.setOnMenuItemClickListener(::onMenuItemClicked)

        // Set up back button
        val navigationIcon = binding.toolbar.context
            .getDrawable(R.drawable.ic_close_black_24dp)?.apply {
                setTint(colorControlNormal)
                setTintMode(PorterDuff.Mode.SRC_IN)
            }
        binding.toolbar.navigationIcon = navigationIcon
        binding.toolbar.setNavigationOnClickListener {
            close()
        }
    }

    private fun setupEmojis() {
        binding.emojiListRecyclerView.adapter = EmojiAdapter {
            val contentWarningFocused = binding.contentWarningEditText.hasFocus()
            viewModel.onEmojiAdded(
                emoji = it,
                index = (if (contentWarningFocused) binding.contentWarningEditText else binding.inputEditText).selectionStart,
                isContentWarningFocussed = contentWarningFocused
            )
        }
        binding.emojiListRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 3, RecyclerView.HORIZONTAL, false)
    }

    private fun setupPrivacySelector() {
        binding.privacyLayout.setOnVisibilityChangedListener {
            viewModel.onVisibilityChanged(it)
        }
    }

    private fun setupContentWarnings() {
        binding.contentWarningButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(view as ViewGroup, AutoTransition().apply {
                duration = 200L
            })

            // Toggle visibility
            binding.contentWarningButton.isSelected = !binding.contentWarningButton.isSelected
            binding.contentWarningLayout.isVisible = !binding.contentWarningLayout.isVisible

            // If no longer visible, clear the text and focus the inputEditText
            if (!binding.contentWarningLayout.isVisible) {
                viewModel.onContentWarningChanged(null)
                binding.inputEditText.requestFocus()
            } else {
                binding.contentWarningEditText.setText("")
                binding.contentWarningEditText.requestFocus()
            }
        }
        binding.contentWarningEditText.doAfterTextChanged { text ->
            if (binding.contentWarningLayout.isVisible) {
                viewModel.onContentWarningChanged(text?.toString())
            }
        }
    }

    private fun setupBottomMenu() {
        // Setup click listeners
        bottomMenuItems.forEach { (button, content) ->
            button.setOnClickListener { displayContentForImageView(button, content) }
        }
    }

    private fun setupProfileCell(account: Account) {
        @ColorInt val placeholderColor = requireContext().colorAttr(R.attr.colorPrimaryLight)

        Glide.with(requireView())
            .load(account.avatar)
            .thumbnail(
                Glide.with(requireView())
                    .load(ColorDrawable(placeholderColor))
                    .apply(RequestOptions.circleCropTransform())
            )
            .apply(RequestOptions.circleCropTransform())
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.profileImageView)

        binding.displayNameTextView.text = account.displayName
        binding.usernameTextView.text = account.fullAcct(args.instanceName)
    }

    private fun displayContentForImageView(imageViewClicked: ImageView, contentView: View) {
        TransitionManager.beginDelayedTransition(view as ViewGroup, AutoTransition().apply {
            duration = 200L
        })

        // Make every other content view invisible then toggle the visibility of this one
        bottomMenuItems
            .filterNot { it.first == imageViewClicked }
            .forEach {
                it.first.isSelected = false
                it.second.isVisible = false
            }

        contentView.isVisible = !contentView.isVisible
        imageViewClicked.isSelected = !imageViewClicked.isSelected
    }

    private fun updateTootButton() {
        binding.tootButton.isEnabled = binding.inputEditText.length() > 0
    }

    private fun onMenuItemClicked(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.delete_item -> {
                if (viewModel.hasBeenModified) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(R.string.start_toot_again)
                        .setPositiveButton(R.string.start_again) { _, _ -> viewModel.deleteTootContents() }
                        .setNegativeButton(R.string.cancel) { _, _ -> }
                        .show()
                }
                true
            }

            else -> false
        }

    private fun close() {
        findNavController().popBackStack()
    }
}