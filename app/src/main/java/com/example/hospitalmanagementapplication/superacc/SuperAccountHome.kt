package com.example.hospitalmanagementapplication.clerk

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.*
import com.example.hospitalmanagementapplication.databinding.ActivityClerkdashboardBinding
import com.example.hospitalmanagementapplication.databinding.ActivitySuperacchomeBinding
import com.example.hospitalmanagementapplication.doctor.DoctorAvailableAppointmentActivity
import com.example.hospitalmanagementapplication.doctor.DoctorDisableAppointmentActivity
import com.example.hospitalmanagementapplication.doctor.DoctorViewAppointment
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SuperAccountHome:AppCompatActivity() {
    private lateinit var binding: ActivitySuperacchomeBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivitySuperacchomeBinding.inflate(layoutInflater)
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
            finish()
        }
        binding.logoutButton.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.addIllness.setOnClickListener{

            val intent = Intent(this, AllIllnessActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.addAnnouncement.setOnClickListener{

            val intent = Intent(this, AddAnnouncement::class.java)
            startActivity(intent)
            finish()
        }
        binding.updatePosition.setOnClickListener {
            val intent = Intent(this, UpdatePositionActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.createAppointment.setOnClickListener {
            val intent = Intent(this, DoctorAvailableAppointmentActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.viewAppointment.setOnClickListener {
            val intent = Intent(this, DoctorViewAppointment::class.java)
            startActivity(intent)
            finish()
        }
        binding.disableAppointment.setOnClickListener {
            val intent = Intent(this, DoctorDisableAppointmentActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.bookingAppointment.setOnClickListener{
            finish()
            val intent = Intent(this, SelectDoctorActivity::class.java)
            startActivity(intent)
        }
        binding.viewAppointmentPatient.setOnClickListener{
            val intent = Intent(this, ViewAppointmentActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.logoutButton.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.viewHospital.setOnClickListener {
            val intent=Intent(this,ViewHospitalActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.viewIllness.setOnClickListener {
            val intent=Intent(this,ViewIllnessActivity::class.java)
            startActivity(intent)
            finish()
        }

    }


}