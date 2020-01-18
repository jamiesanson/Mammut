package io.github.koss.mammut.data.extensions

import io.github.koss.mammut.data.models.Status

val Status.workTag: String
    get() = id.toString() + account?.accountId.toString()

fun Status.getUniqueWorkTag(actionName: String): String =
        "${id}_${account?.accountId}_$actionName"