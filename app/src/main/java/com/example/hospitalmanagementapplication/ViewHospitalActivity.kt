package com.example.hospitalmanagementapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hospitalmanagementapplication.databinding.ActivityRedesignBinding
import com.example.hospitalmanagementapplication.databinding.ActivityViewhospitalBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import java.io.IOException
import java.util.*

class ViewHospitalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewhospitalBinding
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
        binding = ActivityViewhospitalBinding.inflate(layoutInflater)
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



        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterHospitalCard(query)
            }
        })
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
        val wazeAndGoogleMap = cardView.findViewById<ImageView>(R.id.iconImageView)

        // Display distance in the card view
        totalkm.text = "${distance} KM"
        typeOfModel.text = privateGovernment
        nameOfHospital.text = hospital
        addressDetails.text = address

        wazeAndGoogleMap.setOnClickListener {
            // Build an AlertDialog to let the user choose between Waze and Google Maps
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle("Select Navigation App")
            dialogBuilder.setMessage("Choose a navigation app:")
            dialogBuilder.setPositiveButton("Waze") { dialog, which ->
                openWazeNavigation(address ?: "")
            }
            dialogBuilder.setNegativeButton("Google Maps") { dialog, which ->
                openGoogleMapsNavigation(address ?: "")
            }
            dialogBuilder.setNeutralButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

            // Show the AlertDialog
            val dialog = dialogBuilder.create()
            dialog.show()
        }


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

    private fun openWazeNavigation(address: String) {
        val wazePackage = "com.waze"
        val wazeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?q=$address&navigate=yes"))
        wazeIntent.setPackage(wazePackage)

        val packageManager = packageManager // Assuming you have access to the PackageManager

        try {
            startActivity(wazeIntent)
        } catch (e: ActivityNotFoundException) {
            // Waze is not installed, you can redirect the user to download it from the Play Store
            val playStoreUri = Uri.parse("market://details?id=$wazePackage")
            val intent = Intent(Intent.ACTION_VIEW, playStoreUri)
            startActivity(intent)
        }
    }


    private fun openGoogleMapsNavigation(address: String) {
        val uri = "http://maps.google.com/maps?q=$address"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

    fun filterHospitalCard(query: String) {
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        for (i in 0 until cardContainer.childCount) {
            val cardView = cardContainer.getChildAt(i)
            val nameOfHospital = cardView.findViewById<TextView>(R.id.nameOfHospital)

            val nameHospital = nameOfHospital.text.toString()

            if (nameHospital.contains(query, ignoreCase = true)) {
                cardView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
            }
        }
    }
}