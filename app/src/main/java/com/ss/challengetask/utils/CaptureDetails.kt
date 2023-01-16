package com.ss.challengetask.utils

import android.Manifest
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter.State.Empty.painter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ss.challengetask.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.internal.wait
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceDetails(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val progressBarVisible = remember { mutableStateOf(false) }
    val deviceDetails = remember { mutableStateOf<DeviceDetails?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(true) {
        progressBarVisible.value = true
        scope.launch {
            deviceDetails.value = mainViewModel.captureDetails(context = context)
            progressBarVisible.value = false
        }
    }
    Column {
        if (progressBarVisible.value) {
            CircularProgressIndicator(modifier = Modifier.padding(all = 16.dp))
        } else {
            DisplayInfoCard("IMEI", deviceDetails.value?.imei  ?: "",com.ss.challengetask.R.drawable.icon_barcode_svg)
            DisplayInfoCard("Connectivity Status", deviceDetails.value?.connectivityStatus  ?: "",com.ss.challengetask.R.drawable.icon_wifi_svg)
            DisplayInfoCard("Battery Charging Status", deviceDetails.value?.batteryChargingState  ?: "",com.ss.challengetask.R.drawable.icon_battery_svg)
            DisplayInfoCard("Battery Charge Percentage", "${deviceDetails.value?.batteryChargePerc}",com.ss.challengetask.R.drawable.icon_charging_svg)
            DisplayInfoCard("Location", ("lat ${deviceDetails.value?.location?.latitude}\nlong:${deviceDetails.value?.location?.longitude}") ?: "",
                com.ss.challengetask.R.drawable.icon_location_svg, if(deviceDetails.value?.location?.latitude == null) "Please Turn on Location to get Info" else null)
            DisplayInfoCard("Timestamp", deviceDetails.value?.timestamp  ?: "",com.ss.challengetask.R.drawable.icon_clock_svg)
        }
    }
}

@Composable
fun DisplayInfoCard(title : String,info: String, iconRes : Int, warningMsg : String? = null) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.padding(2.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column {
            Row(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${info}", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium))
                    Text(text = title,  style = TextStyle(color = Color.White.copy(alpha = .3f), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = iconRes),
                    contentDescription = null ,
                    tint = Color(0xff84b54a)
                )
            }
            if(warningMsg != null) Text(text = "${warningMsg}", modifier = Modifier.padding(start = 10.dp, bottom = 10.dp) ,style = TextStyle(color = Color.Red, fontSize = 12.sp))
        }
    }
}

data class DeviceDetails(val imei: String?,
                         val connectivityStatus: String?,
                         val batteryChargingState: String?,
                         val batteryChargePerc: Int?,
                         val location: Location?,
                         val timestamp: String?,
                         val imagePath: String? = "",
)

fun getIMEIDeviceId(context: Context): String? {
    val deviceId: String
    deviceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    } else {
        val mTelephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return ""
        }
        if (mTelephony.deviceId != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mTelephony.imei
            } else {
                mTelephony.deviceId
            }
        } else {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
    Log.d("deviceId", deviceId)
    return deviceId
}

fun getImei(context: Context): String {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        tm.imei
    } else {
        tm.deviceId
    }
}

fun getConnectivityStatus(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    if (activeNetwork != null) {
        if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
            return "Connected to WiFi"
        } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
            return "Connected to Mobile Data"
        }
    }
    return "Not Connected"
}

fun getBatteryChargingState(context: Context): String {
    val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
    val chargingState = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    return when (chargingState) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        else -> "Unknown"
    }
}

fun getBatteryChargePerc(context: Context): Int {
//    context.getSystemService(BATTERY_SERVICE) as BatteryManager
    val batteryStatusIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatusIntent = context.registerReceiver(null, batteryStatusIntentFilter)
    val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percentage = (level * 100) / scale
    return percentage
}

//suspend fun getCurrentLocation(context: Context): Location? {
//    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//        return null
//    }
//    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
//    val locationTask = fusedLocationClient.lastLocation.getResult().wait
//    return locationTask.result
//}

fun getCurrentLocation(context: Context): Location? {
    val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers: List<String> = locationManager.getProviders(true)
    var location: Location? = null
    for (i in providers.size - 1 downTo 0) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        location= locationManager.getLastKnownLocation(providers[i])
        if (location != null)
            break
    }
     return location
}
fun getTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}