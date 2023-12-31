package com.example.hospitalmanagementapplication


import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityProfileBinding
import com.example.hospitalmanagementapplication.doctor.DoctorInformationActivity
import com.example.hospitalmanagementapplication.firebase.firestore
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

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.profile);
                IntentManager(this, bottomNavigationView,position)
            }
        }
        firebaseAuth = FirebaseAuth.getInstance()

        var year = 0;
        var month = 0;
        var day = 0;
        var documentID = ""

        firestore().getUserDetails(this) { user ->
            if (user != null) {
                documentID = user.id
                binding.firstNameEt.setText(user.firstname)
                binding.firstNameEt.isEnabled = false


                binding.lastNameET.setText(user.lastname)
                binding.lastNameET.isEnabled = false


                val dob = user.dob.split("-")

                day = dob[0].toInt()
                month = dob[1].toInt()
                year = dob[2].toInt()






                binding.ageET.setText(user.dob)
                binding.ageET.isEnabled = false



                binding.icNumberET.setText(user.ic)
                binding.icNumberET.isEnabled = false


                binding.radioMale.isEnabled = false
                binding.radioFemale.isEnabled = false
                if (user.gender) binding.radioMale.isChecked =
                    true else binding.radioFemale.isChecked = true

                if(user.position==2)
                {
                    binding.editDoctorInfo.visibility=View.VISIBLE
                }
            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }
        binding.editDoctorInfo.setOnClickListener{
            val intent = Intent(this, DoctorInformationActivity::class.java)
            startActivity(intent)

        }
        binding.navigationResetPassword.setOnClickListener{
            val intent = Intent(this, resetPasswordActivity::class.java)
            startActivity(intent)

        }
        binding.button.setOnClickListener {
            if (binding.button.text == "Edit") {
                binding.firstNameEt.isEnabled = true


                binding.lastNameET.isEnabled = true




                binding.ageET.setOnClickListener {

                    var ic = binding.icNumberET.text.toString()
                    var year = 0;
                    var month = 0;
                    var day = 0;
                    if (ic.length >= 6) {

                        var icyear = ic.substring(0, 2).toInt()
                        year = if (icyear in 51..99) {
                            1900 + icyear

                        } else {
                            2000 + icyear
                        }
                        month = ic.substring(2, 4).toInt() - 1
                        day = ic.substring(4, 6).toInt()

                    } else {
                        year = 1999
                        month = 10
                        day = 25
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

                binding.ageET.isEnabled = true


                binding.icNumberET.isEnabled = true


                binding.radioMale.isEnabled = true
                binding.radioFemale.isEnabled = true

                binding.button.text = "Save Edit"
            } else if (binding.button.text == "Save Edit") {

                val lastname = binding.lastNameET.text.toString()
                val firstname = binding.firstNameEt.text.toString()
                val DOB = binding.ageET.text.toString()
                val ic = binding.icNumberET.text.toString()
                var year = 0
                var month = 0
                var day = 0
                var validic = true

                if (lastname.isEmpty() && firstname.isEmpty() && ic.isEmpty() && DOB.isEmpty()) {
                    Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
                } else if (ic.length != 12) {
                    Toast.makeText(this, "Please enter the correct IC Number", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    year = ic.substring(0, 2).toInt()
                    month = ic.substring(2, 4).toInt() - 1
                    day = ic.substring(4, 6).toInt()

                    if (month < 0 || month > 11 || day < 1 || day > 31) {
                        Toast.makeText(this, "Please enter a valid IC Number", Toast.LENGTH_SHORT)
                            .show()
                        validic = false
                    } else if (DOB.isNotEmpty()) {
                        val parts = DOB.split("-")

                        if (year != parts[2].takeLast(2)
                                .toInt() || month != parts[1].toInt() - 1 || day != parts[0].toInt()
                        ) {
                            Toast.makeText(
                                this,
                                "Please enter the correct date of birth matching the IC",
                                Toast.LENGTH_SHORT
                            ).show()
                            validic = false
                        }
                    }
                }

                if (validic) {
                    val currentUser = firebaseAuth.currentUser
                    val userId = currentUser?.uid
                    Log.d("userId", "$userId")
                    if (userId != null) {

                        val lastname = binding.lastNameET.text.toString()
                        val firstname = binding.firstNameEt.text.toString()
                        val dob = binding.ageET.text.toString()
                        val gender = binding.radioMale.isChecked
                        val icNumber = binding.icNumberET.text.toString()

                        val dataToUpdate = mapOf(
                            "lastname" to lastname,
                            "firstname" to firstname,
                            "dob" to dob,
                            "gender" to gender,
                            "ic" to icNumber
                        )

                        firestore().updateDocument("users", documentID, dataToUpdate)
                        showDialogSuccessUpdate()

                    }


                }
            }


        }
    }
    private fun showDialogSuccessUpdate() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text ="The information have updated!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail=dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility= View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Add a listener to handle the user's action when dismissing the dialog
        dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            // You can take further action here if needed
            Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show()
        })

        dialog.show()
    }


}
