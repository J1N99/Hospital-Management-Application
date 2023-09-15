package com.example.hospitalmanagementapplication

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityMainBinding
import com.example.hospitalmanagementapplication.databinding.ActivitySigninBinding
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {

        //TODO Fix all the warning
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.home);
        IntentManager(this, bottomNavigationView)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser

        if (currentUser != null) {
            binding.userEmail.text = currentUser.email
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
            val intent = Intent(this, BookingActivity::class.java)
            startActivity(intent)
            finish()
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

