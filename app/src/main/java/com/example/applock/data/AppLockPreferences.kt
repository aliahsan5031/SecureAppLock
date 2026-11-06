package com.example.applock.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AppLockPreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "app_lock_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setPin(pin: String) = prefs.edit().putString(KEY_PIN, pin).apply()
    fun getPin(): String? = prefs.getString(KEY_PIN, null)

    fun setLockedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, packages).apply()
    }

    fun getLockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun toggleAppLock(packageName: String, lock: Boolean) {
        val current = getLockedPackages().toMutableSet()
        if (lock) current.add(packageName) else current.remove(packageName)
        setLockedPackages(current)
    }

    fun isAppLocked(packageName: String): Boolean = getLockedPackages().contains(packageName)

    companion object {
        private const val KEY_PIN = "secure_user_pin"
        private const val KEY_LOCKED_PACKAGES = "locked_package_names"
    }
}
