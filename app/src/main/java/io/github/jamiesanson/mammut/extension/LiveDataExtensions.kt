package io.github.jamiesanson.mammut.extension

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observe(owner: LifecycleOwner, onChanged: (T) -> Unit) {
    observe(owner, Observer(onChanged))
}

fun <T> LiveData<T>.postSafely(item: T) {
    (this as? MutableLiveData<T>)?.postValue(item)
}