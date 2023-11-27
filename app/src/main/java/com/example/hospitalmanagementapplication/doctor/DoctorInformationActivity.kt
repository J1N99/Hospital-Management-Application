package com.example.hospitalmanagementapplication.doctor

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.hospitalmanagementapplication.databinding.ActivityCreatedoctorinformationBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.doctorInformation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.HomeActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.model.Illness
import com.example.hospitalmanagementapplication.model.department
import com.example.hospitalmanagementapplication.userDetailsActivity
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class DoctorInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatedoctorinformationBinding
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var uploadButton: Button
    private var itemSelected: Any=""
    private var departmentItemSelected: Any=""
    private lateinit var bottomNavigationView: BottomNavigationView

    private var selectedImageUri: Uri? = null
    private var imageFileName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatedoctorinformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        imageView = binding.profileImageView
        selectImageButton = binding.selectImagebtn
        uploadButton = binding.uploadimagebtn


        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
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
                    this@DoctorInformationActivity,
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
                itemSelected = allHospital[position].documentId

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
                    this@DoctorInformationActivity,
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

            }


        val userId = getCurrentUserId()
        firestore().getDoctorInfo(this, userId) { doctorInfo ->
            if (doctorInfo != null) {
                binding.uploadimagebtn.text = "Edit information"


                binding.qualificationET.setText(doctorInfo.quanlification)
                binding.qualificationET.isEnabled = false

                binding.autoCompleteTextView.isEnabled = false
                binding.departmentAutoCompleteTextView.isEnabled = false

                itemSelected = doctorInfo.hospital
                departmentItemSelected = doctorInfo.department
                firestore().getHospitalDetails(this, doctorInfo.hospital) { hospital ->
                    if (hospital != null) {
                        autoComplete.setText(hospital.hospital, false)


                        firestore().getDepartmentDetails(
                            this,
                            doctorInfo.department
                        ) { department ->
                            if (department != null) {
                                departmentAutoComplete.setText(department.department, false)


                            } else {
                                Toast.makeText(this, "Department is null", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "Hospital is null", Toast.LENGTH_SHORT).show()
                    }
                }


                binding.autoCompleteTextView.setText(doctorInfo.hospital)
                binding.departmentAutoCompleteTextView.setText(doctorInfo.department)



                binding.selectImagebtn.visibility = View.GONE
                binding.imageInfo.visibility = View.GONE

                val imagefile = doctorInfo.profileImageUri
                val imagePath = "doctorProfileImages/$imagefile"
                // Initialize Firebase Storage
                val storage = Firebase.storage

                // Reference to the image in Firebase Storage (replace "your-image-path" with the actual path)
                val storageRef = storage.reference.child(imagePath)

                // Determine the file extension based on the MIME type
                val fileExtension =
                    getFileExtension(imagefile!!.toUri()) // Pass the URL of the downloaded image
                val localFile = File.createTempFile("images", ".$fileExtension")

                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Handle the successful download here
                    // You can set the retrieved image to an ImageView using Glide
                    val downloadedUri = uri.toString() // The URL to the downloaded image
                    loadAndDisplayImage(downloadedUri)
                }.addOnFailureListener { e ->
                    // Handle any errors that occurred during the download
                    // e.g., handle network errors or file not found errors
                }

            } else {
                Log.d("Fail", "Fail")
                Toast.makeText(this, "Fail", Toast.LENGTH_SHORT)
            }
        }




        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        uploadButton.setOnClickListener {

            if (binding.uploadimagebtn.text == "Save Information") {

                var doctorDepartment = departmentItemSelected
                var Quanlification = binding.qualificationET.text.toString().trim()
                var hospitalID = itemSelected

                if (doctorDepartment == null || Quanlification.isEmpty() || selectedImageUri == null || hospitalID == null) {
                    Toast.makeText(
                        this,
                        "Please enter all the fields or please select the image",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    uploadImageToFirestore(selectedImageUri!!)
                }

            } else if (binding.uploadimagebtn.text == "Edit information") {
                binding.uploadimagebtn.text = "Save Edit"
                binding.departmentAutoCompleteTextView.isEnabled = true
                binding.qualificationLayout.boxStrokeColor = Color.BLACK
                binding.qualificationET.isEnabled = true

                binding.autoCompleteTextView.isEnabled = true
                binding.selectImagebtn.visibility = View.VISIBLE
                binding.imageInfo.visibility = View.VISIBLE
            } else if (binding.uploadimagebtn.text == "Save Edit") {
                var doctorDepartment = departmentItemSelected
                var Quanlification = binding.qualificationET.text.toString().trim()
                var hospitalID = itemSelected
                if (doctorDepartment == null || Quanlification.isEmpty() || hospitalID == null) {
                    Toast.makeText(
                        this,
                        "Please enter all the fields or please select the image",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (selectedImageUri != null) {
                        uploadUpdateImageToFirestore(selectedImageUri!!)
                    } else {
                        val currentUser = firebaseAuth.currentUser
                        val userId = currentUser?.uid
                        if (userId != null) {
                            val department = departmentItemSelected.toString()
                            val quanlification = binding.qualificationET.text.toString()
                            val hospitalID = itemSelected.toString()
                            Log.e(hospitalID, hospitalID)

                            val dataToUpdate = mapOf(
                                "department" to department,
                                "quanlification" to quanlification,
                                "hospital" to hospitalID
                            )

                            firestore().updateDocument("doctorInformation", userId, dataToUpdate)
                            showDialogSuccessUpdate()
                        }
                    }
                }
            }


        }
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val mimeType = contentResolver.getType(uri)
                    if (mimeType != null && (mimeType.startsWith("image/jpeg") || mimeType.startsWith(
                            "image/png"
                        ) || mimeType.startsWith("image/jpg"))
                    ) {
                        selectedImageUri = uri
                        imageView.setImageURI(uri)
                    } else {
                        Toast.makeText(
                            this,
                            "Please select a JPG, JPEG, or PNG image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        imagePicker.launch(intent)
    }

    private fun uploadImageToFirestore(imageUri: Uri) {
        // Get the current user's ID (replace with your authentication logic)
        val userId = getCurrentUserId()

        // Extract the file extension from the image URI
        val fileExtension = getFileExtension(imageUri)

        if (fileExtension != null) {
            // Format the current date and time
            val currentDateTime =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Combine user ID, current date/time, and file extension to create the image file name
            imageFileName = "$userId-$currentDateTime.$fileExtension"

            val imageRef = storageReference.child("doctorProfileImages/$imageFileName")

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()
                        saveImageUrlToFirestore(imageUrl)

                        val currentUser = firebaseAuth.currentUser
                        val userId = currentUser?.uid
                        if (userId != null) {
                            val department = departmentItemSelected.toString()
                            val quanlification = binding.qualificationET.text.toString()
                            val hospitalID = itemSelected.toString()
                            val filename = imageFileName

                            val doctorInfo =
                                doctorInformation(
                                    userId,
                                    department,
                                    quanlification,
                                    filename,
                                    hospitalID
                                )
                            firestore().createDoctorInformation(this, doctorInfo)
                            showDialogSuccessCreate()
                        }
                    }.addOnFailureListener { e ->
                        // Handle the failure
                    }
                }
                .addOnFailureListener { e ->
                    // Handle the upload failure
                }
        } else {
            Toast.makeText(this, "Unable to determine file extension", Toast.LENGTH_SHORT).show()
        }
    }


    private fun uploadUpdateImageToFirestore(imageUri: Uri) {
        // Get the current user's ID (replace with your authentication logic)
        val userId = getCurrentUserId()

        // Extract the file extension from the image URI
        val fileExtension = getFileExtension(imageUri)

        if (fileExtension != null) {
            // Format the current date and time
            val currentDateTime =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Combine user ID, current date/time, and file extension to create the image file name
            imageFileName = "$userId-$currentDateTime.$fileExtension"

            val imageRef = storageReference.child("doctorProfileImages/$imageFileName")

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()
                        saveImageUrlToFirestore(imageUrl)

                        val currentUser = firebaseAuth.currentUser
                        val userId = currentUser?.uid
                        if (userId != null) {
                            val department = departmentItemSelected.toString()
                            val quanlification = binding.qualificationET.text.toString()
                            val filename = imageFileName
                            val hospitalID = itemSelected.toString()


                            val dataToUpdate = mapOf(
                                "department" to department,
                                "quanlification" to quanlification,
                                "profileImageUri" to filename,
                                "hospital" to hospitalID
                            )

                            firestore().updateDocument("doctorInformation", userId, dataToUpdate)
                            showDialogSuccessUpdate()
                        }
                    }.addOnFailureListener { e ->
                        // Handle the failure
                    }
                }
                .addOnFailureListener { e ->
                    // Handle the upload failure
                }
        } else {
            Toast.makeText(this, "Unable to determine file extension", Toast.LENGTH_SHORT).show()
        }
    }


    // Function to extract file extension from a Uri
    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val data = hashMapOf("imageUrl" to imageUrl)
        firestore.collection("images")
            .add(data)
            .addOnSuccessListener {
                // Image URL successfully saved to Firestore
            }
            .addOnFailureListener { e ->
                // Handle the failure
            }
    }

    private fun getCurrentUserId(): String {
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        return user?.uid ?: ""
    }

    private fun loadAndDisplayImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .into(imageView) // Assuming 'imageView' is the target ImageView where you want to display the image
    }


    private fun showDialogSuccessUpdate() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = "The information have updated!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail = dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility = View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, DoctorHomeActivity::class.java)
            startActivity(intent)
        }

        // Add a listener to handle the user's action when dismissing the dialog
        dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            // You can take further action here if needed
            Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show()
        })

        dialog.show()
    }


    private fun showDialogSuccessCreate() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = "The information have created!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail = dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility = View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, DoctorHomeActivity::class.java)
            startActivity(intent)
        }

        // Add a listener to handle the user's action when dismissing the dialog
        dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            // You can take further action here if needed
            Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show()
        })

        dialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, DoctorInformationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
