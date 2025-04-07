package com.example.location_services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

import android.util.Log
import com.example.location_services.screens.MapScreen
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay

@Composable
fun GetCurrentLocation() {
    val context = LocalContext.current
    var address by remember { mutableStateOf("Fetching address...") }
    var triggerRefresh by remember { mutableStateOf(false) }
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Location", "Permission granted. Fetching location...")
            fetchLocationAndAddress(context, fusedLocationClient) { addressString, latLng ->
                address = addressString ?: "Unable to fetch address"
                userLatLng = latLng
            }

        } else {
            Log.d("Location", "Permission denied by user.")
            address = "Permission denied"
        }
    }

    // React when triggerRefresh changes
    LaunchedEffect(triggerRefresh) {
        Log.d("Location", "LaunchedEffect triggered: $triggerRefresh")
        delay(1000L)
        when (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            PermissionChecker.PERMISSION_GRANTED -> {
                Log.d("Location", "Permission already granted. Fetching location...")
                fetchLocationAndAddress(context, fusedLocationClient) { addressString, latLng ->
                    address = addressString ?: "Unable to fetch address"
                    userLatLng = latLng
                }
            }
            else -> {
                Log.d("Location", "Requesting location permission...")
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Location:\n$address",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.padding(top = 64.dp, bottom = 24.dp)
            )

            Button(onClick = {
                Log.d("Location", "Refresh button clicked")
                address = "Fetching location..."
                triggerRefresh = !triggerRefresh
            }) {
                Text("Refresh Location")
            }
        }
        userLatLng?.let { latLng ->
            MapScreen(userLocation = latLng)
        }

    }
}

@SuppressLint("MissingPermission")
private fun fetchLocationAndAddress(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onAddressFetched: (String?, LatLng?) -> Unit
) {
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location ->
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            getAddressFromLocation(context, it) { address ->
                onAddressFetched("Lat: ${it.latitude}, Lon: ${it.longitude}\n$address", latLng)
            }
        } ?: onAddressFetched("Location not found", null)
    }.addOnFailureListener {
        onAddressFetched("Error fetching location: ${it.message}", null)
    }
}


// Function to convert location to address using Geocoder
private fun getAddressFromLocation(
    context: Context,
    location: Location,
    onAddressFetched: (String?) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())
    val latitude = location.latitude
    val longitude = location.longitude

    try {
        val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
        if (addresses != null) {
            if (addresses.isNotEmpty()) {
                val address = addresses[0].getAddressLine(0)
                onAddressFetched(address)
            } else {
                onAddressFetched("No address found")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onAddressFetched("Error fetching address: ${e.message}")
    }
}