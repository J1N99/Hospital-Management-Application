package com.example.hospitalmanagementapplication

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityMainBinding
import com.example.hospitalmanagementapplication.databinding.ActivityRedesignBinding
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class RedesignActivity: AppCompatActivity() {
    private lateinit var binding: ActivityRedesignBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityRedesignBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.home);
        IntentManager(this, bottomNavigationView)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser

        binding.titleAnnouncement.text = "Announcement For All the User" //todo make the annocument flexible
        binding.descriptionAnnouncement.text = "influenza  A,B and Kawasaki are high rised in Penang. Please take care of it"


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

