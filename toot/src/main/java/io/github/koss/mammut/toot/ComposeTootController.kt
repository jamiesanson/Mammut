package io.github.koss.mammut.toot

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.toot.dagger.*
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import java.lang.IllegalStateException
import javax.inject.Inject

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
    }


}