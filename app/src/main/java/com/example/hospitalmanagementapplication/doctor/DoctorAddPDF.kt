package com.example.hospitalmanagementapplication

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.clerk.ClerkDashboardActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAddpdfBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.hospitalmanagementapplication.databinding.ActivityRedesignBinding
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.model.Illness
import com.example.hospitalmanagementapplication.model.PDFInfo


class DoctorAddPDF : AppCompatActivity() {
    private lateinit var binding: ActivityAddpdfBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private var hospitalID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddpdfBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        val allIllness: MutableList<Illness> = mutableListOf() // Use MutableList to allow modification

        // Initialize an empty adapter for now
        val adapter = ArrayAdapter(this, R.layout.list_private_government, listOf<String>())
        autoComplete.setAdapter(adapter)

        firestore().getAllIllness { fetchedIllnesses ->
            // Populate the allIllness list with data from Firestore
            allIllness.clear() // Clear the list to remove any existing data
            allIllness.addAll(fetchedIllnesses)

            // Create the adapter with all illnesses
            val initialAdapter = ArrayAdapter(this, R.layout.list_private_government, allIllness.map { it.illnessName })

            // Set the adapter for the AutoCompleteTextView
            autoComplete.setAdapter(initialAdapter)
        }

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on user input
                val filteredIllnesses = allIllness.filter { it.illnessName.contains(s.toString(), ignoreCase = true) }

                if (filteredIllnesses.isEmpty()) {
                    // No results found, clear the text
                    autoComplete.text = null
                }

                // Update the adapter with filtered results
                val filteredAdapter = ArrayAdapter(this@DoctorAddPDF, R.layout.list_private_government, filteredIllnesses.map { it.illnessName })
                autoComplete.setAdapter(filteredAdapter)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used in this case
            }
        })

        autoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                itemSelected = allIllness[position].documentID
            }





        binding.button.setOnClickListener {
            var illness = itemSelected.toString()
            var medicine = binding.medicineET.text.toString().trim()
            var action = binding.actionET.text.toString().trim()
            if (illness.isEmpty()||(medicine.isEmpty()&&action.isEmpty())) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {

                val pdf =
                    PDFInfo("", illness, medicine,action)
                firestore().createPDFInfo(this, pdf)


            }
        }

    }



}

