package com.example.hospitalmanagementapplication

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivitySigninBinding
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

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
                                Log.d("Firestore", "${userId}")

                                usersRef.document(userId).get().addOnCompleteListener { innerTask ->
                                    progressDialog.dismiss()

                                    if (innerTask.isSuccessful) {
                                        val document: DocumentSnapshot? = innerTask.result
                                        if (document != null && document.exists()) {
                                            Log.d(
                                                "Firestore",
                                                "Document exists. Going to HomeActivity."
                                            )
                                            successLoginAndGotDetails()
                                        } else {
                                            Log.d(
                                                "Firestore",
                                                "Document does not exist. Going to userDetailsActivity."
                                            )
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
                                FirebaseAuth.getInstance().signOut()
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
        Log.v("Test", "test")
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun goRegisterUserDetails() {
        Log.v("No", "No")
        val intent = Intent(this, userDetailsActivity::class.java)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
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
                dialog.dismiss()
        }

        // Add a listener to handle the user's action when dismissing the dialog
        dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            // You can take further action here if needed
            Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show()
        })

        dialog.show()
    }
}