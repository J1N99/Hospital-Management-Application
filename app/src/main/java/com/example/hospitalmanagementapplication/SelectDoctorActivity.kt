package com.example.hospitalmanagementapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.databinding.ActivitySelectdoctorBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.model.department
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
    private lateinit var hospitalItemSelected: Any
    private lateinit var departmentItemSelected: Any
    private var currentFilterType: Int = 0

    private var originalDoctorList: List<doctorInformation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectdoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
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
                filterDoctors(query,1)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })

        firestore().getAllDoctorFromDoctorInfo { doctorList ->
            doctorAdapter.setDoctors(doctorList)
            originalDoctorList = doctorAdapter.getDoctors()
        }

        binding.resetFilter.setOnClickListener {
            resetDoctorList()
        }

        val autoComplete: AutoCompleteTextView = findViewById(R.id.hospitalAutoCompleteTextView)
        val allHospital: MutableList<Hospital> = mutableListOf()

        // Initialize an empty adapter for now
        val adapter = ArrayAdapter(this, R.layout.list_private_government, listOf<String>())
        autoComplete.setAdapter(adapter)

        firestore().getAllHospital { fetchHospital ->
            // Populate the allIllness list with data from Firestore
            allHospital.clear() // Clear the list to remove any existing data
            allHospital.addAll(fetchHospital)

            // Create the adapter with all illnesses
            val initialAdapter = ArrayAdapter(
                this,
                R.layout.list_private_government,
                allHospital.map { it.hospital })

            // Set the adapter for the AutoCompleteTextView
            autoComplete.setAdapter(initialAdapter)
        }

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on user input
                val filteredIllnesses =
                    allHospital.filter { it.hospital.contains(s.toString(), ignoreCase = true) }

                if (filteredIllnesses.isEmpty()) {
                    // No results found, clear the text
                    autoComplete.text = null
                }

                // Update the adapter with filtered results
                val filteredAdapter = ArrayAdapter(
                    this@SelectDoctorActivity,
                    R.layout.list_private_government,
                    filteredIllnesses.map { it.hospital })
                autoComplete.setAdapter(filteredAdapter)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used in this case
            }
        })

        autoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                hospitalItemSelected = allHospital[position].documentId
                autoComplete.setAdapter(null)
                filterDoctors(hospitalItemSelected.toString(),2)
            }





        val departmentAutoComplete: AutoCompleteTextView =
            findViewById(R.id.departmentAutoCompleteTextView)
        val allDepartment: MutableList<department> = mutableListOf()

        // Initialize an empty adapter for now
        val departmentAdapter =
            ArrayAdapter(this, R.layout.list_private_government, listOf<String>())
        departmentAutoComplete.setAdapter(departmentAdapter)

        firestore().getAllDepartment { fetchDepartment ->
            // Populate the allIllness list with data from Firestore
            allDepartment.clear() // Clear the list to remove any existing data
            allDepartment.addAll(fetchDepartment)

            // Create the adapter with all illnesses
            val initialAdapter = ArrayAdapter(
                this,
                R.layout.list_private_government,
                allDepartment.map { it.department })

            // Set the adapter for the AutoCompleteTextView
            departmentAutoComplete.setAdapter(initialAdapter)
        }

        departmentAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on user input
                val filteredDepartment =
                    allDepartment.filter { it.department.contains(s.toString(), ignoreCase = true) }

                if (filteredDepartment.isEmpty()) {
                    // No results found, clear the text
                    departmentAutoComplete.text = null
                }

                // Update the adapter with filtered results
                val filteredAdapter = ArrayAdapter(
                    this@SelectDoctorActivity,
                    R.layout.list_private_government,
                    filteredDepartment.map { it.department })
                departmentAutoComplete.setAdapter(filteredAdapter)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used in this case
            }
        })

        departmentAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                departmentItemSelected = allDepartment[position].documentID
                departmentAutoComplete.setAdapter(null)
                filterDoctors(departmentItemSelected.toString(),3)

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
                firestore().getDepartmentDetails(this@SelectDoctorActivity, doctor.department) { departments ->
                    if (departments != null) {

                        department.text = "Department: " + departments.department+"\n"
                    }
                }




                firestore().getHospitalDetails(
                    this@SelectDoctorActivity,
                    doctor.hospital
                ) { hospitals ->
                    if (hospitals != null) {
                        hospital.text = "Hospital: " + hospitals.hospital +"\n"
                    }
                }

                qualification.text = "Qualification: " + doctor.quanlification+"\n"
                val userId = doctor.userID
                firestore().getOtherUserDetails(this@SelectDoctorActivity, userId ?: "") { user ->
                    if (user != null) {
                        val doctorName = "DR " + user.firstname + " " + user.lastname
                        name.text = "Doctor Name: $doctorName\n"
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

    private fun filterDoctors(query: String, type: Int) {
        currentFilterType = type
        val doctorList = if (type == 0) {
            originalDoctorList // If type is 0, use the original doctor list
        } else {
            doctorAdapter.getDoctors() // Otherwise, use the current doctor list
        }

        val filteredDoctors = mutableListOf<doctorInformation>()
        var callbacksCompleted = 0

        for (doctor in doctorList) {
            when (type) {
                1 -> {
                    getUserFullName(doctor.userID) { fullName ->
                        callbacksCompleted++
                        if (fullName.toLowerCase().contains(query)) {
                            // Add the doctor to the filtered list
                            filteredDoctors.add(doctor)
                        }

                        if (callbacksCompleted == doctorList.size) {
                            // All callbacks have completed, update the RecyclerView
                            doctorAdapter.setDoctors(filteredDoctors)
                        }
                    }
                }
                2 -> {
                    callbacksCompleted++
                    if (doctor.hospital == query) {
                        filteredDoctors.add(doctor)
                    }

                    if (callbacksCompleted == doctorList.size) {
                        // All callbacks have completed, update the RecyclerView
                        doctorAdapter.setDoctors(filteredDoctors)
                    }
                }
                3 -> {
                    callbacksCompleted++
                    if (doctor.department == query) {
                        filteredDoctors.add(doctor)
                    }

                    if (callbacksCompleted == doctorList.size) {
                        // All callbacks have completed, update the RecyclerView
                        doctorAdapter.setDoctors(filteredDoctors)
                    }
                }
            }
        }
    }

    private fun resetDoctorList() {
        // Reset the filter type and show the original doctor list
        currentFilterType = 0
        doctorAdapter.setDoctors(originalDoctorList)
        binding.searchBarText.text.clear()
        binding.departmentAutoCompleteTextView.text.clear()
        binding.hospitalAutoCompleteTextView.text.clear()
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
