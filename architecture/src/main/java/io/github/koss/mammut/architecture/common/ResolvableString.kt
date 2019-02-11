package io.github.koss.mammut.architecture.common

import android.content.res.Resources
import android.os.Parcelable
import io.github.koss.mammut.architecture.SideEffect
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

private typealias Resolver = (Resources) -> String

@Parcelize
open class ResolvableString(val resolver: @RawValue Resolver): Parcelable, SideEffect