package com.example.hospitalmanagementapplication.clerk

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.*
import com.example.hospitalmanagementapplication.databinding.ActivityClerkdashboardBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ClerkDashboardActivity:AppCompatActivity() {
    private lateinit var binding: ActivityClerkdashboardBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityClerkdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.home);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser


        firestore().getAnnouncement { announcementData ->
            if (announcementData != null) {
                // Handle the announcement data here
                binding.titleAnnouncement.text= announcementData["announcementTitle"].toString()
                binding.descriptionAnnouncement.text=announcementData["announcement"].toString()
            }
        }


        if (currentUser != null) {
            firestore().getUserDetails(this) { user ->
                if (user != null) {
                    binding.welcomeText.text ="Hi "+ user.firstname+" "+user.lastname
                } else {
                    Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else
        {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }



        binding.addHospital.setOnClickListener{

            val intent = Intent(this, allHospitalActivity::class.java)
            startActivity(intent)

        }
        binding.logoutButton.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)

        }
        binding.addIllness.setOnClickListener{

            val intent = Intent(this, AllIllnessActivity::class.java)
            startActivity(intent)

        }
        binding.addAnnouncement.setOnClickListener{

            val intent = Intent(this, AddAnnouncement::class.java)
            startActivity(intent)

        }
        binding.updatePosition.setOnClickListener {
            val intent = Intent(this, AllUserActivity::class.java)
            startActivity(intent)

        }
        binding.addMedicine.setOnClickListener {
            val intent = Intent(this, AllMedicineActivity::class.java)
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