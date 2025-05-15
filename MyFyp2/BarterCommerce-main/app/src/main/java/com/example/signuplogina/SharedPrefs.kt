package com.example.signuplogina

import android.content.Context
import android.content.SharedPreferences
import android.util.Log


class SharedPrefs() {


//    private val prefs: SharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
private val prefs: SharedPreferences by lazy {
    MyApplication.instance.applicationContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
}
    fun setValue(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        Log.e("NavigationDebug", "🟢 Sharedprefs1 Loaded ")

    }

    fun getValue(key: String): String? {
        return prefs.getString(key, null)
        Log.e("NavigationDebug", "🟢 Sharedprefs2 Loaded ")

    }


    fun setIntValue(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        Log.e("NavigationDebug", "🟢 Sharedprefs3 Loaded ")

    }

    fun getIntValue(key: String, i: Int): Int {
        return prefs.getInt(key, i.toInt())
        Log.e("NavigationDebug", "🟢 Sharedprefs4 Loaded ")

    }
}