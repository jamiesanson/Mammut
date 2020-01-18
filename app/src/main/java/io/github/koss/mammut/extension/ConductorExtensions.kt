package io.github.koss.mammut.extension

import com.bluelinelabs.conductor.Controller
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.subfeature.navigation.InstanceController
import java.lang.IllegalStateException


fun Controller.instanceComponent(): InstanceComponent =
        (parentController as? InstanceController)?.component
                ?: parentController?.instanceComponent()
                ?: throw IllegalStateException("No parent InstanceController found")