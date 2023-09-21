package com.example.hospitalmanagementapplication

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityForgetpasswordBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.CurrentDateTime
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity(){
    private lateinit var binding: ActivityForgetpasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: Loader
    private  var userID=""
    private  var forgetPasswordEmail=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetpasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        binding.button.setOnClickListener {
              val ICNumber = binding.ICEt.text.toString().trim()
              if (ICNumber.isNotEmpty()) {
                  progressDialog = Loader(this)
                  progressDialog.show()

                  firestore().getEmailFromIC(ICNumber,
                      onComplete = { user ->
                          if (user != null) {
                              userID=user.id
                              Log.d("Test",userID)

                              firestore().getEmailByUID(userID,
                                  onComplete = { email ->
                                      if (email != null) {
                                          forgetPasswordEmail=email
                                          val dataToUpdate=mapOf("resetPassword" to true,"updateby" to userID,"updatetime" to CurrentDateTime().getCurrentDateTimeFormatted())

                                          firestore().updatePosition(userID,dataToUpdate)
                                          sendEmailtoResetTempPassword(forgetPasswordEmail)
                                      } else {
                                          progressDialog.dismiss()
                                          Toast.makeText(this,"No user found with the specified UID.",Toast.LENGTH_SHORT).show()
                                      }
                                  },
                                  onError = { e ->
                                      println("Error querying Firestore: $e")
                                  }
                              )

                          } else {
                             Toast.makeText(this,"No account found with this IC",Toast.LENGTH_SHORT).show()
                          }
                      },
                      onError = { e ->
                          println("Error querying Firestore: $e")
                      }
                  )




              } else {
                  // Email field is empty, show an error to the user
                  Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
              }
              }
    }

    fun sendEmailtoResetTempPassword(forgetPasswordEmail:String){

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
    }
    fun showDialogForgetPassword() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text ="Please check the email for reset for temporary password"
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