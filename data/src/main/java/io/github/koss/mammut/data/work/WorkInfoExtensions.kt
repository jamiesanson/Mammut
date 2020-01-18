package io.github.koss.mammut.data.work

import androidx.work.WorkInfo
import io.github.koss.mammut.data.extensions.workTag
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.data.work.WorkConstants.TootInteraction.TAG_BOOST
import io.github.koss.mammut.data.work.WorkConstants.TootInteraction.TAG_RETOOT
import io.github.koss.mammut.data.work.WorkConstants.TootInteraction.TAG_UNBOOST
import io.github.koss.mammut.data.work.WorkConstants.TootInteraction.TAG_UNRETOOT

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

fun List<WorkInfo>.containsSuccessfulRetoot(status: Status): Boolean = this
        .filter { it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_RETOOT) }

fun List<WorkInfo>.containsSuccessfulUnretoot(status: Status): Boolean = this
        .filter { it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_UNRETOOT) }

fun List<WorkInfo>.hasPendingUnboostFor(status: Status): Boolean = this
        .filter { !it.state.isFinished }
        .filter { it.tags.contains(status.workTag) }
        .any { it.tags.contains(TAG_UNBOOST) }