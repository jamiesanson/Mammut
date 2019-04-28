package io.github.koss.mammut.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
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
                   val accountEmojis: ArrayList<Emoji> = ArrayList()
): Parcelable
