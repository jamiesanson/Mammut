package io.github.koss.mammut.feature.contributors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class ContributorsController: BaseController() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_contributors, container, false)

}