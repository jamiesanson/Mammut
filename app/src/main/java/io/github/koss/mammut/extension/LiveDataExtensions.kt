package io.github.jamiesanson.mammut.extension

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

suspend fun <T> LiveData<T>.awaitFirst(): T = suspendCancellableCoroutine {
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
    GlobalScope.launch (Dispatchers.Main) {
        observeForever(observer)
    }
}