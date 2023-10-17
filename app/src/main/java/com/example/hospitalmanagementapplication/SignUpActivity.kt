package com.example.hospitalmanagementapplication

import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.hospitalmanagementapplication.databinding.ActivitySignupBinding
import com.example.hospitalmanagementapplication.utils.Loader
import com.example.hospitalmanagementapplication.utils.PasswordValidation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: Loader

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        binding.navigationSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.signUp.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passET.text.toString()
            val confirmPassword = binding.confirmPassEt.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                progressDialog = Loader(this)
                progressDialog.show()
                if (password == confirmPassword) {
                    if (!PasswordValidation.isPasswordValid(password)) {
                        progressDialog.dismiss()
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "The password length should include one upper and lower case letter, digit, and special character.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = firebaseAuth.currentUser
                                    user?.sendEmailVerification()
                                        ?.addOnCompleteListener { verificationTask ->
                                            progressDialog.dismiss() // Dismiss the progressDialog here
                                            if (verificationTask.isSuccessful) {
                                                showVerificationDialog()
                                            } else {
                                                val errorMessage =
                                                    verificationTask.exception?.message
                                                if (errorMessage != null) {
                                                    Toast.makeText(
                                                        this,
                                                        "Failed to send verification email: $errorMessage",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                } else {
                                    progressDialog.dismiss() // Dismiss the progressDialog here
                                    Snackbar.make(
                                        findViewById(android.R.id.content),
                                        task.exception.toString(),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Password and confirm password do not match",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Please fill in all the required fields", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    fun showVerificationDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        buttonDismiss.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        dialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
