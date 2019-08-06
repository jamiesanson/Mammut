package io.github.koss.mammut.base.util

import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun <T> LiveData<T>.observe(owner: LifecycleOwner, onChanged: (T) -> Unit) {
    observe(owner, Observer(onChanged))
}

fun <T> LiveData<T>.postSafely(item: T) {
    (this as? MutableLiveData<T>)?.postValue(item)
}

fun <X> LiveData<List<X>>.filterElements(predicate: (X) -> Boolean): LiveData<List<X>> {
    return MediatorLiveData<List<X>>().apply {
        addSource(this@filterElements) { x ->
            value = x.filter(predicate)
        }
    }
}

suspend fun <T> LiveData<T>.awaitFirst(): T = coroutineScope {
    suspendCancellableCoroutine<T> {
        var resumed = false
        // Try short circuit
        value?.let { value ->
            it.resume(value)
            resumed = true
            return@suspendCancellableCoroutine
        }

        // Wait for first emission
        val observer = { value: T ->
            if (!resumed) {
                it.resume(value)
                resumed = true
            }
        }
        launch(Dispatchers.Main) {
            observeForever(observer)
        }
    }
}