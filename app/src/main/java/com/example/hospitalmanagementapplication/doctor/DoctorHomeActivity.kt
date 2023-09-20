package com.example.hospitalmanagementapplication.doctor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.SignInActivity
import com.example.hospitalmanagementapplication.databinding.ActivityDoctorhomeBinding
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class DoctorHomeActivity  : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorhomeBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {

        //TODO Fix all the warning
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorhomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.home);
        IntentManager(this, bottomNavigationView)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser

        if (currentUser != null) {
            binding.userEmail.text  ="Hello Doctor"
        }
        else
        {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        binding.logoutButton.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.userEmail.setOnClickListener{
            val intent = Intent(this, DoctorAvailableAppoinmentActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.viewAppointment.setOnClickListener{
            val intent = Intent(this, DoctorViewAppointment::class.java)
            startActivity(intent)
            finish()
        }
    }
}