package com.ss.challengetask.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ss.challengetask.R
import kotlinx.coroutines.delay
import java.io.File

class ComposeFileProvider : FileProvider(
    R.xml.file_paths
) {
    companion object {
        fun getTempImageFile(context: Context): Pair<Uri, File> {
            val directory = File(context.cacheDir, "images")
            directory.mkdirs()
            val file = File.createTempFile(
                "photo_${System.currentTimeMillis()}",
                ".jpg",
                directory,
            )
            val authority = context.packageName + ".fileprovider"
            return Pair(getUriForFile(
                context,
                authority,
                file,
            ),file)
        }
    }
}

@Composable
fun ImagePicker(
    modifier: Modifier = Modifier,
    imageUriCallback : (Uri) -> Unit,
) {
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val imageBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    lateinit var cameraDevice: CameraDevice
    val mBackgroundHandler by lazy { Handler(HandlerThread("CameraBackground").apply { start() }.looper) }
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val rearFaceCameraId = cameraManager.cameraIdList.firstOrNull {
        cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    } ?: throw Exception("No rear-facing camera found.")
    val takePhoto = {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Please Grant Permission for Camera , to take photo", Toast.LENGTH_LONG).show()
        }else {
            cameraManager.openCamera(rearFaceCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    takePictureSelf(context, windowManager, cameraDevice, mBackgroundHandler) { bitmap: Bitmap?, path: Uri? ->
                        imageUri.value = path
                        path?.let { imageUriCallback(it) }
//                        Toast.makeText(context, "${path}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            },
                null
            )
        }
    }
    LaunchedEffect(true) {
        // delay(500)
        takePhoto()
    }
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Row(horizontalArrangement = Arrangement.Center
                ,verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically){
                OutlinedButton(onClick = { takePhoto() },
                    shape = CircleShape,
                    border= BorderStroke(1.dp, Color.White)
                ) {
                    Text(modifier = modifier
                        .padding(8.dp)
                        .align(alignment = Alignment.CenterVertically), text = "Take photo", style = TextStyle(color= Color.White))
                }
            }
            if (imageUri.value != null) {
                AsyncImage(model = imageUri.value, modifier = Modifier.height(67.dp).width(48.dp).padding(4.dp), contentDescription = "Taken Photo")
            }
        }
    }

}