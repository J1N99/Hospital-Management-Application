package com.example.hospitalmanagementapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.databinding.ActivitySelectdoctorBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.doctorInformation
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

class SelectDoctorActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectdoctorBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressDialog: Loader
    private lateinit var recyclerView: RecyclerView
    private lateinit var doctorAdapter: DoctorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectdoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }
        firebaseAuth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        doctorAdapter = DoctorAdapter()
        recyclerView.adapter = doctorAdapter

        val searchBarText = findViewById<EditText>(R.id.searchBarText)
        searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().toLowerCase()
                filterDoctors(query)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })

        firestore().getAllDoctorFromDoctorInfo { doctorList ->
            doctorAdapter.setDoctors(doctorList)
        }
    }

    inner class DoctorAdapter : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {
        private var doctorList: List<doctorInformation> = emptyList()

        fun setDoctors(doctors: List<doctorInformation>) {
            this.doctorList = doctors
            notifyDataSetChanged()
        }
        fun getDoctors(): List<doctorInformation> {
            return doctorList
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.doctorlist_card_view, parent, false)
            return DoctorViewHolder(view)
        }

        override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
            val doctor = doctorList[position]
            holder.bind(doctor)
        }

        override fun getItemCount(): Int {
            return doctorList.size
        }

        inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val hospital = itemView.findViewById<TextView>(R.id.hospital)
            private val department = itemView.findViewById<TextView>(R.id.department)
            private val qualification = itemView.findViewById<TextView>(R.id.qualification)
            private val name = itemView.findViewById<TextView>(R.id.doctorName)
            private val bookAppointment = itemView.findViewById<TextView>(R.id.bookingAppointment)
            private val imageView = itemView.findViewById<ImageView>(R.id.imageView)

            init {
                bookAppointment.setOnClickListener {
                    val doctorId = doctorList[adapterPosition].userID
                    val intent = Intent(this@SelectDoctorActivity, BookingActivity::class.java)
                    intent.putExtra("doctorID", doctorId)
                    startActivity(intent)
                }
            }

            fun bind(doctor: doctorInformation) {
                department.text = "Department: " + doctor.department
                firestore().getHospitalDetails(this@SelectDoctorActivity, doctor.hospital) { hospitals ->
                    if (hospitals != null) {
                        hospital.text = "Hospital: " + hospitals.hospital
                    }
                }

                qualification.text = "Qualification: " + doctor.quanlification
                val userId = doctor.userID
                firestore().getOtherUserDetails(this@SelectDoctorActivity, userId ?: "") { user ->
                    if (user != null) {
                        val doctorName = "DR " + user.firstname + " " + user.lastname
                        name.text = "Doctor Name: $doctorName"
                    }
                }

                val imagefile = doctor.profileImageUri
                val imagePath = "doctorProfileImages/$imagefile"
                val storage = Firebase.storage
                val storageRef = storage.reference.child(imagePath)

                val fileExtension = getFileExtension(imagefile!!.toUri())
                val localFile = File.createTempFile("images", ".$fileExtension")

                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadedUri = uri.toString()
                    loadAndDisplayImage(downloadedUri, imageView)
                }.addOnFailureListener { e ->
                    // Handle any errors that occurred during the download
                    // e.g., handle network errors or file not found errors
                }
            }
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
            .into(imageView)
    }

    private fun filterDoctors(query: String) {
        val filteredDoctors = mutableListOf<doctorInformation>()

        // Access doctorList using the getDoctors() method
        for (doctor in doctorAdapter.getDoctors()) {
            val doctorName = getUserFullName(doctor.userID) { fullName ->
                // Callback function to handle the full name
                if (fullName.toLowerCase().contains(query)) {
                    // Add the doctor to the filtered list
                    filteredDoctors.add(doctor)
                }
            }
        }

        // Set the filtered list of doctors in the adapter to update the RecyclerView
        doctorAdapter.setDoctors(filteredDoctors)
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
