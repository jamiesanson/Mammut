package io.github.koss.mammut.toot

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Spannable
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.view.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.sys1yagi.mastodon4j.api.entity.Emoji
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.toot.dagger.*
import io.github.koss.mammut.toot.emoji.EmojiAdapter
import io.github.koss.mammut.toot.model.SubmissionState
import io.github.koss.mammut.toot.model.TootModel
import io.github.koss.mammut.toot.model.iconRes
import io.github.koss.mammut.toot.view.update
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.compose_toot_controller.*
import kotlinx.android.synthetic.main.layout_privacy.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.textChangedListener
import java.lang.IllegalStateException
import javax.inject.Inject

const val MAX_TOOT_LENGTH = 500

/**
 * This is the main controller for composing a toot. All things related to how this controller operates
 * can be found in the Toot module.
 */
@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class ComposeTootController: BaseController() {

    private lateinit var viewModel: ComposeTootViewModel

    @Inject
    @ComposeTootScope
    lateinit var factory: MammutViewModelFactory

    /**
     * List of bottom menu items and their content views
     */
    private val bottomMenuItems: List<Pair<ImageView, View>>?
            get() = view?.run {
                listOf(
                    insertEmojiButton to emojiListRecyclerView,
                    privacyButton to privacyLayout
                )
            }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as? SubcomponentFactory)
                ?.buildSubcomponent<ComposeTootModule, ComposeTootComponent>(ComposeTootModule())
                ?.inject(this) ?: throw IllegalStateException("Context must be subcomponent factory")

        viewModel = ViewModelProviders
                .of(context as FragmentActivity, factory)[ComposeTootViewModel::class.java]
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        // Hide Keyboard
        activity?.findViewById<View>(android.R.id.content)?.run {
            activity?.getSystemService<InputMethodManager>()
                    ?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.compose_toot_controller, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)
        setupToolbar()
        setupTextChangedListeners()
        setupTootButton()
        setupEmojis()
        setupBottomMenu()
        setupPrivacySelector()
        setupContentWarnings()

        viewModel.initialise(null, textHeight = inputEditText.lineHeight)

        viewModel.model.observe(this, Observer(::onModelChanged))
        viewModel.submissionState.observe(this, Observer(::onSubmissionStateChanged))
        viewModel.availableEmojis.observe(this, Observer(::onEmojisRetrieved))
        viewModel.renderedStatus.observe(this, Observer(::onInputTextChanged))
        viewModel.renderedContentWarning.observe(this, Observer(::onContentWarningChanged))
    }

    private fun onModelChanged(model: TootModel?) {
        model ?: return

        privacyLayout.selectedVisibility = model.visibility
        privacyButton.setImageResource(model.visibility.iconRes)

        if (model.spoilerText != null && !contentWarningLayout.isVisible) {
            // Show content warning if it's in the model but not user visible. Only makes sense if
            // showing for the first time, i.e initialised from a draft.
            contentWarningButton.isSelected = true
            contentWarningLayout.isVisible = true
        }
    }

    private fun onInputTextChanged(inputText: Spannable) =
            inputEditText.update(inputText)

    private fun onContentWarningChanged(contentWarning: Spannable) =
            contentWarningEditText.update(contentWarning)
    
    private fun onSubmissionStateChanged(submissionState: SubmissionState?) {
        submissionState ?: return

        TransitionManager.beginDelayedTransition(view as ViewGroup)
        submissionLoadingLayout.isVisible = submissionState.isSubmitting
        
        when {
            submissionState.hasSubmitted -> {
                // Close and toast
                view?.let {
                    Toast.makeText(it.context, R.string.toot_posted, Toast.LENGTH_SHORT).show()
                }
                
                close()
            }
            submissionState.error != null -> {
                // Show error
                view?.let {
                    Snackbar.make(it, submissionState.error, Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    private fun onEmojisRetrieved(emojis: List<Emoji>?) {
        (emojiListRecyclerView.adapter as? EmojiAdapter)?.submitList(emojis)
    }

    private fun setupTootButton() {
        updateTootButton()
        tootButton.onClick {
            viewModel.onSendTootClicked()
        }
    }

    private fun setupTextChangedListeners() {
        remainingCharactersTextView.text = (MAX_TOOT_LENGTH - inputEditText.length()).toString()

        inputEditText.textChangedListener {
            afterTextChanged { text ->
                val length = text?.length ?: 0
                remainingCharactersTextView.text = "${MAX_TOOT_LENGTH - length}"
                updateTootButton()
                viewModel.onStatusChanged(text?.toString() ?: "")
            }
        }
    }

    private fun setupToolbar() {
        if (toolbar.menu.isNotEmpty()) return

        // Inflate delete and send items
        val colorControlNormal = toolbar.context.colorAttr(R.attr.colorControlNormal)
        toolbar.inflateMenu(R.menu.menu_compose)
        toolbar.menu.children
                .forEach {
                    it.icon.setTint(colorControlNormal)
                    it.icon.setTintMode(PorterDuff.Mode.SRC_IN)
                }
        toolbar.setOnMenuItemClickListener(::onMenuItemClicked)

        // Set up back button
        val navigationIcon = toolbar.context
                .getDrawable(R.drawable.ic_close_black_24dp)?.apply {
                    setTint(colorControlNormal)
                    setTintMode(PorterDuff.Mode.SRC_IN)
                }
        toolbar.navigationIcon = navigationIcon
        toolbar.setNavigationOnClickListener {
            close()
        }
    }

    private fun setupEmojis() {
        emojiListRecyclerView.adapter = EmojiAdapter {
            val contentWarningFocused = contentWarningEditText.hasFocus()
            viewModel.onEmojiAdded(
                    emoji = it,
                    index = (if (contentWarningFocused) contentWarningEditText else inputEditText).selectionStart,
                    isContentWarningFocussed = contentWarningFocused)
        }
        emojiListRecyclerView.layoutManager = GridLayoutManager(view!!.context, 3, RecyclerView.HORIZONTAL, false)
    }

    private fun setupPrivacySelector() {
        privacyLayout.setOnVisibilityChangedListener {
            viewModel.onVisibilityChanged(it)
        }
    }

    private fun setupContentWarnings() {
        contentWarningButton.onClick {
            TransitionManager.beginDelayedTransition(view as ViewGroup, AutoTransition().apply {
                duration = 200L
            })
            
            // Toggle visibility
            contentWarningButton.isSelected = !contentWarningButton.isSelected
            contentWarningLayout.isVisible = !contentWarningLayout.isVisible

            // If no longer visible, clear the text and focus the inputEditText
            if (!contentWarningLayout.isVisible) {
                viewModel.onContentWarningChanged(null)
                inputEditText.requestFocus()
            } else {
                contentWarningEditText.setText("")
                contentWarningEditText.requestFocus()
            }
        }
        contentWarningEditText.textChangedListener {
            afterTextChanged { text ->
                if (contentWarningLayout.isVisible) {
                    viewModel.onContentWarningChanged(text?.toString())
                }
            }
        }
    }

    private fun setupBottomMenu() {
        // Setup click listeners
        bottomMenuItems?.forEach { (button, content) ->
            button.onClick { displayContentForImageView(button, content) }
        }
    }

    private fun displayContentForImageView(imageViewClicked: ImageView, contentView: View) {
        TransitionManager.beginDelayedTransition(view as ViewGroup, AutoTransition().apply {
            duration = 200L
        })

        // Make every other content view invisible then toggle the visibility of this one
        bottomMenuItems
                ?.filterNot { it.first == imageViewClicked }
                ?.forEach {
                    it.first.isSelected = false
                    it.second.isVisible = false
                }

        contentView.isVisible = !contentView.isVisible
        imageViewClicked.isSelected = !imageViewClicked.isSelected
    }

    private fun updateTootButton() {
        tootButton.isEnabled = inputEditText.length() > 0
    }

    private fun onMenuItemClicked(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                R.id.delete_item -> {
                    if (viewModel.hasBeenModified) {
                        AlertDialog.Builder(view?.context!!)
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
        router.popCurrentController()
    }
}