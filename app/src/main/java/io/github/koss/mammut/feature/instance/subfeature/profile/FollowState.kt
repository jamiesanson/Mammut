package io.github.jamiesanson.mammut.feature.instance.subfeature.profile

sealed class FollowState {

    data class Following(val loadingUnfollow: Boolean = false): FollowState()

    data class NotFollowing(val loadingFollow: Boolean = false): FollowState()

    object IsMe: FollowState()
}