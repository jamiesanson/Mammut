package io.github.jamiesanson.mammut.feature.instance.subfeature.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Accounts
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.database.MammutDatabase
import io.github.jamiesanson.mammut.data.models.Account
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.extension.run
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileScope
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import javax.inject.Named

class ProfileViewModel @Inject constructor(
        @ProfileScope
        private val account: Account?,
        @InstanceScope
        @Named("instance_name")
        private val instanceName: String,
        @InstanceScope
        private val client: MastodonClient,
        private val database: MammutDatabase
): ViewModel() {

    val accountLiveData: LiveData<Account> = MutableLiveData()

    init {
        if (account == null) {
            launch {
                val registration = database.instanceRegistrationDao().getRegistrationByName(instanceName)

                val id = registration?.account?.accountId ?: throw IllegalStateException("No associated account found with for $instanceName")
                val accountResult = Accounts(client).getAccount(id).run()
                val account = when (accountResult) {
                    is Either.Right -> accountResult.b
                    is Either.Left -> {
                        throw Exception(accountResult.a)
                    }
                }.toEntity()

                accountLiveData.postSafely(account)
            }
        } else {
            accountLiveData.postSafely(account)
        }
    }

}