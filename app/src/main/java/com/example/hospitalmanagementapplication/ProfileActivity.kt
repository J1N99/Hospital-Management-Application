package com.example.hospitalmanagementapplication


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityProfileBinding
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth


class ProfileActivity:AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.profile);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()


    }
    override fun onBackPressed() {
        // Create an Intent to navigate to TargetActivity
        val intent = Intent(this, ForgetPasswordActivity::class.java)
        startActivity(intent)

        // Optionally, finish the current activity to remove it from the back stack
        finish()
    }
}
