package com.example.wavesoffood

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wavesoffood.databinding.ActivityChooseLocationBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.*

class ChooseLocation : AppCompatActivity() {

    private val binding: ActivityChooseLocationBinding by lazy {
        ActivityChooseLocationBinding.inflate(layoutInflater)
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var locationCallback: LocationCallback

    private val client = OkHttpClient()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Fetching your location...")
        progressDialog.setCancelable(false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }

        locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        binding.listoflocation.setOnItemClickListener { parent, _, position, _ ->
            val selectedLocation = parent.getItemAtPosition(position).toString()
            saveLocationToFirebase(selectedLocation)
        }

        binding.listoflocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let {
                    if (it.length > 2) fetchLocationSuggestions(it)
                }
            }
        })

        askLocationPermission()
    }

    private fun askLocationPermission() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                checkIfGPSEnabled()
            } else {
                Toast.makeText(this, "Location permission denied. Choose manually.", Toast.LENGTH_SHORT).show()
            }
        }

        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun checkIfGPSEnabled() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { getCurrentLocation() }
            .addOnFailureListener { showGPSDialog() }
    }

    private fun showGPSDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("To provide better service, please enable:\n\n• Device Location\n• High Accuracy Mode")
            .setPositiveButton("Turn On") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No Thanks") { _, _ ->
                Toast.makeText(this, "Please select location manually", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        progressDialog.setMessage("Getting your current location...")
        progressDialog.show()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    try {
                        val geocoder = Geocoder(this@ChooseLocation, Locale.getDefault())
                        val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addressList.isNullOrEmpty()) {
                            val fullAddress = addressList[0].getAddressLine(0) ?: "Unknown Address"
                            binding.listoflocation.setText(fullAddress, false)
                            saveLocationToFirebase(fullAddress)
                        } else {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@ChooseLocation, "Address not found", Toast.LENGTH_SHORT).show()
                                progressDialog.dismiss()
                            }
                        }
                    } catch (e: Exception) {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this@ChooseLocation, "Error getting address", Toast.LENGTH_SHORT).show()
                            progressDialog.dismiss()
                        }
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun fetchLocationSuggestions(query: String) {
        progressDialog.setMessage("Fetching location suggestions...")
        progressDialog.show()

        val url = "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(query, "UTF-8")}&format=json"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WavesOfFoodApp")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                        Toast.makeText(this@ChooseLocation, "Error fetching suggestions", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed && responseBody != null) {
                        progressDialog.dismiss()
                        val suggestions = mutableListOf<String>()
                        val jsonArray = JSONArray(responseBody)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            suggestions.add(obj.getString("display_name"))
                        }

                        val adapter = ArrayAdapter(
                            this@ChooseLocation,
                            android.R.layout.simple_list_item_1,
                            suggestions
                        )

                        binding.listoflocation.setAdapter(adapter)
                        try {
                            if (binding.listoflocation.windowToken != null) {
                                binding.listoflocation.showDropDown()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    private fun saveLocationToFirebase(location: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().reference.child("user").child(uid)
            userRef.child("location").setValue(location)
                .addOnSuccessListener {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Location: $location", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to save location", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
