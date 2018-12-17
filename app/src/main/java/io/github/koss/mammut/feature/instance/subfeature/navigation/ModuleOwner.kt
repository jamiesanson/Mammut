package io.github.koss.mammut.feature.instance.subfeature.navigation

interface ModuleOwner {

    fun stashModule(key: String, module: Any)

    fun retrieveModule(key: String): Any
}