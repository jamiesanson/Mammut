package io.github.jamiesanson.mammut.feature.instance.subfeature.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.core.orNull
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Accounts
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.database.MammutDatabase
import io.github.jamiesanson.mammut.data.models.Account
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.extension.run
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileScope
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

    val accountLiveData: LiveData<Account> = MutableLiveData()

    val followStateLiveData: LiveData<FollowState> = MutableLiveData()

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
                followStateLiveData.postSafely(FollowState.IsMe)
            }
        } else {
            // Get relationship to current account
            // Go ahead and try and get some account info
            launch accountInfo@{
                val registration = database.instanceRegistrationDao().getRegistrationByName(instanceName)

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