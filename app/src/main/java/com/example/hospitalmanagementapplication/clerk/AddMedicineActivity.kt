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
import com.example.hospitalmanagementapplication.databinding.ActivityAddmedicineBinding
import com.example.hospitalmanagementapplication.model.Medicine
import com.example.hospitalmanagementapplication.utils.IntentManager


class AddMedicineActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddmedicineBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private var medicineID = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddmedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        val items = listOf("One Day 3 Time", "One Day Two Time","One day one Time")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        val adapter = ArrayAdapter(this, R.layout.list_private_government, items)
        autoComplete.setAdapter(adapter)
        autoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                itemSelected = parent.getItemAtPosition(position)
                Log.e("Check",itemSelected.toString())
            }


        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        medicineID = intent.getStringExtra("medicineID") ?: ""



        if (medicineID != "") {
            binding.button.text = "Save Edit"
            binding.buttonDeleteHospital.visibility = View.VISIBLE
            firestore().getMedicineActivity(this, medicineID) { medicine ->
                if (medicine != null) {
                    autoComplete.setText(medicine.medicationTime, false)
                    itemSelected = medicine.medicationTime
                    binding.nameOfMedicineET.setText(medicine.medicineName)
                    binding.descriptionET.setText(medicine.description)

                } else {
                    Toast.makeText(this, "Medicine is null", Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.button.setOnClickListener {
            var nameOfMedicine = binding.nameOfMedicineET.text.toString().trim()
            var description = binding.descriptionET.text.toString().trim()
            var medicineTakenTime = itemSelected.toString()
            if (nameOfMedicine.isEmpty()  || description.isEmpty() || medicineTakenTime.isEmpty()) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {

                if (binding.button.text == "Save Edit") {


                    val dataToUpdate = mapOf(
                        "nameOfMedicine" to nameOfMedicine,
                        "description" to description,
                        "medicineTaken" to medicineTakenTime
                    )

                    firestore().updateDocument("medicine", medicineID, dataToUpdate)
                    showDialogSuccessUpdate("The information have updated!")
                } else {
                    val medicineInfo =
                        Medicine("",nameOfMedicine, description, medicineTakenTime)
                    firestore().createMedicine(this, medicineInfo)
                    showDialogSuccessUpdate("The information have created!")
                }
            }
        }

        binding.buttonDeleteHospital.setOnClickListener {

            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete this medecine?")
                .setPositiveButton("Yes") { _, _ ->
                    firestore().deleteDocument(medicineID, "medicine",
                        onSuccess = {
                            val intent = Intent(this, AllMedicineActivity::class.java)
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
            val intent = Intent(this, AllMedicineActivity::class.java)
            startActivity(intent)
            finish()
        }


        dialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, AllMedicineActivity::class.java)
        startActivity(intent)
        finish()
    }
}

