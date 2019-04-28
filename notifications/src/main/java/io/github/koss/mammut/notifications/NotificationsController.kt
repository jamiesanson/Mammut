package io.github.koss.mammut.notifications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.base.util.arg
import io.github.koss.mammut.notifications.dagger.NotificationsComponent
import io.github.koss.mammut.notifications.dagger.NotificationsModule
import io.github.koss.mammut.notifications.dagger.NotificationsScope
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import javax.inject.Inject

/**
 * This is the main controller for notifications, accessible through the bottom navigation control
 * on the Instance screen
 */
@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class NotificationsController: BaseController {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    private lateinit var viewModel: NotificationsViewModel

    @Inject
    @NotificationsScope
    lateinit var factory: MammutViewModelFactory

    private val accessToken: String by arg(ARG_INSTANCE_ACCESS_TOKEN)

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (targetController as? SubcomponentFactory)
                ?.buildSubcomponent<NotificationsModule, NotificationsComponent>(NotificationsModule())
                ?.inject(this) ?: throw IllegalStateException("ParentController must be subcomponent factory")

        viewModel = ViewModelProviders
                .of(context as FragmentActivity, factory).get(accessToken, NotificationsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_notifications, container, false)

    companion object {

        @JvmStatic
        fun newInstance(accessToken: String) =
                NotificationsController(bundleOf(ARG_INSTANCE_ACCESS_TOKEN to accessToken))

        const val ARG_INSTANCE_ACCESS_TOKEN = "instance_access_token"
    }
}