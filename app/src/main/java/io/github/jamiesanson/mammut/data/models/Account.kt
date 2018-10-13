package io.github.jamiesanson.mammut.data.models

data class Account(val accountId: Long = 0L,
                   val userName: String = "",
                   val acct: String = "",
                   val displayName: String = "",
                   val note: String = "",
                   val accountUrl: String = "",
                   val avatar: String = "",
                   val header: String = "",
                   val isLocked: Boolean = false,
                   val accountCreatedAt: String = "",
                   val followersCount: Int = 0,
                   val followingCount: Int = 0,
                   val statusesCount: Int = 0,
                   val emojis: List<Emoji> = emptyList())
