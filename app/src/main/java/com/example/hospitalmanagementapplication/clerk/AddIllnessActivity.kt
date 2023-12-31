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
import com.example.hospitalmanagementapplication.firebase.firestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.hospitalmanagementapplication.databinding.ActivityAddillnessBinding
import com.example.hospitalmanagementapplication.model.Illness
import com.example.hospitalmanagementapplication.utils.IntentManager


class AddIllnessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddillnessBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private var illnessID = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddillnessBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        illnessID = intent.getStringExtra("illnessID") ?: ""



        if (illnessID != "") {
            binding.button.text = "Save Edit"
            binding.buttonDeleteHospital.visibility = View.VISIBLE
            firestore().getIllnessActivity(this, illnessID) { illness ->
                if (illness != null) {
                    binding.nameOfIllnessET.setText(illness.illnessName)
                    binding.descriptionET.setText(illness.description)
                    binding.actionTakenET.setText(illness.actionTaken)
                } else {
                    Toast.makeText(this, "illness is null", Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.button.setOnClickListener {
            var nameOfIllness = binding.nameOfIllnessET.text.toString().trim()
            var description = binding.descriptionET.text.toString().trim()
            var actionTaken = binding.actionTakenET.text.toString().trim()
            if (nameOfIllness.isEmpty() || description.isEmpty() || actionTaken.isEmpty()) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {

                if (binding.button.text == "Save Edit") {

                    Log.e("test",nameOfIllness)
                    val dataToUpdate = mapOf(
                        "illnessName" to nameOfIllness,
                        "description" to description,
                        "actionTaken" to actionTaken
                    )
                    Log.e("test",dataToUpdate.toString())
                    firestore().updateDocument("illness", illnessID, dataToUpdate)
                    showDialogSuccessUpdate("The information have updated!")
                } else {
                    val illnessInfo =
                        Illness("",nameOfIllness, description, actionTaken)
                    firestore().createIllness(this, illnessInfo)
                    showDialogSuccessUpdate("The information have created!")
                }
            }
        }

        binding.buttonDeleteHospital.setOnClickListener {

            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete this illness?")
                .setPositiveButton("Yes") { _, _ ->
                    firestore().deleteDocument(illnessID, "illness",
                        onSuccess = {
                            val intent = Intent(this, AllIllnessActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onFailure = { e ->
                            Log.w("ERROR", "Error deleting document", e)
                        })
                }
                .setNegativeButton("No", null)
                .show()
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
            val intent = Intent(this, AllIllnessActivity::class.java)
            startActivity(intent)
            finish()
        }


        dialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, AllIllnessActivity::class.java)
        startActivity(intent)
        finish()
    }
}

