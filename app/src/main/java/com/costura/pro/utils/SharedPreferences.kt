package com.costura.pro.utils

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
        set(value) = sharedPreferences.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, value).apply()

    var userId: String?
        get() = sharedPreferences.getString(Constants.KEY_USER_ID, null)
        set(value) = sharedPreferences.edit().putString(Constants.KEY_USER_ID, value).apply()

    var userRole: String?
        get() = sharedPreferences.getString(Constants.KEY_USER_ROLE, null)
        set(value) = sharedPreferences.edit().putString(Constants.KEY_USER_ROLE, value).apply()

    var username: String?
        get() = sharedPreferences.getString(Constants.KEY_USERNAME, null)
        set(value) = sharedPreferences.edit().putString(Constants.KEY_USERNAME, value).apply()

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}