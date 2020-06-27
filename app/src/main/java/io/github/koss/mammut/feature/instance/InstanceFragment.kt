package io.github.koss.mammut.feature.instance

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.koss.mammut.R
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.base.util.retained
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.home.dagger.HomeModule
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceModule
import io.github.koss.mammut.feature.profile.dagger.ProfileModule
import io.github.koss.mammut.feed.dagger.FeedModule
import io.github.koss.mammut.notifications.dagger.NotificationsModule
import io.github.koss.mammut.search.dagger.SearchModule
import io.github.koss.mammut.toot.dagger.ComposeTootModule

const val ARG_INSTANCE_NAME = "arg_instance_name"
const val ARG_AUTH_CODE = "arg_auth_code"

class InstanceFragment : Fragment(R.layout.instance_fragment), SubcomponentFactory {

    private val component: InstanceComponent by retained(key = { requireArguments().getString(ARG_AUTH_CODE)!! }) {
        (context as AppCompatActivity).applicationComponent
                .plus(instanceModule)
    }

    private val instanceModule: InstanceModule by lazy {
        InstanceModule(
                instanceName = requireArguments().getString(ARG_INSTANCE_NAME)!!,
                accessToken = requireArguments().getString(ARG_AUTH_CODE)!!)
    }

    override fun <Module, Subcomponent> buildSubcomponent(module: Module): Subcomponent {
        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        return when (module) {
            is ComposeTootModule -> component.plus(module)
            is NotificationsModule -> component.plus(module)
            is FeedModule -> component.plus(module)
            is ProfileModule -> component.plus(module)
            is HomeModule -> component.plus(module)
            is SearchModule -> component.plus(module)
            else -> throw IllegalArgumentException("Unknown module type")
        } as Subcomponent
    }
}