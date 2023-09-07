package com.example.hospitalmanagementapplication

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityForgetpasswordBinding
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity(){
    private lateinit var binding: ActivityForgetpasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: Loader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetpasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        binding.button.setOnClickListener {
              val forgetPasswordEmail = binding.emailEt.text.toString().trim()
            //TODO advance forget password
              if (forgetPasswordEmail.isNotEmpty()) {
                  progressDialog = Loader(this)
                  progressDialog.show()

                  firebaseAuth.sendPasswordResetEmail(forgetPasswordEmail)
                      .addOnCompleteListener { task: Task<Void> ->
                          if (task.isSuccessful) {
                              progressDialog.dismiss()
                              showDialogForgetPassword()
                          } else {
                              // Password reset email failed to send
                              // You can show an error message to the user
                              progressDialog.dismiss()
                              Toast.makeText(this, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                          }
                      }
              } else {
                  // Email field is empty, show an error to the user
                  Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
              }
              }
    }
    fun showDialogForgetPassword() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text ="Please check the email for reset password"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
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