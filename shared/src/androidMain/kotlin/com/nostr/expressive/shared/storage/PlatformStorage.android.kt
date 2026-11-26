package com.nostr.expressive.shared.storage

import android.content.Context
import android.content.SharedPreferences

actual object PlatformStorage {
    private var prefs: SharedPreferences? = null
    private val memoryFallback = mutableMapOf<String, String>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("nostr_expressive_prefs", Context.MODE_PRIVATE)
    }

    actual fun saveString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply() ?: run {
            memoryFallback[key] = value
        }
    }

    actual fun getString(key: String): String? {
        return prefs?.getString(key, null) ?: memoryFallback[key]
    }

    actual fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply() ?: run {
            memoryFallback.remove(key)
        }
    }
}
