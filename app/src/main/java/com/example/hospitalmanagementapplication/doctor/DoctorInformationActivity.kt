package com.example.hospitalmanagementapplication.doctor

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
        val userId = getCurrentUserId()
        firestore().getDoctorInfo(this,userId) {doctorInfo ->
            if (doctorInfo != null) {
                binding.uploadimagebtn.text = "Edit information"
                binding.departmentET.setText( doctorInfo.department)
                binding.departmentET.isEnabled = false

                binding.qualificationET.setText(doctorInfo.quanlification)
                binding.qualificationET.isEnabled = false

                binding.selectImagebtn.visibility= View.GONE
                binding.imageInfo.visibility=View.GONE

                val imagefile=doctorInfo.profileImageUri
                val imagePath="doctorProfileImages/$imagefile"
                // Initialize Firebase Storage
                val storage = Firebase.storage

                // Reference to the image in Firebase Storage (replace "your-image-path" with the actual path)
                val storageRef = storage.reference.child(imagePath)

                // Determine the file extension based on the MIME type
                val fileExtension = getFileExtension(imagefile!!.toUri()) // Pass the URL of the downloaded image
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

            }else
            {
                Log.d("Fail","Fail")
                Toast.makeText(this,"Fail", Toast.LENGTH_SHORT)
            }
        }




        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        uploadButton.setOnClickListener {

            if(binding.uploadimagebtn.text=="Save Information")
            {

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
            else if(binding.uploadimagebtn.text=="Edit information")
            {
                binding.uploadimagebtn.text = "Save Edit"
                binding.departmentET.isEnabled = true
                binding.departmentLayout.boxStrokeColor= Color.BLACK
                binding.qualificationLayout.boxStrokeColor= Color.BLACK
                binding.qualificationET.isEnabled = true
                binding.selectImagebtn.visibility= View.VISIBLE
                binding.imageInfo.visibility= View.VISIBLE
            }
            else if(binding.uploadimagebtn.text=="Save Edit")
            {
                var doctorDepartment=binding.departmentET.text.toString().trim()
                var Quanlification=binding.qualificationET.text.toString().trim()
                if (doctorDepartment.isEmpty() && Quanlification.isEmpty() &&selectedImageUri != null) {
                    Toast.makeText(this, "Please enter all the fields or please select the image", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    uploadUpdateImageToFirestore(selectedImageUri!!)
                }
            }


        }
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
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
                                doctorInformation(userId, department, quanlification, filename)
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
                            val department = binding.departmentET.text.toString()
                            val quanlification = binding.qualificationET.text.toString()
                            val filename = imageFileName





                                val dataToUpdate = mapOf(
                                    "department" to department,
                                    "quanlification" to quanlification,
                                    "profileImageUri" to filename,
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
        textViewMessage.text ="The information have updated!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail=dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility= View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, HomeActivity::class.java)
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
        textViewMessage.text ="The information have created!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail=dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility= View.GONE
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

}
