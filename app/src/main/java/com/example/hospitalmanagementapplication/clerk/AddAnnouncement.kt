package com.example.hospitalmanagementapplication

import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.clerk.ClerkDashboardActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAddannoucementBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.hospitalmanagementapplication.databinding.ActivityAddhospitalBinding
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File


class AddAnnouncement : AppCompatActivity() {
    private lateinit var binding: ActivityAddannoucementBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private var hospitalID = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddannoucementBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firestore().getAnnouncement { announcementData ->
            if (announcementData != null) {
                // Handle the announcement data here
                binding.announcementTitleET.setText( announcementData["announcementTitle"].toString())
                binding.announcementET.setText( announcementData["announcement"].toString())
            }
        }


        binding.button.setOnClickListener {

            var announcementTitle = binding.announcementTitleET.text.toString().trim()
            var announcement = binding.announcementET.text.toString().trim()
            if (announcementTitle.isEmpty() || announcement.isEmpty()) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {


                firestore().createOrUpdateAnnouncement(announcementTitle, announcement)
                    .addOnSuccessListener {
                        showDialogSuccessUpdate("The information have updated!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GG",e.toString())
                        Toast.makeText(
                            this,
                            "Error on create or update announcement",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            }
        }


    }


    private fun showDialogSuccessUpdate(textShow: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = textShow
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail = dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility = View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ClerkDashboardActivity::class.java)
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

