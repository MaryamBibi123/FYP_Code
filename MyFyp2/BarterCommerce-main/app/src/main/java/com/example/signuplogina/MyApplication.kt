package com.example.signuplogina

import android.app.Application

class MyApplication : Application() {

    companion object {
        private var _instance: MyApplication? = null
        val instance: MyApplication
            get() = _instance
                ?: throw IllegalStateException("MyApplication is not initialized yet.")
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
    }
}
