package com.example.applock.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class LockType {
    PIN,
    PASSWORD, // Alphanumeric password
    PATTERN
}

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

    fun setLockType(type: LockType) {
        prefs.edit().putString(KEY_LOCK_TYPE, type.name).apply()
    }

    fun getLockType(): LockType {
        val name = prefs.getString(KEY_LOCK_TYPE, LockType.PIN.name)
        return try {
            LockType.valueOf(name ?: LockType.PIN.name)
        } catch (e: Exception) {
            LockType.PIN
        }
    }

    // Biometric toggle (fingerprint/face unlock shortcut on overlay)
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setPin(pin: String) = prefs.edit().putString(KEY_PIN, pin).apply()
    fun getPin(): String = prefs.getString(KEY_PIN, "1234") ?: "1234"

    fun setPassword(password: String) = prefs.edit().putString(KEY_PASSWORD, password).apply()
    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "pass123") ?: "pass123"

    fun setPattern(pattern: String) = prefs.edit().putString(KEY_PATTERN, pattern).apply()
    fun getPattern(): String = prefs.getString(KEY_PATTERN, "0,1,2,5") ?: "0,1,2,5"

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
        private const val KEY_PASSWORD = "secure_user_password"
        private const val KEY_PATTERN = "secure_user_pattern"
        private const val KEY_LOCK_TYPE = "secure_lock_type"
        private const val KEY_BIOMETRIC_ENABLED = "secure_biometric_enabled"
        private const val KEY_LOCKED_PACKAGES = "locked_package_names"
    }
}
