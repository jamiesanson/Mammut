package io.github.koss.mammut.feature.profile.domain

sealed class FollowState {

    data class Following(val loadingUnfollow: Boolean = false): FollowState()

    data class NotFollowing(val loadingFollow: Boolean = false): FollowState()

    object IsMe: FollowState()
}