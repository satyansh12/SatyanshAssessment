package com.ss.challengetask.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.WindowManager
import com.ss.challengetask.utils.ComposeFileProvider.Companion.getTempImageFile
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

private val ORIENTATIONS = SparseIntArray().apply {
    append(Surface.ROTATION_0, 90)
    append(Surface.ROTATION_90, 0)
    append(Surface.ROTATION_180, 270)
    append(Surface.ROTATION_270, 180)
}

fun takePictureSelf(
    context: Context,
    windowManager: WindowManager,
    cameraDevice: CameraDevice,
    mBackgroundHandler: Handler,
    onTakenPicktureCallback: (Bitmap?, Uri?) -> Unit
) {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
        var jpegSizes: Array<Size>? = null
        if (characteristics != null) {
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG)
        }
        var width = 640
        var height = 480
        if (jpegSizes != null && jpegSizes.isNotEmpty()) {
            width = jpegSizes[0].width
            height = jpegSizes[0].height
        }
        val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
        val outputSurfaces = ArrayList<Surface>(2)
        outputSurfaces.add(reader.surface)
        val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(reader.surface)
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        // Orientation
        val rotation = windowManager.defaultDisplay.rotation
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS[rotation])
//        val fileName = "photo_${System.currentTimeMillis()}.jpg"
//        val file = File.createTempFile(fileName, null, context.cacheDir);
//        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileUriPair = getTempImageFile(context)
        val readerListener = ImageReader.OnImageAvailableListener { reader ->
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                save(bytes,fileUriPair.second.absolutePath)
            } catch (e: FileNotFoundException) {
                onTakenPicktureCallback(null, null)
            } catch (e: IOException) {
                onTakenPicktureCallback(null, null)
            } finally {
                if (image != null) {
                    image.close()
                }
            }
        }
        reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
        val captureListener = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                val bitmap = BitmapFactory.decodeFile(fileUriPair.second.absolutePath)
                onTakenPicktureCallback(bitmap, fileUriPair.first)
            }
        }
        cameraDevice.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.capture(captureBuilder.build(), captureListener, mBackgroundHandler)
                } catch (e: CameraAccessException) {
                    onTakenPicktureCallback(null, null)
                }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                onTakenPicktureCallback(null, null)
            }
        }, mBackgroundHandler)
    } catch (e: CameraAccessException) {
        onTakenPicktureCallback(null,null)
    }
}

private fun save(bytes: ByteArray,filepath:String) {
    var output: OutputStream? = null
    try {
        output = FileOutputStream(filepath)
        output.write(bytes)
    } finally {
        output?.close()
    }
}