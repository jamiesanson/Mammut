package io.github.jamiesanson.mammut.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.jamiesanson.mammut.data.models.Account
import io.github.jamiesanson.mammut.data.models.InstanceAccessToken
import kotlinx.android.parcel.RawValue

@Entity
data class InstanceRegistrationEntity(
        @PrimaryKey
        var id: Long = 0,

        val clientId: String = "",

        val clientSecret: String = "",

        val redirectUri: String = "",

        val instanceName: String = "",

        @Embedded
        val accessToken: @RawValue InstanceAccessToken? = null,

        @Embedded
        val account: @RawValue Account? = null
)