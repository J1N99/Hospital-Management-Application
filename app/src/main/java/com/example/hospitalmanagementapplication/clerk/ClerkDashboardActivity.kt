package com.example.hospitalmanagementapplication.clerk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.SelectDoctorActivity
import com.example.hospitalmanagementapplication.SignInActivity
import com.example.hospitalmanagementapplication.databinding.ActivityClerkdashboardBinding
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

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser



        binding.titleAnnouncement.text = "Announcement For All the User" //todo make the annocument flexible
        binding.descriptionAnnouncement.text = "influenza  A,B and Kawasaki are high rised in Penang. Please take care of it"


        if (currentUser == null)
        {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }


        binding.addHospital.setOnClickListener{

            val intent = Intent(this, addHospital::class.java)
            startActivity(intent)
            finish()
        }



    }


}