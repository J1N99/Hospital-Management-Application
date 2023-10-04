package com.example.hospitalmanagementapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.databinding.ActivityAlluserBinding
import com.example.hospitalmanagementapplication.databinding.ActivitySelectdoctorBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SelectDoctorActivity :AppCompatActivity() {
    private lateinit var binding: ActivitySelectdoctorBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressDialog: Loader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectdoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()




        // Find the EditText for the search bar
        val searchBarText = findViewById<EditText>(R.id.searchBarText)

        // Add a TextWatcher to listen for text changes in the search bar
        searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter doctors based on the search query
                val query = s.toString().trim().toLowerCase()
                    filterDoctors(query)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })


        // Call the getAllDoctor function to retrieve the list of doctors
        firestore().getAllDoctorFromDoctorInfo { doctorList ->
            // Here, you have access to the list of doctors retrieved from Firestore
            // You can use this list to create card views or update your UI
            for (doctor in doctorList) {
                // Extract doctor information
                val doctorId = doctor.userID
                // Create card view or update UI here
                createCardView(doctorId)
            }
        }


        /*
        binding.doctorListview.setOnItemClickListener { parent, view, position, id ->
            firestore().getAllDoctor { userList ->

                val selectedDoctorId = userList[position].id
                val intent = Intent(this, BookingActivity::class.java)
                intent.putExtra("doctorID", selectedDoctorId)
                startActivity(intent)


            }
        }

         */
    }


    fun createCardView(doctorId:String?) {
        Log.e("Run","run")
        // Find the LinearLayout within the ConstraintLayout
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        // Inflate your card view layout here (e.g., from XML)
        val cardView = LayoutInflater.from(this).inflate(R.layout.doctorlist_card_view, null)

        // Bind data to card view elements (TextViews, etc.)
        val hospital = cardView.findViewById<TextView>(R.id.hospital)
        val department = cardView.findViewById<TextView>(R.id.department)
        val qualification = cardView.findViewById<TextView>(R.id.qualification)
        val name=cardView.findViewById<TextView>(R.id.doctorName)
        val bookAppointment = cardView.findViewById<TextView>(R.id.bookingAppointment)


        bookAppointment.setOnClickListener{
            val intent = Intent(this, BookingActivity::class.java)
            intent.putExtra("doctorID", doctorId)
            startActivity(intent)

        }
        // Create a new ImageView for each card view
        val imageView = cardView.findViewById<ImageView>(R.id.imageView)

        firestore().getDoctorInfo(this, doctorId ?: "") { doctorInfo ->
            if (doctorInfo != null) {
                department.text="Department: "+doctorInfo.department
                hospital.text="Hospital: "+doctorInfo.hospital
                qualification.text="Qualification: "+doctorInfo.quanlification
                firestore().getOtherUserDetails(this, doctorId?:"") { user ->
                    if (user != null) {
                       val doctorName =user.firstname + " " + user.lastname
                        Log.d("3", "$doctorName")
                        name.text = "Doctor Name:DR $doctorName"
                    }
                }

                val imagefile = doctorInfo.profileImageUri
                val imagePath = "doctorProfileImages/$imagefile"
                // Initialize Firebase Storage
                val storage = Firebase.storage

                // Reference to the image in Firebase Storage (replace "your-image-path" with the actual path)
                val storageRef = storage.reference.child(imagePath)

                // Determine the file extension based on the MIME type
                val fileExtension = getFileExtension(imagefile!!.toUri()) // Pass the URL of the downloaded image
                val localFile = File.createTempFile("images", ".$fileExtension")

                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Handle the successful download here
                    // You can set the retrieved image to the specific ImageView using Glide
                    val downloadedUri = uri.toString() // The URL to the downloaded image
                    loadAndDisplayImage(downloadedUri, imageView)
                }.addOnFailureListener { e ->
                    // Handle any errors that occurred during the download
                    // e.g., handle network errors or file not found errors
                }
            } else {
                Log.d("Fail", "Fail")
                Toast.makeText(this, "Fail", Toast.LENGTH_SHORT)
            }
        }
        // Add the card view to the LinearLayout
        cardContainer.addView(cardView)
    }
    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun loadAndDisplayImage(imageUrl: String, imageView: ImageView) {
        Glide.with(this)
            .load(imageUrl)
            .into(imageView) // Assuming 'imageView' is the target ImageView where you want to display the image
    }



    private fun filterDoctors(query: String) {
        // Find the LinearLayout for card views
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        // Clear existing card views
        cardContainer.removeAllViews()

        // Call the getAllDoctor function to retrieve the list of doctors
        firestore().getAllDoctorFromDoctorInfo { doctorList ->
            // Filter doctors based on the query
            val filteredDoctors = doctorList.filter { doctor ->
                // Get the doctor's full name using getUserFullName function
                val doctorName = getUserFullName(doctor.userID) { fullName ->
                    // Callback function to handle the full name
                    if (fullName.toLowerCase().contains(query)) {
                        // Create card view for the matching doctor
                        createCardView(doctor.userID)
                    }
                }
                // We don't return doctorName here; it's retrieved asynchronously
                // The callback function is used to handle the result
                false
            }
        }
    }


    private fun getUserFullName(userId: String, callback: (String) -> Unit) {
        firestore().getOtherUserDetails(this, userId) { user ->
            if (user != null) {
                val doctorName = "DR " + user.firstname + " " + user.lastname
                callback(doctorName)
            } else {
                // Handle the case when user data is not available
                callback("") // You can return an empty string or handle it differently
            }
        }

    }

}