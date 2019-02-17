package io.github.koss.mammut.toot

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.toot.dagger.*
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.compose_toot_controller.*
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.sdk27.coroutines.textChangedListener
import java.lang.IllegalStateException
import javax.inject.Inject

private const val MAX_TOOT_LENGTH = 500

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

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as? SubcomponentFactory)
                ?.buildSubcomponent<ComposeTootModule, ComposeTootComponent>(ComposeTootModule())
                ?.inject(this) ?: throw IllegalStateException("Context must be subcomponent factory")

        viewModel = ViewModelProviders
                .of(context as FragmentActivity, factory)[ComposeTootViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.compose_toot_controller, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)
        setupToolbar()
        setupRemainingCharacters()
    }

    private fun setupRemainingCharacters() {
        remainingCharactersTextView.text = (MAX_TOOT_LENGTH - inputEditText.length()).toString()

        inputEditText.textChangedListener {
            afterTextChanged { text ->
                val length = text?.length ?: 0
                remainingCharactersTextView.text = "${MAX_TOOT_LENGTH - length}"
            }
        }
    }

    private fun setupToolbar() {
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
            router.popCurrentController()
        }
    }

    private fun onMenuItemClicked(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                R.id.delete_item -> {
                    viewModel.onDeleteClicked()
                    true
                }
                R.id.toot_item -> {
                    viewModel.onSendTootClicked()
                    true
                }
                else -> false
            }


}