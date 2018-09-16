package io.github.jamiesanson.mammut.data.converters

import io.github.jamiesanson.mammut.data.database.entities.InstanceRegistrationEntity
import io.github.jamiesanson.mammut.data.models.InstanceRegistration

fun InstanceRegistrationEntity.toModel(): InstanceRegistration
        = InstanceRegistration(id, clientId, clientSecret, redirectUri, instanceName, accessToken)

fun InstanceRegistration.toEntity(): InstanceRegistrationEntity
        = InstanceRegistrationEntity(id, clientId, clientSecret, redirectUri, instanceName, accessToken)