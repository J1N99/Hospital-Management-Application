package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivitySigninBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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

        binding.button.setOnClickListener {
            //TODO(Button need to click twice)
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val currentUser = firebaseAuth.currentUser

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (currentUser != null) {
                            val userId = currentUser.uid
                            val usersRef = firestore.collection("users")
                            Log.d("Firestore", "${userId}")

                            usersRef.document(userId).get().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document: DocumentSnapshot? = task.result
                                    if (document != null && document.exists()) {
                                        Log.d("Firestore", "Document exists. Going to HomeActivity.")
                                        successLoginAndGotDetails()
                                    } else {
                                        Log.d("Firestore", "Document does not exist. Going to userDetailsActivity.")
                                        goRegisterUserDetails()
                                    }
                                } else {
                                    Log.e("Firestore", "Error getting document: ${task.exception?.message}")
                                    Toast.makeText(this@SignInActivity, "Firestore Error:${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@SignInActivity, it.exception?.message, Toast.LENGTH_SHORT).show()
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
}