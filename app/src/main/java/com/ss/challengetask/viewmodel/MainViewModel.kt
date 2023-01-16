package com.ss.challengetask.viewmodel

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ss.challengetask.utils.DeviceDetails
import com.ss.challengetask.utils.getBatteryChargePerc
import com.ss.challengetask.utils.getBatteryChargingState
import com.ss.challengetask.utils.getConnectivityStatus
import com.ss.challengetask.utils.getCurrentLocation
import com.ss.challengetask.utils.getIMEIDeviceId
import com.ss.challengetask.utils.getTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    var capturedImageUri : Uri? = null
    var isUserOnline = false
    var isUploadOnNetworkNeeded : Boolean? = null
    private lateinit var recentDeviceDetails : DeviceDetails
    fun setLatestDeviceDetails(deviceDetails: DeviceDetails){
        recentDeviceDetails = deviceDetails
    }
    fun setUserOnlineOfflineStatus(isOnline : Boolean){
        isUserOnline = isOnline
    }
    fun setLatestCapturedImageUri(uri : Uri){
        Log.d("Firestore", "setLatestCapturedImageUri Firestore ${capturedImageUri}")
        capturedImageUri = uri
    }
    suspend fun captureDetails(context : Context): DeviceDetails {
        val imei = getIMEIDeviceId(context)
        val connectivityStatus = getConnectivityStatus(context)
        val batteryChargingState = getBatteryChargingState(context)
        val batteryChargePerc = getBatteryChargePerc(context)
        var location: Location? = null
        location = getCurrentLocation(context)
        val timestamp = getTimestamp()
        val deviceDetails = DeviceDetails(imei, connectivityStatus, batteryChargingState, batteryChargePerc, location, timestamp)
        setLatestDeviceDetails(deviceDetails = deviceDetails)
        return deviceDetails
    }

    fun saveDeviceDetails() {
        if(!isUserOnline) {
            /* add method */
            isUploadOnNetworkNeeded = true
            return
        }
        if(!::recentDeviceDetails.isInitialized) return
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        // Create a new reference to a file in Firebase Storage
        val imageRef = storage.getReference("images/${recentDeviceDetails.imei}_${recentDeviceDetails.timestamp}")
        // Get the data from the recentDeviceDetails object
        val data = hashMapOf(
            "imei" to recentDeviceDetails.imei,
            "connectivityStatus" to recentDeviceDetails.connectivityStatus,
            "batteryChargingState" to recentDeviceDetails.batteryChargingState,
            "batteryChargePerc" to recentDeviceDetails.batteryChargePerc,
            "location" to recentDeviceDetails.location,
            "timestamp" to recentDeviceDetails.timestamp
        )
        // Check if the imagePath is not null
        if (capturedImageUri != null) {
            // Create a file from the image path
//            val imageFile = Uri.fromFile(File(recentDeviceDetails.imagePath))
            // Upload the image to Firebase Storage
            imageRef.putFile(capturedImageUri!!)
                .addOnSuccessListener {
                    // Get the download URL for the image
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Save the download URL to the data object
                        data["imagePath"] = downloadUrl.toString()
                        Log.d("Firestore", "imagePath Firestore Saving ${downloadUrl}")
                        // Save the data to Firestore
                        db.collection("devices").document(recentDeviceDetails.imei ?: "")
                            .set(data)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Saved Data Model and Image Successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error saving device details", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firebase Storage", "Error uploading image", e)
                }
        } else {
            // Save the data to Firestore
            db.collection("devices").document(recentDeviceDetails.imei ?: "")
                .set(data)
                .addOnSuccessListener {
                    Log.d("Firestore", "Saved Data Model Only")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error saving device details", e)
                }
        }
    }
}