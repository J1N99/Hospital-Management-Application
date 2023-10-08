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
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.model.Appointment


class ViewAppointmentActivity: AppCompatActivity() {
    private lateinit var binding: ActivityViewappointmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewappointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()


        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        appointmentAdapter = AppointmentAdapter()
        recyclerView.adapter = appointmentAdapter

        firestore().getAndDisplayAppointments { appointments ->
            appointmentAdapter.setAppointments(appointments)
        }


    }
    // Define your ViewHolder class for the RecyclerView
    private inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Initialize your views here (e.g., TextViews, ImageView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val doctorTextView: TextView = itemView.findViewById(R.id.doctorTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val imageView:ImageView=itemView.findViewById(R.id.imageView)
        val cancelAppointment:TextView=itemView.findViewById(R.id.cancelAppointment)
    }

    // Define your Adapter class for the RecyclerView
    private inner class AppointmentAdapter : RecyclerView.Adapter<AppointmentViewHolder>() {
        private var appointments: List<Appointment> = emptyList()

        fun setAppointments(appointments: List<Appointment>) {
            this.appointments = appointments
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.appointment_card_view, parent, false)
            return AppointmentViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
            val appointment = appointments[position]
            // Bind data to the ViewHolder views here
            holder.dateTextView.text = "Date: ${appointment.dateAppointment}"
            holder.timeTextView.text = "Time: ${appointment.time}"

            val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd") // Define the format of your date string
            val dateString = appointment.dateAppointment

            val currentDate = LocalDate.now()
            val date = LocalDate.parse(dateString, dateFormat)

            val daysUntilDate = ChronoUnit.DAYS.between(currentDate, date)

            if (daysUntilDate < 0) {
                holder.cancelAppointment.visibility = View.GONE
            }

            holder.cancelAppointment.setOnClickListener {
                firestore().deleteDocument(appointment.documentID?:"", "appointments",
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

            val doctorId = appointment.doctorId
            firestore().getOtherUserDetails(this@ViewAppointmentActivity, appointment.doctorId?:"") { user ->
                if (user != null) {
                    holder.doctorTextView.text = "DR " + user.firstname + " " + user.lastname
                }
            }

            firestore().getDoctorInfo(this@ViewAppointmentActivity, doctorId ?: "") { doctorInfo ->
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
                        loadAndDisplayImage(downloadedUri, holder.imageView) // Modify this line as needed
                    }.addOnFailureListener { e ->
                        // Handle any errors that occurred during the download
                        // e.g., handle network errors or file not found errors
                    }
                } else {
                    Log.d("Fail", "Fail")
                    Toast.makeText(this@ViewAppointmentActivity, "Fail", Toast.LENGTH_SHORT)
                }
            }
        }

        override fun getItemCount(): Int {
            return appointments.size
        }
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

