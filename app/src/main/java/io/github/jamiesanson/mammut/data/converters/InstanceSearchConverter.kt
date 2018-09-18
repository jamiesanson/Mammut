package io.github.jamiesanson.mammut.data.converters

import io.github.jamiesanson.mammut.data.database.entities.InstanceSearchResultEntity
import io.github.jamiesanson.mammut.data.models.InstanceSearchResult

fun InstanceSearchResultEntity.toModel() = InstanceSearchResult(name, users)

fun InstanceSearchResult.toEntity() = InstanceSearchResultEntity(name = name, users = users)