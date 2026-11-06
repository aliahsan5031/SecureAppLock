package com.example.applock.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.applock.data.local.AppDatabase
import com.example.applock.data.local.IntruderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntruderPhotoCapturer(private val context: Context) : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun captureIntruderPhoto(attemptedPackage: String) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)

                val dir = File(context.filesDir, "intruders").apply { if (!exists()) mkdirs() }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val photoFile = File(dir, "INTRUDER_${timeStamp}.jpg")

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("IntruderCapture", "Captured: ${photoFile.absolutePath}")
                            CoroutineScope(Dispatchers.IO).launch {
                                AppDatabase.getInstance(context).intruderDao().insertIntruder(
                                    IntruderEntity(
                                        imagePath = photoFile.absolutePath,
                                        attemptedPackage = attemptedPackage
                                    )
                                )
                            }
                            cleanup(cameraProvider)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e("IntruderCapture", "Photo capture failed: ${exc.message}", exc)
                            cleanup(cameraProvider)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("IntruderCapture", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun cleanup(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
