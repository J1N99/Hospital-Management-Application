package com.example.hospitalmanagementapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.hospitalmanagementapplication.databinding.ActivityViewappointmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.databinding.ActivityRedesignBinding
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.model.doctorInformation
import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import java.io.IOException
import java.util.Locale
class RedesignActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRedesignBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var currentAddress = ""

    data class Hospital(
        val documentID: String?,
        val hospital: String?,
        val address: String?,
        val privateGovernment: String?,
        val distance: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRedesignBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for location permission and get the current address
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            currentAddress = getCurrentAddress() ?: ""
            binding.currentAddress.text = "Current Address: $currentAddress"
        }

        // Fetch hospitals from Firestore and calculate distances
        firestore().getAllHospital { allHospitalList ->
            val sortedHospitalList = allHospitalList.mapNotNull { hospitalInfo ->
                val documentID = hospitalInfo.documentId
                val address = hospitalInfo.address
                val hospital = hospitalInfo.hospital
                val privateGovernment = hospitalInfo.privateGovernment
                val latLng = getLatLngFromAddress(this, address ?: "")

                if (latLng != null) {
                    val currentLatLong = getLatLngFromAddress(this, currentAddress ?: "")
                    val cardLatLong = latLng
                    if (currentLatLong != null) {
                        val (currentLat, currentLong) = currentLatLong.split(",")
                        val (cardLat, cardLong) = cardLatLong.split(",")
                        val distance = calculateDistance(
                            currentLat.toDoubleOrNull() ?: 0.0,
                            currentLong.toDoubleOrNull() ?: 0.0,
                            cardLat.toDoubleOrNull() ?: 0.0,
                            cardLong.toDoubleOrNull() ?: 0.0
                        )
                        Hospital(documentID, hospital, address, privateGovernment, distance)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }.sortedBy { it.distance }

            // Create card views based on sorted hospital list
            sortedHospitalList.forEach { hospitalInfo ->
                createCardView(
                    hospitalInfo.documentID,
                    hospitalInfo.address,
                    hospitalInfo.hospital,
                    hospitalInfo.privateGovernment,
                    hospitalInfo.distance
                )
            }
        }
    }

    fun createCardView(
        documentID: String?,
        address: String?,
        hospital: String?,
        privateGovernment: String?,
        distance: Float
    ) {
        // Find the LinearLayout within the ConstraintLayout
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        // Inflate your card view layout here (e.g., from XML)
        val cardView = layoutInflater.inflate(R.layout.hospital_card_view, null)

        // Set margins for the card view to create spacing between them
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.card_margin)
        ) // Adjust the margin as needed
        cardView.layoutParams = layoutParams

        // Bind data to card view elements (TextViews, etc.)
        val typeOfModel = cardView.findViewById<TextView>(R.id.typeOfModel)
        val nameOfHospital = cardView.findViewById<TextView>(R.id.nameOfHospital)
        val addressDetails = cardView.findViewById<TextView>(R.id.addressDetail)
        val totalkm = cardView.findViewById<TextView>(R.id.totalKM)

        // Display distance in the card view
        totalkm.text = "${distance} KM"
        typeOfModel.text = privateGovernment
        nameOfHospital.text = hospital
        addressDetails.text = address

        // Add the card view to the LinearLayout
        cardContainer.addView(cardView)
    }

    private fun getCurrentAddress(): String? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (location != null) {
                // You have the location, now you can get the address
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses: List<Address>? =
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    return addresses[0].getAddressLine(0)
                }
            }
        }
        return null // Return null if unable to get the address
    }

    fun getLatLngFromAddress(context: Context, addressStr: String): String? {
        val geocoder = Geocoder(context)
        try {
            val addresses: List<Address>? = geocoder.getFromLocationName(addressStr, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude
                return "$latitude, $longitude"
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get current location
                val address = getCurrentAddress()
                binding.currentAddress.text = "Current Address: $address"
            } else {
                // Permission denied, handle the case (e.g., show a message)
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateDistance(
        currentLatitude: Double,
        currentLongitude: Double,
        cardLatitude: Double,
        cardLongitude: Double
    ): Float {
        val currentLocation = Location("currentLocation")
        currentLocation.latitude = currentLatitude
        currentLocation.longitude = currentLongitude

        val cardLocation = Location("cardLocation")
        cardLocation.latitude = cardLatitude
        cardLocation.longitude = cardLongitude

        // Calculate the distance in meters
        val distanceInMeters = currentLocation.distanceTo(cardLocation)

        // Convert meters to kilometers with one decimal place
        val distanceInKilometers = distanceInMeters / 1000.0

        // Format the result to have one decimal place
        val formattedDistance = String.format("%.1f", distanceInKilometers)

        // Parse the formatted distance back to a float
        return formattedDistance.toFloat()
    }
}