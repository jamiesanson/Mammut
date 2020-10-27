package io.github.koss.mammut.base.util

import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume

fun <T> LiveData<T>.observe(owner: LifecycleOwner, onChanged: (T) -> Unit) {
    observe(owner, Observer(onChanged))
}

fun <T> LiveData<T>.tryPost(item: T) {
    (this as? MutableLiveData<T>)?.postValue(item)
}

fun <X> LiveData<List<X>>.filterElements(predicate: (X) -> Boolean): LiveData<List<X>> {
    return MediatorLiveData<List<X>>().apply {
        addSource(this@filterElements) { x ->
            value = x.filter(predicate)
        }
    }
}

suspend fun <T> LiveData<T>.awaitFirst(): T = suspendCancellableCoroutine { continuation ->
    // Try short circuit
    value?.let { value ->
        continuation.resume(value)
        return@suspendCancellableCoroutine
    }

    // Wait for first emission
    val observer = { value: T ->
        continuation.resume(value)
    }

    observeForever(observer)

    continuation.invokeOnCancellation {
        removeObserver(observer)
    }
}