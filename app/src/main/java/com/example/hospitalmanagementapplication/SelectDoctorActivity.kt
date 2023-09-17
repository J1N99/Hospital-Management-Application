package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAlluserBinding
import com.example.hospitalmanagementapplication.databinding.ActivitySelectdoctorBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class SelectDoctorActivity :AppCompatActivity() {
    private lateinit var binding: ActivitySelectdoctorBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectdoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore().getAllDoctor { userList ->
            // Once you have the user data, you can populate it into the ListView
            val doctorAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                userList.map { it.firstname + " " + it.lastname })
            binding.doctorListview.adapter = doctorAdapter
        }

        binding.doctorListview.setOnItemClickListener { parent, view, position, id ->
            firestore().getAllUsers { userList ->

                val selectedDoctorId = userList[position].id
                val intent = Intent(this, BookingActivity::class.java)
                intent.putExtra("doctorID", selectedDoctorId)
                startActivity(intent)


            }


        }
    }
}