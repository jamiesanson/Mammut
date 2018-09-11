package io.github.jamiesanson.mammut.feature.joininstance

import arrow.optics.optics
import io.github.koss.randux.utils.Action

data class InstanceUrlChanged(val url: String): Action()

@optics
data class JoinInstanceState(
        val instanceUrl: String = "",
        val instanceJoined: Boolean = false
) { companion object }