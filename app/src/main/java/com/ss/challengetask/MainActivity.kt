package com.ss.challengetask

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.ss.challengetask.ui.theme.ChallengeTaskTheme
import com.ss.challengetask.utils.CaptureBtn
import com.ss.challengetask.utils.DeviceDetails
import com.ss.challengetask.utils.ImagePicker
import com.ss.challengetask.utils.RequestLocationPermission
import com.ss.challengetask.utils.timer.TimerApp
import com.ss.challengetask.viewmodel.CountTimeViewModel
import com.ss.challengetask.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel : MainViewModel by viewModels()
    private val countTimeViewModel : CountTimeViewModel by viewModels()

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val CAMERA_PERMISSION_CODE = 1
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Do something when the network is available
                Log.d("Internet", "Internet Connected")
                mainViewModel.setUserOnlineOfflineStatus(isOnline = true)
                // upload on network
                if (mainViewModel.isUploadOnNetworkNeeded != null && mainViewModel.isUploadOnNetworkNeeded == true) {
                    mainViewModel.saveDeviceDetails()
                    mainViewModel.isUploadOnNetworkNeeded = false
                }
            }
            override fun onLost(network: Network) {
                // Do something when the network is lost
                Log.d("Internet", "Internet Disconnected")
                mainViewModel.setUserOnlineOfflineStatus(isOnline = false)
            }
        }
        setContent {
            ChallengeTaskTheme {
                // A surface container using the 'background' color from the theme
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val deviceDetails = remember { mutableStateOf<DeviceDetails?>(null) }
                val timeInfo = countTimeViewModel.time.observeAsState("15")
                countTimeViewModel.addOnFinishCallback {
                    scope.launch {
                        deviceDetails.value = mainViewModel.captureDetails(context = context)
                        Toast.makeText(context,"Finish -> Capturing & and Saving to Cloud", Toast.LENGTH_SHORT).show()
                        mainViewModel.saveDeviceDetails()
                    }
                }
                Column(modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black)) {
                    Column(modifier= Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp)
                        .verticalScroll(rememberScrollState())) {
                        AppHeader()
                        DeviceDetails(mainViewModel)
                        RequestLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        ImagePicker {
                            mainViewModel.setLatestCapturedImageUri(it)
                        }
                        AppInfoTextView(timeInfo.value)
                        TimerApp(countTimeViewModel)
                    }
                    CaptureBtn(modifier = Modifier) {
                        scope.launch {
                            deviceDetails.value = mainViewModel.captureDetails(context = context)
                            Toast.makeText(context,"Capturing & and Saving to Cloud", Toast.LENGTH_SHORT).show()
                            mainViewModel.saveDeviceDetails()
                        }
                    }
                }
            }
        } // setContent
    }

    @Composable
    private fun AppInfoTextView(timeInfo: String) {
        Text(
            fontSize = 12.sp,
            text = "This App Automatically Captures Data Every ${timeInfo} Minutes",
            color = Color.White.copy(alpha = .5f),
            modifier = Modifier.padding(8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    override fun onStart() {
        super.onStart()
        val builder = NetworkRequest.Builder()
        networkCallback?.let { connectivityManager?.registerNetworkCallback(builder.build(), it) }
    }

    override fun onStop() {
        super.onStop()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Denied for Camera Access", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppHeader() {
    Column {
        Text("Satyansh", style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold))
        Text("Assessment" ,style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Light))
    }
}
