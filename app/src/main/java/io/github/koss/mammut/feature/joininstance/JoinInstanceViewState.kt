package io.github.koss.mammut.feature.joininstance

import android.content.res.Resources
import android.os.Parcelable
import io.github.koss.mammut.architecture.OneShot
import io.github.koss.mammut.data.models.InstanceSearchResult
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

/**
 * ViewState of the Join Instance screen
 */
@Parcelize
data class JoinInstanceViewState(
        val isLoading: Boolean = false,
        val errorMessage: OneShot<(Resources) -> String>,
        val oauthUrl: OneShot<String>,
        val searchResults: @RawValue List<InstanceSearchResult>
): Parcelable {

    companion object {
        val INITIAL = JoinInstanceViewState(
                isLoading = false,
                errorMessage = OneShot.empty(),
                oauthUrl = OneShot.empty(),
                searchResults = emptyList()
        )
    }
}