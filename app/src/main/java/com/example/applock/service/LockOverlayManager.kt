package com.example.applock.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.applock.camera.IntruderPhotoCapturer
import com.example.applock.data.AppLockPreferences
import com.example.applock.ui.BiometricPromptActivity
import com.example.applock.ui.LockScreenOverlay

class LockOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val prefs = AppLockPreferences(context)
    private val photoCapturer = IntruderPhotoCapturer(context)
    private var failedAttempts = 0

    fun showLockOverlay(packageName: String, onAuthenticated: () -> Unit) {
        if (overlayView != null) return

        currentAuthCallback = {
            failedAttempts = 0
            dismiss()
            onAuthenticated()
        }

        currentFailedCallback = {
            failedAttempts++
            if (failedAttempts >= 2) {
                photoCapturer.captureIntruderPhoto(packageName)
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val lifecycleOwner = OverlayLifecycleOwner().apply { performRestore(null); handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                LockScreenOverlay(
                    targetPackage = packageName,
                    lockType = prefs.getLockType(),
                    biometricEnabled = prefs.isBiometricEnabled(),
                    expectedPin = prefs.getPin(),
                    expectedPassword = prefs.getPassword(),
                    expectedPattern = prefs.getPattern(),
                    onBiometricRequest = {
                        val intent = Intent(context, BiometricPromptActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                        context.startActivity(intent)
                    },
                    onSuccess = {
                        currentAuthCallback?.invoke()
                    },
                    onFailure = {
                        currentFailedCallback?.invoke()
                    }
                )
            }
        }

        overlayView = composeView
        windowManager.addView(composeView, layoutParams)
    }

    fun dismiss() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        currentAuthCallback = null
        currentFailedCallback = null
    }

    fun isShowing(): Boolean = overlayView != null

    companion object {
        private var currentAuthCallback: (() -> Unit)? = null
        private var currentFailedCallback: (() -> Unit)? = null

        fun notifyBiometricSuccess() {
            currentAuthCallback?.invoke()
        }

        fun notifyBiometricFailed() {
            currentFailedCallback?.invoke()
        }
    }

    private class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun performRestore(savedState: android.os.Bundle?) = savedStateRegistryController.performRestore(savedState)
        fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    }
}
