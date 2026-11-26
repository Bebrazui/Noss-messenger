package com.nostr.expressive.shared.storage

import java.util.prefs.Preferences

actual object PlatformStorage {
    private val prefs = Preferences.userRoot().node("com.nostr.expressive")

    actual fun saveString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    actual fun getString(key: String): String? {
        return prefs.get(key, null)
    }

    actual fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }
}
