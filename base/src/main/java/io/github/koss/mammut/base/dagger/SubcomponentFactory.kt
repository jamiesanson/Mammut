package io.github.koss.mammut.base.dagger

interface SubcomponentFactory {

    fun <Module, Subcomponent> buildSubcomponent(module: Module): Subcomponent
}