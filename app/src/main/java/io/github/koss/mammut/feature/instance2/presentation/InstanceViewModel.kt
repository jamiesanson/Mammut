package io.github.koss.mammut.feature.instance2.presentation

import androidx.lifecycle.*
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.navigation.Event
import io.github.koss.mammut.base.navigation.NavigationEvent
import io.github.koss.mammut.base.navigation.NavigationEventBus
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.base.navigation.Tab
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.feature.instance2.presentation.navigation.UserPeekRequested
import io.github.koss.mammut.feature.instance2.presentation.state.*
import io.github.koss.mammut.feature.instance2.presentation.navigation.NavigationEvent as InstanceNavigationEvent
import io.github.koss.mammut.repo.RegistrationRepository
import io.github.koss.randux.createStore
import io.github.koss.randux.utils.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class InstanceViewModel @Inject constructor(
        @InstanceScope
        @Named("instance_access_token")
        private val instanceAccessToken: String,
        @InstanceScope
        @Named("instance_name")
        private val instanceName: String,
        @ApplicationScope
        private val navigationEventBus: NavigationEventBus,
        registrationRepository: RegistrationRepository
) : ViewModel() {

    private val store: Store = createStore(
            reducer = InstanceReducer(),
            preloadedState = InstanceState(
                    instanceName = instanceName,
                    accessToken = instanceAccessToken
            )
    )

    private val stateRelay = Channel<InstanceState>(capacity = CONFLATED)

    val state = liveData {
        for (item in stateRelay) {
            emit(item)
        }
    }

    val navigationEvents: LiveData<Event<InstanceNavigationEvent>> = MutableLiveData()

    init {
        // Publish state
        store.subscribe {
            (store.getState() as? InstanceState)?.let {
                stateRelay.offer(it)
            }
        }

        navigationEventBus.events.observeForever { event ->
            when (val contents = event.peekContent()) {
                is NavigationEvent.Instance.Changed -> if (contents.newInstance.accessToken?.accessToken == state.value?.accessToken) {
                    event.getContentIfNotHandled() // Handle the event
                    (navigationEvents as MutableLiveData).postValue(Event(UserPeekRequested))
                }
                is NavigationEvent.Feed.OffscreenCountChanged -> if (contents.targetInstanceToken == instanceAccessToken) {
                    event.getContentIfNotHandled()
                    store.dispatch(OnOffscreenItemCountChanged(contents.newCount))
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            registrationRepository.getAllRegistrationsFlow()
                    .collect {
                        store.dispatch(OnRegistrationsLoaded(it))
                    }
        }
    }

    fun changeFeedType(newFeedType: FeedType) {
        navigationEventBus.sendEvent(NavigationEvent.Feed.TypeChanged(newFeedType = newFeedType, targetInstanceToken = instanceAccessToken))
        store.dispatch(OnFeedTypeChanged(newFeedType))
    }

    fun reselectTab(tab: Tab) {
        navigationEventBus.sendEvent(NavigationEvent.Instance.TabReselected(tab))
    }
}