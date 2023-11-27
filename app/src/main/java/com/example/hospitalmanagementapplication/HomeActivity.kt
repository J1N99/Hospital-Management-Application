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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var requestLauncher:ActivityResultLauncher<String>
    private lateinit var progressDialog: Loader

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }






        val currentUser: FirebaseUser? = firebaseAuth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {

                val userId = currentUser.uid
                val usersRef = firestore.collection("users")

                usersRef.document(userId).get().addOnCompleteListener { innerTask ->


                    if (innerTask.isSuccessful) {
                        val document: DocumentSnapshot? = innerTask.result
                        if (document != null && document.exists()) {

                        } else {
                            val intent = Intent(this, userDetailsActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }






        // Create a periodic work request to run the worker every hour
        val workRequest = PeriodicWorkRequest.Builder(
            AppointmentNotification::class.java,
            1, // Repeat interval
            TimeUnit.HOURS
        ).build()


        // Enqueue the work request with WorkManager
        WorkManager.getInstance(this).enqueue(workRequest)





        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.home);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = Loader(this)

        progressDialog.show()

        firestore().getAnnouncement { announcementData ->
            if (announcementData != null) {
                // Handle the announcement data here
                binding.titleAnnouncement.text = announcementData["announcementTitle"].toString()
                binding.descriptionAnnouncement.text = announcementData["announcement"].toString()

                // Continue with the next Firestore call
                if (currentUser != null) {
                    firestore().getUserDetails(this) { user ->


                        if (user != null) {
                            binding.welcomeText.text = "Hi " + user.firstname + " " + user.lastname
                            progressDialog.dismiss() // Dismiss the loader after the second callback
                        } else {
                            Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    progressDialog.dismiss() // Dismiss the loader if there is no current user
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                }
            } else {
                progressDialog.dismiss() // Dismiss the loader if the announcementData is null
                Toast.makeText(this, "Announcement data is null", Toast.LENGTH_SHORT).show()
            }
        }


        binding.bookingAppointment.setOnClickListener{

            val intent = Intent(this, SelectDoctorActivity::class.java)
            startActivity(intent)
        }
        binding.viewAppointment.setOnClickListener{
            val intent = Intent(this, ViewAppointmentActivity::class.java)
            startActivity(intent)

        }
        binding.logoutButton.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)

        }

        binding.viewHospital.setOnClickListener {
            val intent=Intent(this,ViewHospitalActivity::class.java)
            startActivity(intent)

        }
        binding.viewIllness.setOnClickListener {
            val intent=Intent(this,ViewIllnessActivity::class.java)
            startActivity(intent)

        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                // Call the system method to close the app
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.appTasks.forEach { taskInfo ->
                    taskInfo.finishAndRemoveTask()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

}

