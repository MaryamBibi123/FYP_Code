package com.example.signuplogina.fragments

// SingleLiveEvent.kt (typically in a 'utils' or 'common' package)

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Log

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and SnackBar messages.
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is re-created. SingleLiveEvent prevents this.
 *
 * Note: Only one observer is supported.
 */
open class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Log.w("SingleLiveEvent", "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner, Observer<T> { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    fun call() {
        value = null
    }

    // Helper to get content and mark it as handled (useful if you don't want to use the automatic pending flag)
    // Not strictly necessary if you rely on the observe behavior, but can be useful.
    fun getContentIfNotHandled(): T? {
        return if (pending.compareAndSet(true, false)) {
            value
        } else {
            null
        }
    }

    fun peekContent(): T? = value
}