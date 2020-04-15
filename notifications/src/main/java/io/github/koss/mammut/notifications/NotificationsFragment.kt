package io.github.koss.mammut.notifications

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.notifications.dagger.NotificationsComponent
import io.github.koss.mammut.notifications.dagger.NotificationsModule
import io.github.koss.mammut.notifications.dagger.NotificationsScope
import io.github.koss.mammut.notifications.databinding.NotificationFragmentBinding
import javax.inject.Inject
import javax.inject.Named

class NotificationsFragment : Fragment(R.layout.notification_fragment) {

    private val binding by viewLifecycleLazy { NotificationFragmentBinding.bind(requireView()) }

    private lateinit var viewModel: NotificationsViewModel

    @Inject
    @NotificationsScope
    lateinit var factory: MammutViewModelFactory

    @Inject
    @InstanceScope
    @Named("instance_access_token")
    lateinit var accessToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findSubcomponentFactory()
                .buildSubcomponent<NotificationsModule, NotificationsComponent>(NotificationsModule())
                .inject(this)

        viewModel = ViewModelProvider(requireActivity(), factory).get(accessToken, NotificationsViewModel::class.java)
    }
}
