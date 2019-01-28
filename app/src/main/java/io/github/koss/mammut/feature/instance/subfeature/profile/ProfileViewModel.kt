package io.github.koss.mammut.feature.instance.subfeature.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.core.orNull
import com.crashlytics.android.Crashlytics
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Accounts
import io.github.koss.mammut.data.converters.toEntity
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.NetworkState
import io.github.koss.mammut.extension.postSafely
import io.github.koss.mammut.extension.run
import io.github.koss.mammut.feature.instance.dagger.InstanceScope
import io.github.koss.mammut.feature.instance.subfeature.profile.dagger.ProfileScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
): ViewModel(), CoroutineScope by GlobalScope {

    val networkState: LiveData<NetworkState> = MutableLiveData()

    val accountLiveData: LiveData<Account> = MutableLiveData()

    val followStateLiveData: LiveData<FollowState> = MutableLiveData()

    init {
       load()
    }

    fun load() {
        networkState.postSafely(NetworkState.Loading)

        if (account == null) {
            launch {
                val registration = database.instanceRegistrationDao().getRegistrationByName(instanceName)

                val id = registration?.account?.accountId ?: throw IllegalStateException("No associated account found with for $instanceName")
                val accountResult = Accounts(client).getAccount(id).run()
                val account = when (accountResult) {
                    is Either.Right -> accountResult.b
                    is Either.Left -> {
                        // We're most likely offline. Log this as a warning just in case
                        networkState.postSafely(NetworkState.Offline)
                        Crashlytics.log(Log.WARN, ProfileViewModel::class.java.name, accountResult.a.error)
                        null
                    }
                }?.toEntity() ?: return@launch

                accountLiveData.postSafely(account)
                followStateLiveData.postSafely(FollowState.IsMe)
                networkState.postSafely(NetworkState.Loaded)
            }
        } else {
            // Get relationship to current account
            // Go ahead and try and get some account info
            launch accountInfo@{
                Accounts(client)
                        .getRelationships(accountIds = listOf(account.accountId))
                        .run()
                        .orNull()
                        ?.let { relationships ->
                            relationships.firstOrNull { it.id == account.accountId }?.let {
                                if (it.isFollowing) {
                                    followStateLiveData.postSafely(FollowState.Following())
                                } else {
                                    followStateLiveData.postSafely(FollowState.NotFollowing())
                                }
                            } ?: kotlin.run {
                                followStateLiveData.postSafely(FollowState.NotFollowing())
                            }
                        }
            }

            accountLiveData.postSafely(account)
            networkState.postSafely(NetworkState.Loaded)
        }
    }

    fun requestFollowToggle(followState: FollowState) {
        when (followState) {
            is FollowState.Following -> launch {
                followStateLiveData.postSafely(FollowState.Following(loadingUnfollow = true))
                Accounts(client)
                        .postUnFollow(account?.accountId!!)
                        .run()
                        .orNull()
                        ?.let {
                    if (!it.isFollowing) {
                        followStateLiveData.postSafely(FollowState.NotFollowing())
                    } else {
                        followStateLiveData.postSafely(FollowState.Following())
                    }
                }
            }
            is FollowState.NotFollowing -> launch {
                followStateLiveData.postSafely(FollowState.NotFollowing(loadingFollow = true))
                Accounts(client)
                        .postFollow(account?.accountId!!)
                        .run()
                        .orNull()
                        ?.let {
                            if (it.isFollowing) {
                                followStateLiveData.postSafely(FollowState.Following())
                            } else {
                                followStateLiveData.postSafely(FollowState.NotFollowing())
                            }
                        }
            }
            FollowState.IsMe -> throw IllegalStateException("You can't follow yourself")
        }
    }

}