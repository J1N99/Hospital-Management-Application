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
import com.example.hospitalmanagementapplication.databinding.ActivityTestemailBinding
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.GlobalScope
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.random.Random

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TestMailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestemailBinding
    private lateinit var progressDialog: Loader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestemailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.button.setOnClickListener {
            val forgetPasswordEmail = "ggit9000@gmail.com"
            // Generate a random 6-digit number
            val sixDigitRandomNumber = generateRandomSixDigitNumber()

            if (forgetPasswordEmail.isNotEmpty()) {
                progressDialog = Loader(this)
                progressDialog.show()

                // Send the email with the random number on a background thread
                sendEmailInBackground(forgetPasswordEmail, sixDigitRandomNumber)
            } else {
                // Email field is empty, show an error to the user
                Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDialogForgetPassword() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = "Please check the email for the reset password code"
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

    private fun sendEmailInBackground(to: String, sixDigitRandomNumber: Int) {
        // Use Kotlin Coroutine to perform this task in a background thread
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val username = "angwj99@gmail.com"
                val password = "lotoojgbuvppupgr"

                val props = Properties()
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "465"
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.ssl.enable"] = "true"


                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(username))
                message.addRecipient(Message.RecipientType.TO, InternetAddress(to))
                message.subject = "Forget password for account $to"
                message.setText("The forget password validation code is: $sixDigitRandomNumber\nPlease ignore if you didn't request this.")

                Transport.send(message)

                // Notify the UI thread that the email has been sent
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    showDialogForgetPassword()
                }

                Log.e("Success", "Email sent successfully.")
            } catch (e: Exception) {
                Log.e("Fail", "Error sending email: ${e.message}")
                Log.e("To", "Error sending email: $to")

                // Notify the UI thread of the error
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@TestMailActivity, "Error sending email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateRandomSixDigitNumber(): Int {
        val min = 100_000
        val max = 999_999
        return Random.nextInt(min, max)
    }
}

