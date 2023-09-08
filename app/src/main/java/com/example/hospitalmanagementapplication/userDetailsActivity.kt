package com.example.hospitalmanagementapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityUserdetailsBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class userDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserdetailsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserdetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("Inside", "Hello world")
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance() // Initialize Firebase database

        binding.button.setOnClickListener {
            saveUserDetails()
        }

        binding.ageET.setOnClickListener {
            // Get the current date
            val year = 1999
            val month = 10
            val day =25

            // Create a date picker dialog
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    // Handle the selected date and set it in ageET
                    val formattedDate = "$selectedDayOfMonth-${selectedMonth + 1}-$selectedYear"
                    binding.ageET.setText(formattedDate)
                },
                year,
                month,
                day
            )

            // Show the date picker dialog when ageET is clicked
            datePickerDialog.show()
        }
    }

    private fun saveUserDetails() {

        val currentUser = firebaseAuth.currentUser
        val userId = currentUser?.uid
        Log.d("userId","$userId")
        if (userId != null) {
            val lastname = binding.lastNameET.text.toString()
            val firstname = binding.firstNameEt.text.toString()
            val dob = binding.ageET.text.toString()
            val gender = binding.radioMale.isChecked
            val icNumber=binding.icNumberET.text.toString()
            val position=1


            val user = User(userId, lastname, firstname, gender,dob,icNumber,position)
            firestore().registerUserDetails(this,user)

        }
    }
}
