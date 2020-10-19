package io.github.koss.mammut.feature.profile.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Accounts
import io.github.koss.mammut.data.converters.toLocalModel
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.NetworkState
import io.github.koss.mammut.data.extensions.run
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.scope.ProfileScope
import io.github.koss.mammut.base.util.Logging.logWarning
import io.github.koss.mammut.base.util.postSafely
import io.github.koss.mammut.data.extensions.Result
import io.github.koss.mammut.data.extensions.orNull
import io.github.koss.mammut.feature.profile.domain.FollowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class ProfileViewModel @Inject constructor(
        @ProfileScope
        private val accountId: Long?,
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

        if (accountId == null) {
            launch {
                val registration = database.instanceRegistrationDao().getRegistrationByName(instanceName)

                val id = registration?.account?.accountId ?: throw IllegalStateException("No associated account found with for $instanceName")
                val accountResult = Accounts(client).getAccount(id).run()
                val account = when (accountResult) {
                    is Result.Success -> accountResult.data
                    is Result.Failure -> {
                        // We're most likely offline. Log this as a warning just in case
                        networkState.postSafely(NetworkState.Offline)
                        logWarning { accountResult.error.error }
                        null
                    }
                }?.toLocalModel() ?: return@launch

                accountLiveData.postSafely(account)
                followStateLiveData.postSafely(FollowState.IsMe)
                networkState.postSafely(NetworkState.Loaded)
            }
        } else {
            // Get relationship to current account
            // Go ahead and try and get some account info
            launch {
                val accountResult = Accounts(client).getAccount(accountId).run()
                val account = when (accountResult) {
                    is Result.Success -> accountResult.data
                    is Result.Failure -> {
                        // We're most likely offline. Log this as a warning just in case
                        networkState.postSafely(NetworkState.Offline)
                        logWarning { accountResult.error.error }
                        null
                    }
                }?.toLocalModel() ?: return@launch

                accountLiveData.postSafely(account)

                Accounts(client)
                        .getRelationships(accountIds = listOf(accountId))
                        .run()
                        .orNull()
                        ?.let { relationships ->
                            relationships.firstOrNull { it.id == accountId }?.let {
                                if (it.isFollowing) {
                                    followStateLiveData.postSafely(FollowState.Following())
                                } else {
                                    followStateLiveData.postSafely(FollowState.NotFollowing())
                                }
                            } ?: kotlin.run {
                                followStateLiveData.postSafely(FollowState.NotFollowing())
                            }
                        }

                networkState.postSafely(NetworkState.Loaded)
            }

        }
    }

    fun requestFollowToggle(followState: FollowState) {
        when (followState) {
            is FollowState.Following -> launch {
                followStateLiveData.postSafely(FollowState.Following(loadingUnfollow = true))
                Accounts(client)
                        .postUnFollow(accountId!!)
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
                        .postFollow(accountId!!)
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