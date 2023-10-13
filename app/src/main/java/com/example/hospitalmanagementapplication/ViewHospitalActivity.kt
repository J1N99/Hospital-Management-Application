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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalmanagementapplication.databinding.ActivityViewhospitalBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException
import java.util.*

class ViewHospitalActivity :AppCompatActivity() {
    private lateinit var binding: ActivityViewhospitalBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var currentAddress = ""
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HospitalAdapter
    private var sortedHospitalList: List<Hospital> = emptyList()
    private lateinit var bottomNavigationView: BottomNavigationView

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

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)



        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

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
            sortedHospitalList = allHospitalList.mapNotNull { hospitalInfo ->
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

            adapter = HospitalAdapter(sortedHospitalList)
            recyclerView.adapter = adapter
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

    fun getCurrentAddress(): String? {
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
        val filteredList = sortedHospitalList.filter { hospitalInfo ->
            hospitalInfo.hospital?.contains(query, ignoreCase = true) == true
        }
        adapter.updateData(filteredList)
    }

    inner class HospitalAdapter(private var hospitalList: List<Hospital>) :
        RecyclerView.Adapter<HospitalAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val typeOfModel: TextView = itemView.findViewById(R.id.typeOfModel)
            val nameOfHospital: TextView = itemView.findViewById(R.id.nameOfHospital)
            val totalKM: TextView = itemView.findViewById(R.id.totalKM)
            val address: TextView = itemView.findViewById(R.id.address)
            val addressDetail: TextView = itemView.findViewById(R.id.addressDetail)
            val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.hospital_card_view, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val hospitalInfo = hospitalList[position]

            holder.typeOfModel.text = hospitalInfo.privateGovernment
            holder.nameOfHospital.text = hospitalInfo.hospital
            holder.totalKM.text = "${hospitalInfo.distance} KM"
            holder.address.text = "Address:" // You can set a more descriptive label if needed
            holder.addressDetail.text = hospitalInfo.address

            // Set an onClickListener for the navigation icon
            holder.iconImageView.setOnClickListener {
                // Build an AlertDialog to let the user choose between Waze and Google Maps
                val dialogBuilder = AlertDialog.Builder(this@ViewHospitalActivity)
                dialogBuilder.setTitle("Select Navigation App")
                dialogBuilder.setMessage("Choose a navigation app:")
                dialogBuilder.setPositiveButton("Waze") { dialog, which ->
                    openWazeNavigation(hospitalInfo.address ?: "")
                }
                dialogBuilder.setNegativeButton("Google Maps") { dialog, which ->
                    openGoogleMapsNavigation(hospitalInfo.address ?: "")
                }
                dialogBuilder.setNeutralButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }

                // Show the AlertDialog
                val dialog = dialogBuilder.create()
                dialog.show()
            }
        }

        override fun getItemCount(): Int {
            return hospitalList.size
        }

        fun updateData(newHospitalList: List<Hospital>) {
            hospitalList = newHospitalList
            notifyDataSetChanged()
        }
    }
}