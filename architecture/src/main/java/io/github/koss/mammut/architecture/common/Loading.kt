package io.github.koss.mammut.architecture.common

import android.os.Parcelable
import io.github.koss.mammut.architecture.SideEffect
import kotlinx.android.parcel.Parcelize

@Parcelize
open class Loading(val isLoading: Boolean): Parcelable, SideEffect