package io.github.koss.mammut.feature.joininstance.sideeffects

import android.os.Parcelable
import io.github.koss.mammut.architecture.SideEffect
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OAuthUrlFormed(val url: String): Parcelable, SideEffect