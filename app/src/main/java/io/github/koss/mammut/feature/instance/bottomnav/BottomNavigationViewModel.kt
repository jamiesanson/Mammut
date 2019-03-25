package io.github.koss.mammut.feature.instance.bottomnav

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.extension.fullAcct
import io.github.koss.mammut.feature.instance.dagger.InstanceScope
import io.github.koss.mammut.repo.RegistrationRepository
import javax.inject.Inject
import javax.inject.Named

/**
 * Simple ViewModel for presenting bottom navigation content
 */
class BottomNavigationViewModel @Inject constructor(
        private val registrationRepository: RegistrationRepository,
        @InstanceScope
        @Named("instance_access_token")
        private val instanceAccessToken: String
) : ViewModel() {

    val viewState: LiveData<BottomNavigationViewState> = Transformations
            .map(registrationRepository.getAllCompletedRegistrationsLive(), ::produceViewState)

    private fun produceViewState(registrations: List<InstanceRegistration>): BottomNavigationViewState =
            BottomNavigationViewState(
                    currentUser = registrations
                            .first { it.accessToken?.accessToken == instanceAccessToken }
                            .account!!,
                    allAccounts = registrations
                            .mapNotNull { it.account?.copy(acct = it.account!!.fullAcct(it.instanceName)) }
                            .toSet())

}