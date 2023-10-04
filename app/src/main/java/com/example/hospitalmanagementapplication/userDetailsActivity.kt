package com.example.hospitalmanagementapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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

            var ic=binding.icNumberET.text.toString()
            var year=0;
            var month=0;
            var day=0;
            if (ic.length>=6) {

                    var icyear = ic.substring(0, 2).toInt()
                    year = if (icyear in 51..99) {
                        1900 + icyear

                    } else {
                        2000 + icyear
                    }
                    month = ic.substring(2, 4).toInt() - 1
                    day = ic.substring(4, 6).toInt()


            }
            else{
                year=1999
                month=10
                day=25
            }


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

        var lastname=binding.lastNameET.text.toString().trim()
        var firstname=binding.firstNameEt.text.toString().trim()
        var ic=binding.icNumberET.text.toString().trim()
        var DOB=binding.ageET.text.toString().trim()



        var year = 0
        var month = 0
        var day = 0
        var validic = true

        if (lastname.isEmpty() && firstname.isEmpty() && ic.isEmpty() && DOB.isEmpty()) {
            Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
        } else if (ic.length != 12) {
            Toast.makeText(this, "Please enter the correct IC Number", Toast.LENGTH_SHORT).show()
        } else {
            year = ic.substring(0, 2).toInt()
            month = ic.substring(2, 4).toInt() - 1
            day = ic.substring(4, 6).toInt()

            if (month < 0 || month > 11 || day < 1 || day > 31) {
                Toast.makeText(this, "Please enter a valid IC Number", Toast.LENGTH_SHORT).show()
                validic = false
            } else if (DOB.isNotEmpty()) {
                val parts = DOB.split("-")

                if (year != parts[2].takeLast(2).toInt() || month != parts[1].toInt() - 1 || day != parts[0].toInt()) {
                    Toast.makeText(this, "Please enter the correct date of birth matching the IC", Toast.LENGTH_SHORT).show()
                    validic = false
                }
            }
        }

        if (validic) {
                val currentUser = firebaseAuth.currentUser
                val userId = currentUser?.uid
                Log.d("userId", "$userId")
                if (userId != null) {

                    val email = currentUser?.email ?: ""
                    val lastname = binding.lastNameET.text.toString()
                    val firstname = binding.firstNameEt.text.toString()
                    val dob = binding.ageET.text.toString()
                    val gender = binding.radioMale.isChecked
                    val icNumber = binding.icNumberET.text.toString()
                    val position = 1


                    val user =
                        User(userId, email, lastname, firstname, gender, dob, icNumber, position)
                    firestore().registerUserDetails(this, user)

                }


            }
        }
    }

