package com.example.hospitalmanagementapplication.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.HomeActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.SignInActivity
import com.example.hospitalmanagementapplication.clerk.ClerkDashboardActivity
import com.example.hospitalmanagementapplication.databinding.ActivityDoctorhomeBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

class DoctorHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorhomeBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userPosition:Number
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityDoctorhomeBinding.inflate(layoutInflater)
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
            val userId = currentUser?.uid


            if (userId != null) {
                val db = FirebaseFirestore.getInstance()
                val doctorInfoRef = db.collection("doctorInformation").document(userId)

                doctorInfoRef.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document.exists()) {
                            //do nothing here
                        } else {
                            startActivity(Intent(this, DoctorInformationActivity::class.java))
                            finish() // Optionally, finish the current activity to prevent going back.
                        }
                    } else {
                        // Handle errors in accessing Firestore here
                        Log.e("error", "firestore error here")
                    }
                }
            }



            firestore().getAnnouncement { announcementData ->
                if (announcementData != null) {
                    // Handle the announcement data here
                    binding.titleAnnouncement.text =
                        announcementData["announcementTitle"].toString()
                    binding.descriptionAnnouncement.text =
                        announcementData["announcement"].toString()
                }
            }

            if (currentUser != null) {
                firestore().getUserDetails(this) { user ->
                    if (user != null) {
                        binding.welcomeText.text = "Hi " + user.firstname + " " + user.lastname
                    } else {
                        Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
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
        }
    }