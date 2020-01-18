package io.github.koss.mammut.data.converters

import io.github.koss.mammut.data.database.entities.InstanceRegistrationEntity
import io.github.koss.mammut.data.models.InstanceRegistration

fun InstanceRegistrationEntity.toNetworkModel(): InstanceRegistration
        = InstanceRegistration(id, clientId, clientSecret, redirectUri, instanceName, accessToken, account, orderIndex)

fun InstanceRegistration.toLocalModel(): InstanceRegistrationEntity
        = InstanceRegistrationEntity(id, clientId, clientSecret, redirectUri, instanceName, accessToken, account, orderIndex)