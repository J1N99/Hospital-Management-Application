package com.example.hospitalmanagementapplication.doctor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

class DoctorInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatedoctorinformationBinding
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var uploadButton: Button

    private var selectedImageUri: Uri? = null
    private var imageFileName=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatedoctorinformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        imageView = binding.profileImageView
        selectImageButton = binding.selectImagebtn
        uploadButton = binding.uploadimagebtn

        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        uploadButton.setOnClickListener {

            var doctorDepartment=binding.departmentET.text.toString().trim()
            var Quanlification=binding.qualificationET.text.toString().trim()


            if (doctorDepartment.isEmpty() && Quanlification.isEmpty() &&selectedImageUri != null) {
                Toast.makeText(this, "Please enter all the fields or please select the image", Toast.LENGTH_SHORT).show()
            }
            else
            {
                uploadImageToFirestore(selectedImageUri!!)
            }


        }
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val mimeType = contentResolver.getType(uri)
                    if (mimeType != null && (mimeType.startsWith("image/jpeg") || mimeType.startsWith("image/png")||mimeType.startsWith("image/jpg"))) {
                        selectedImageUri = uri
                        imageView.setImageURI(uri)
                    } else {
                        Toast.makeText(this, "Please select a JPG, JPEG, or PNG image", Toast.LENGTH_SHORT).show()
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
                            val department = binding.departmentET.text.toString()
                            val quanlification = binding.qualificationET.text.toString()
                            val filename = imageFileName
                            val doctorInfo =
                                doctorInformation(userId, department, quanlification, filename, hospital = null)
                            firestore().createDoctorInformation(this, doctorInfo)
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
}
