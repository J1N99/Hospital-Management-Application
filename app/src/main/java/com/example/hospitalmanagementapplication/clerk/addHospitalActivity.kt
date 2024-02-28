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
import com.example.hospitalmanagementapplication.databinding.ActivityAddhospitalBinding
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.utils.IntentManager


class addHospitalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddhospitalBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private var itemSelected: Any=""
    private var hospitalID = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddhospitalBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        hospitalID = intent.getStringExtra("hospitalId") ?: ""
        Log.e(hospitalID, hospitalID)


        val items = listOf("Private", "Government")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        val adapter = ArrayAdapter(this, R.layout.list_private_government, items)
        autoComplete.setAdapter(adapter)
        autoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                itemSelected = parent.getItemAtPosition(position)
            }

        if (hospitalID != "") {
            binding.button.text = "Save Edit"
            binding.buttonDeleteHospital.visibility = View.VISIBLE
            firestore().getHospitalDetails(this, hospitalID) { hospital ->
                if (hospital != null) {
                    autoComplete.setText(hospital.privateGovernment, false)
                    itemSelected = hospital.privateGovernment
                    binding.hospitalET.setText(hospital.hospital)
                    binding.addressET.setText(hospital.address)
                } else {
                    Toast.makeText(this, "Hospital is null", Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.button.setOnClickListener {

            var hospital = binding.hospitalET.text.toString().trim()
            var address = binding.addressET.text.toString().trim()
            if (itemSelected=="" || hospital.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {
                var privateGovernment = itemSelected.toString()
                if (binding.button.text == "Save Edit") {


                    val dataToUpdate = mapOf(
                        "privateGovernment" to privateGovernment,
                        "hospital" to hospital,
                        "address" to address
                    )

                    firestore().updateDocument("hospital", hospitalID, dataToUpdate)
                    showDialogSuccessUpdate("The information have updated!")
                } else {
                    val hospitalInfo =
                        Hospital(privateGovernment, hospital, address)
                    firestore().createHospital(this, hospitalInfo)
                    showDialogSuccessUpdate("The information have created!")
                }
            }
        }

        binding.buttonDeleteHospital.setOnClickListener {

            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete this hospital?")
                .setPositiveButton("Yes") { _, _ ->

                    // Call the function to check if an item with the respective foreign key exists
                    firestore().getForeignKeyItem(hospitalID, "hospital", "doctorInformation") { success ->
                        if (success) {
                            Log.e("Foreign key exisit","OK")
                            Toast.makeText(this, "Cannot delete because the hospital is referenced.", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("Foreign key not exisit","OK")
                            firestore().deleteDocument(hospitalID, "hospital",
                                onSuccess = {
                                    val intent = Intent(this, allHospitalActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                onFailure = { e ->
                                    Log.w("ERROR", "Error deleting document", e)
                                })
                        }
                    }
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
            val intent = Intent(this, allHospitalActivity::class.java)
            startActivity(intent)
            finish()
        }
        dialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, allHospitalActivity::class.java)
        startActivity(intent)
        finish()
    }
}

