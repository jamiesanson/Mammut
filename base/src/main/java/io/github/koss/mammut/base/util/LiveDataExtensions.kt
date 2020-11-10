package io.github.koss.mammut.base.util

import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume

fun <T> LiveData<T>.observe(owner: LifecycleOwner, onChanged: (T) -> Unit) {
    observe(owner, Observer(onChanged))
}

fun <T> LiveData<T>.postIfMutable(item: T) {
    (this as? MutableLiveData<T>)?.postValue(item)
}