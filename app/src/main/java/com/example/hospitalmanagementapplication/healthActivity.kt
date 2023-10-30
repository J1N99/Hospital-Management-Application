package com.example.hospitalmanagementapplication

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.hospitalmanagementapplication.databinding.ActivityMainBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import android.os.Build
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import com.example.hospitalmanagementapplication.databinding.ActivityHealthBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class healthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHealthBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var requestLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityHealthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser: FirebaseUser? = firebaseAuth.currentUser
        val userID = currentUser?.uid
        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
            }
        }
        firestore().getHealthReport(this) { healthReportList ->
            if (healthReportList.isNotEmpty()) {
                // Data has been successfully retrieved
                val healthReport = healthReportList[0]
                // Use the healthReport object as needed
                binding.bloodTypeResult.text = healthReport.bloodType
                binding.heightET.text = healthReport.height
                binding.weightInput.text = healthReport.weight
            }

            calculateBMI()
        }

        binding.heightCard.setOnClickListener {
            // Create an EditText to get user input
            val inputEditText = EditText(this)
            // Set an InputFilter to allow only numeric input
            inputEditText.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

            // Create an AlertDialog
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Enter the height in cm")
                .setView(inputEditText)
                .setPositiveButton("OK") { dialog, which ->
                    // Handle the user input here
                    val userInput = inputEditText.text.toString()
                    binding.heightET.text = userInput
                    val dataToUpdate = mapOf(
                        "height" to userInput,
                    )

                    firestore().updateDocument("healthReport", userID ?: "", dataToUpdate)
                    calculateBMI()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    // Handle cancel if needed
                }
                .create()

            alertDialog.show()
        }


        binding.weightCard.setOnClickListener {
            // Create an EditText to get user input
            val inputEditText = EditText(this)
            // Set an InputFilter to allow only numeric input
            inputEditText.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

            // Create an AlertDialog
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Enter the weight in KG")
                .setView(inputEditText)
                .setPositiveButton("OK") { dialog, which ->
                    // Handle the user input here
                    val userInput = inputEditText.text.toString()
                    binding.weightInput.text = userInput
                    val dataToUpdate = mapOf(
                        "weight" to userInput,
                    )

                    firestore().updateDocument("healthReport", userID ?: "", dataToUpdate)
                    calculateBMI()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    // Handle cancel if needed
                }
                .create()

            alertDialog.show()

        }



        binding.bloodTypeCard.setOnClickListener {
            // Define an array of blood types
            val bloodTypes = arrayOf("A", "B", "AB", "O")


            // Create a Spinner to display blood type options
            val bloodTypeSpinner = Spinner(this)
            val paddingInDp = 15 // Desired padding in dp
            val scale = resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale + 0.5f).toInt() // Convert dp to pixels
            bloodTypeSpinner.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bloodTypes)
            bloodTypeSpinner.adapter = adapter


// Create an AlertDialog
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Select your blood type")
                .setView(bloodTypeSpinner)
                .setPositiveButton("OK") { dialog, which ->
                    // Handle the user's blood type selection here
                    val selectedBloodType = bloodTypeSpinner.selectedItem.toString()
                    // Update the UI or perform any other actions with the selected blood type
                    binding.bloodTypeResult.text = selectedBloodType

                    // If you want to store the selected blood type in Firestore, you can do so here
                    val dataToUpdate = mapOf("bloodType" to selectedBloodType)
                    firestore().updateDocument("healthReport", userID ?: "", dataToUpdate)
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    // Handle cancel if needed
                }
                .create()

            alertDialog.show()

        }


    }


    private fun calculateBMI() {
        if (binding.heightET.text.isNotEmpty() && binding.weightInput.text.isNotEmpty()) {
            val heightMeters = binding.heightET.text.toString().toDouble() / 100
            val weightKg = binding.weightInput.text.toString().toDouble()
            val bmi = (weightKg / (heightMeters * heightMeters))
            binding.bmiResult.text = String.format("%.2f", bmi)
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

}

