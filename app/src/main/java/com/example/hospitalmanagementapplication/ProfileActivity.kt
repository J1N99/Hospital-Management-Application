package com.example.hospitalmanagementapplication


import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityProfileBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.auth.User


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

        var year=0;
        var month=0;
        var day=0;
        var documentID=""

        firestore().getUserDetails(this) { user ->
            if (user != null) {
                documentID=user.id
                binding.firstNameEt.setText(user.firstname)
                binding.firstNameEt.isEnabled=false
                binding.firstNameEt.setBackgroundResource(android.R.color.darker_gray)

                binding.lastNameET.setText(user.lastname)
                binding.lastNameET.isEnabled=false
                binding.lastNameET.setBackgroundResource(android.R.color.darker_gray)

                val dob=user.dob.split("-")

                day=dob[0].toInt()
                month=dob[1].toInt()
                year=dob[2].toInt()






                binding.ageET.setText(user.dob)
                binding.ageET.isEnabled=false
                binding.ageET.setBackgroundResource(android.R.color.darker_gray)


                binding.icNumberET.setText(user.ic)
                binding.icNumberET.isEnabled=false
                binding.icNumberET.setBackgroundResource(android.R.color.darker_gray)

                binding.radioMale.isEnabled=false
                binding.radioFemale.isEnabled=false
                if (user.gender) binding.radioMale.isChecked=true else binding.radioFemale.isChecked=true


            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }

        binding.button.setOnClickListener{
           if(binding.button.text=="Edit") {
                binding.firstNameEt.isEnabled = true
                binding.firstNameEt.setBackgroundResource(android.R.color.transparent)

                binding.lastNameET.isEnabled=true
                binding.lastNameET.setBackgroundResource(android.R.color.transparent)



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
                           val formattedDate =
                               "$selectedDayOfMonth-${selectedMonth + 1}-$selectedYear"
                           binding.ageET.setText(formattedDate)
                       },
                       year,
                       month,
                       day
                   )

                   // Show the date picker dialog when ageET is clicked
                   datePickerDialog.show()
               }

                binding.ageET.isEnabled=true
                binding.ageET.setBackgroundResource(android.R.color.transparent)

                binding.icNumberET.isEnabled=true
                binding.icNumberET.setBackgroundResource(android.R.color.transparent)

                binding.radioMale.isEnabled=true
                binding.radioFemale.isEnabled=true

               binding.button.text="Save Edit"
            }
            else if(binding.button.text=="Save Edit")
            {
                val lastname = binding.lastNameET.text.toString()
                val firstname = binding.firstNameEt.text.toString()
                val dob = binding.ageET.text.toString()
                val gender = binding.radioMale.isChecked
                val icNumber=binding.icNumberET.text.toString()

                val dataToUpdate=mapOf("lastname" to lastname,"firstname" to firstname,"dob" to dob, "gender" to gender, "ic" to icNumber)

                firestore().updateDocument("users",documentID,dataToUpdate)
            }
        }


    }
    override fun onBackPressed() {
        // Create an Intent to navigate to TargetActivity
        val intent = Intent(this, ForgetPasswordActivity::class.java)
        startActivity(intent)

        // Optionally, finish the current activity to remove it from the back stack
        finish()
    }
}
