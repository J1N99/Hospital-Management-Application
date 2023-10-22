package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAllmedicineBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.Medicine
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AllMedicineActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllmedicineBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private lateinit var medicineList: List<Medicine>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllmedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firestore().getAllMedicine { allMedicine ->
            medicineList = allMedicine
            val medicineAdapter = ArrayAdapter(
                this@AllMedicineActivity,
                android.R.layout.simple_list_item_1,
                medicineList.map { "${it.medicineName} " }


            )
            binding.medicineListView.adapter = medicineAdapter

            binding.medicineListView.setOnItemClickListener { parent, view, position, id ->
                val selectedMedicineId = medicineList[position].documentID
                val intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("medicineID", selectedMedicineId)
                startActivity(intent)
            }
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim().lowercase()

                // Only filter the list if there is non-empty search text
                if (searchText.isNotEmpty()) {
                    val filteredList = medicineList.filter { medicineList ->
                        medicineList.medicineName.lowercase().contains(searchText)
                    }

                    // Update the ListView with the filtered list
                    val medicineAdapter = ArrayAdapter(
                        this@AllMedicineActivity,
                        android.R.layout.simple_list_item_1,
                        filteredList.map { "${it.medicineName}" }
                    )
                    binding.medicineListView.adapter = medicineAdapter
                } else {
                    // If the search text is empty, show the original list
                    val medicineAdapter = ArrayAdapter(
                        this@AllMedicineActivity,
                        android.R.layout.simple_list_item_1,
                        medicineList.map { "${it.medicineName}" }
                    )
                    binding.medicineListView.adapter = medicineAdapter
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.floatingActionButton.setOnClickListener{
            val intent = Intent(this, AddMedicineActivity::class.java)
            startActivity(intent)
        }
    }
}
