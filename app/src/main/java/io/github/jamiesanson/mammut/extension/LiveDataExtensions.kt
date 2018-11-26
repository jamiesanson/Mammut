package io.github.jamiesanson.mammut.extension

import androidx.lifecycle.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

suspend fun <T> LiveData<T>.awaitFirst(): T = suspendCancellableCoroutine {
    // Try short circuit
    value?.let { value ->
        it.resume(value)
        return@suspendCancellableCoroutine
    }

    // Wait for first emission
    val observer = { value: T ->
        it.resume(value)
    }
    observeForever(observer)
}