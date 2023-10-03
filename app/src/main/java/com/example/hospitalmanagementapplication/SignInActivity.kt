package com.example.hospitalmanagementapplication

import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivitySigninBinding
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {
    //TODO add on resend verification email
    private lateinit var binding: ActivitySigninBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: Loader
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.navigationSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.navigationForgetPassword.setOnClickListener{
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                progressDialog = Loader(this)
                progressDialog.show()

                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val currentUser = firebaseAuth.currentUser
                        if (currentUser != null) {
                            if(currentUser.isEmailVerified) {

                                val userId = currentUser.uid
                                val usersRef = firestore.collection("users")


                                usersRef.document(userId).get().addOnCompleteListener { innerTask ->
                                    progressDialog.dismiss()

                                    if (innerTask.isSuccessful) {
                                        val document: DocumentSnapshot? = innerTask.result
                                        if (document != null && document.exists()) {
                                            firestore().getUserDetails(this) { user ->
                                                if (user != null) {
                                                   if(user.resetPassword)
                                                   {
                                                       resetPassword()
                                                   }
                                                    else
                                                   {
                                                       successLoginAndGotDetails()
                                                   }

                                                } else {
                                                    Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                        } else {
                                            goRegisterUserDetails()
                                        }
                                    } else {
                                        Log.e(
                                            "Firestore",
                                            "Error getting document: ${innerTask.exception?.message}"
                                        )
                                        Toast.makeText(
                                            this@SignInActivity,
                                            "Firestore Error:${innerTask.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            else{

                                progressDialog.dismiss()
                                showVerificationDialog()

                            }
                        } else {
                            progressDialog.dismiss()
                            Log.e("FirebaseAuth", "Current user is null.")
                            Toast.makeText(this@SignInActivity, "Authentication Error", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this@SignInActivity, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@SignInActivity, "Please enter all the required fields!!", Toast.LENGTH_SHORT).show()
            }
        }

    }

        private fun successLoginAndGotDetails() {
            firestore().getUserPosition(this) { position ->
                if (position != null) {
                    if(position==1)
                    {

                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                    }
                    else if(position==2)
                    {
                        val intent = Intent(this, DoctorHomeActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    // Handle the case where there was an error or the position is not available.
                }
            }



    }

    private fun goRegisterUserDetails() {
        val intent = Intent(this, userDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun resetPassword() {
        val intent = Intent(this, resetPasswordActivity::class.java)
        intent.putExtra("resetPassword", true)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        if (firebaseAuth.currentUser != null) {

            firestore().getUserPosition(this) { position ->
                if (position != null) {
                if (position == 2) {
                        val intent = Intent(this, DoctorHomeActivity::class.java)
                        startActivity(intent)
                    }
                }
                else
                {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                }
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
        val buttonResendVerificationCode=dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonResendVerificationCode.setOnClickListener {
            progressDialog = Loader(this)
            progressDialog.show()
            firebaseAuth.currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Verification email sent successfully.", Toast.LENGTH_SHORT).show()
                        FirebaseAuth.getInstance().signOut()
                        dialog.dismiss()
                        progressDialog.dismiss()
                    } else {
                        Toast.makeText(this, "Error sending verification email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        FirebaseAuth.getInstance().signOut()
                        dialog.dismiss()
                        progressDialog.dismiss()

                    }
                }
        }

        buttonDismiss.setOnClickListener {
                dialog.dismiss()
        }

        // Add a listener to handle the user's action when dismissing the dialog
        dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            // You can take further action here if needed
            Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show()
        })

        dialog.show()
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