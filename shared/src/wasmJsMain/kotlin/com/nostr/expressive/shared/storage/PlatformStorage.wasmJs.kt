package com.nostr.expressive.shared.storage

@JsFun("(key, value) => { try { localStorage.setItem(key, value); } catch(e){} }")
private external fun jsSaveStorage(key: String, value: String)

@JsFun("(key) => { try { return localStorage.getItem(key); } catch(e){ return null; } }")
private external fun jsGetStorage(key: String): String?

@JsFun("(key) => { try { localStorage.removeItem(key); } catch(e){} }")
private external fun jsRemoveStorage(key: String)

actual object PlatformStorage {
    private val memoryFallback = mutableMapOf<String, String>()

    actual fun saveString(key: String, value: String) {
        try {
            jsSaveStorage(key, value)
        } catch (_: Throwable) {
            memoryFallback[key] = value
        }
    }

    actual fun getString(key: String): String? {
        return try {
            jsGetStorage(key) ?: memoryFallback[key]
        } catch (_: Throwable) {
            memoryFallback[key]
        }
    }

    actual fun remove(key: String) {
        try {
            jsRemoveStorage(key)
        } catch (_: Throwable) {
            memoryFallback.remove(key)
        }
    }
}
