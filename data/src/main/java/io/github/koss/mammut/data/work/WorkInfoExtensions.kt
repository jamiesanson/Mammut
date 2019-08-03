package io.github.koss.mammut.data.work

import androidx.work.WorkInfo
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.data.extensions.workTag
import io.github.koss.mammut.data.work.WorkConstants.TootInteraction.TAG_BOOST
import io.github.koss.mammut.data.work.WorkConstants.TootInteraction.TAG_UNBOOST

fun List<WorkInfo>.hasPendingBoostFor(status: Status): Boolean = this
        .filter { !it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_BOOST) }

fun List<WorkInfo>.containsSuccessfulBoost(status: Status): Boolean = this
        .filter { it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_BOOST) }

fun List<WorkInfo>.containsSuccessfulUnboost(status: Status): Boolean = this
        .filter { it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_UNBOOST) }

fun List<WorkInfo>.hasPendingUnboostFor(status: Status): Boolean = this
        .filter { !it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_UNBOOST) }