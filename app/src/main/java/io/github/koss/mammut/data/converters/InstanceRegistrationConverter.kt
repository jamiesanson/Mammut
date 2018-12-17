package io.github.koss.mammut.data.converters

import io.github.koss.mammut.data.database.entities.InstanceRegistrationEntity
import io.github.koss.mammut.data.models.InstanceRegistration

fun InstanceRegistrationEntity.toModel(): InstanceRegistration
        = InstanceRegistration(id, clientId, clientSecret, redirectUri, instanceName, accessToken, account)

fun InstanceRegistration.toEntity(): InstanceRegistrationEntity
        = InstanceRegistrationEntity(id, clientId, clientSecret, redirectUri, instanceName, accessToken, account)