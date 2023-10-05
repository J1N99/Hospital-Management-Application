package com.example.hospitalmanagementapplication

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
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.marginTop
import com.bumptech.glide.Glide


class ViewAppointmentActivity: AppCompatActivity() {
    private lateinit var binding: ActivityViewappointmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewappointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getAndDisplayAppointments { appointments ->
            // Here, you have access to the list of appointments retrieved from Firestore
            // You can use this list to create card views or update your UI
            for (appointment in appointments) {
                // Create card views or update UI elements with appointment data
                val documentID=appointment.documentID
                val dateAppointment = appointment.dateAppointment
                val doctorId = appointment.doctorId
                val time = appointment.time

                // Create card view or update UI here
                createCardView(documentID,dateAppointment, doctorId, time)
            }
        }


    }

    fun createCardView(documentID: String?, date: String?, doctorId: String?, time: String?) {
        // Find the LinearLayout within the ConstraintLayout
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        // Inflate your card view layout here (e.g., from XML)
        val cardView = LayoutInflater.from(this).inflate(R.layout.appointment_card_view, null)

        // Set margins for the card view to create spacing between them
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.card_margin)) // Adjust the margin as needed
        cardView.layoutParams = layoutParams


        // Bind data to card view elements (TextViews, etc.)
        val dateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val doctorTextView = cardView.findViewById<TextView>(R.id.doctorTextView)
        val timeTextView = cardView.findViewById<TextView>(R.id.timeTextView)
        val cancelAppointment = cardView.findViewById<TextView>(R.id.cancelAppointment)

        // Create a new ImageView for each card view
        val imageView = cardView.findViewById<ImageView>(R.id.imageView)

        firestore().getDoctorInfo(this, doctorId ?: "") { doctorInfo ->
            if (doctorInfo != null) {
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

        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd") // Define the format of your date string
        val dateString = date

        val currentDate = LocalDate.now()
        val date = LocalDate.parse(dateString, dateFormat)

        val daysUntilDate = ChronoUnit.DAYS.between(currentDate, date)

        if (daysUntilDate < 0) {
            cancelAppointment.visibility = View.GONE
        }

        var doctorName = ""
        var doctorID = doctorId ?: ""
        var StringDocumentID = documentID ?: ""
        firestore().getOtherUserDetails(this, doctorID) { user ->
            if (user != null) {
                doctorName = "DR " + user.firstname + " " + user.lastname
                Log.d("3", "$doctorName")
                doctorTextView.text = "Doctor: $doctorName"
            }
        }

        // Set the appointment data to the TextViews
        dateTextView.text = "Date: $date"

        Log.d("4", "$doctorName")
        timeTextView.text = "Time: $time"

        cancelAppointment.setOnClickListener {
            firestore().deleteDocument(StringDocumentID, "appointments",
                onSuccess = {
                    // Create an Intent to restart the current activity
                    val intent = intent
                    finish() // Finish the current activity
                    startActivity(intent) // Start a new instance of the current activity
                },
                onFailure = { e ->
                    Log.w("ERROR", "Error deleting document", e)
                })
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

}

