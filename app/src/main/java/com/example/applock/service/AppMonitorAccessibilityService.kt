package com.example.applock.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.applock.data.AppLockPreferences

class AppMonitorAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: LockOverlayManager
    private lateinit var prefs: AppLockPreferences
    private var lastUnlockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = LockOverlayManager(this)
        prefs = AppLockPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == applicationContext.packageName ||
            packageName == "com.android.systemui" ||
            packageName == lastUnlockedPackage) {
            return
        }

        if (prefs.isAppLocked(packageName)) {
            if (!overlayManager.isShowing()) {
                overlayManager.showLockOverlay(
                    packageName = packageName,
                    onAuthenticated = {
                        lastUnlockedPackage = packageName
                    }
                )
            }
        } else {
            lastUnlockedPackage = null
            overlayManager.dismiss()
        }
    }

    override fun onInterrupt() {
        overlayManager.dismiss()
    }
}
