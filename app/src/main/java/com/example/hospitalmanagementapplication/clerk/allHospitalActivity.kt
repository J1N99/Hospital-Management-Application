package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAllhospitalBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.model.User
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class allHospitalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllhospitalBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private lateinit var hospitalList: List<Hospital>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllhospitalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firestore().getAllHospital { allHospitalList ->
            hospitalList = allHospitalList
            val hospitalAdapter = ArrayAdapter(
                this@allHospitalActivity,
                android.R.layout.simple_list_item_1,
                hospitalList.map { "${it.hospital} " }
            )
            binding.hospitalListview.adapter = hospitalAdapter

            binding.hospitalListview.setOnItemClickListener { parent, view, position, id ->
                val selectedHospitalId = hospitalList[position].documentId
                val intent = Intent(this, addHospitalActivity::class.java)
                intent.putExtra("hospitalId", selectedHospitalId)
                startActivity(intent)
            }
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim().lowercase()

                // Only filter the list if there is non-empty search text
                if (searchText.isNotEmpty()) {
                    val filteredList = hospitalList.filter { hospitalList ->
                        hospitalList.hospital.lowercase().contains(searchText)
                    }

                    // Update the ListView with the filtered list
                    val hospitalAdapter = ArrayAdapter(
                        this@allHospitalActivity,
                        android.R.layout.simple_list_item_1,
                        filteredList.map { "${it.hospital}" }
                    )
                    binding.hospitalListview.adapter = hospitalAdapter
                } else {
                    // If the search text is empty, show the original list
                    val hospitalAdapter = ArrayAdapter(
                        this@allHospitalActivity,
                        android.R.layout.simple_list_item_1,
                        hospitalList.map { "${it.hospital}" }
                    )
                    binding.hospitalListview.adapter = hospitalAdapter
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.floatingActionButton.setOnClickListener{
            val intent = Intent(this, addHospitalActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
