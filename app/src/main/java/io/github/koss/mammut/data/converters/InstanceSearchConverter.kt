package io.github.koss.mammut.data.converters

import io.github.koss.mammut.data.database.entities.InstanceSearchResultEntity
import io.github.koss.mammut.data.models.InstanceSearchResult

fun InstanceSearchResultEntity.toModel() = InstanceSearchResult(name, users)

fun InstanceSearchResult.toEntity() = InstanceSearchResultEntity(name = name, users = users)