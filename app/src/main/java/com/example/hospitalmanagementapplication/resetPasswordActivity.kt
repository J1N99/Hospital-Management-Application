package com.example.hospitalmanagementapplication

import android.app.ActivityManager
import android.content.Context
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
import com.example.hospitalmanagementapplication.databinding.ActivityResetpasswordBinding
import com.example.hospitalmanagementapplication.databinding.ActivitySigninBinding
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.CurrentDateTime
import com.example.hospitalmanagementapplication.utils.Loader
import com.example.hospitalmanagementapplication.utils.PasswordValidation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class resetPasswordActivity:AppCompatActivity() {


        private lateinit var binding: ActivityResetpasswordBinding
        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var firestore: FirebaseFirestore

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityResetpasswordBinding.inflate(layoutInflater)
            setContentView(binding.root)

            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            val currentUser=firebaseAuth.currentUser
            val userID=currentUser?.uid?:""

            binding.button.setOnClickListener {
                val originalPassword = binding.OriginalPassET.text.toString()
                val password = binding.passET.text.toString()
                val confirmPassword = binding.confirmPassEt.text.toString()
                if (originalPassword.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {

                    if (password == (confirmPassword)) {
                        if (!PasswordValidation.isPasswordValid(password)) {
                            Toast.makeText(
                                this,
                                "The password length should One upper and lower cast ,digit and special Char".toString(),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            firestore().changePasswordWithReauthentication(this,originalPassword, password, OnCompleteListener { task ->
                                if (task.isSuccessful) {

                                    val dataToUpdate=mapOf("resetPassword" to false,"updateby" to userID,"updatetime" to CurrentDateTime().getCurrentDateTimeFormatted())

                                    firestore().updatePosition(userID,dataToUpdate)
                                    showSuccessChangePassword()
                                } else {
                                 Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Password and confirm password is not same",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill in all the required field", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

    fun showSuccessChangePassword()   {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text ="The password have been change. Please login again!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        val buttonEmail=dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility=View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, SignInActivity::class.java)
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

