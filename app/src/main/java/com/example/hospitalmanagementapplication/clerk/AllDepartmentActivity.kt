package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAlldepartmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.department
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AllDepartmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlldepartmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private lateinit var departmentList: List<department>
    private lateinit var filteredList: List<department>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlldepartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firestore().getAllDepartment { departmentListDetails ->
            departmentList = departmentListDetails
            filteredList=departmentList
            val departmentAdapter = ArrayAdapter(
                this@AllDepartmentActivity,
                android.R.layout.simple_list_item_1,
                departmentList.map { "${it.department} " }
            )
            binding.departmentListview.adapter = departmentAdapter

            binding.departmentListview.setOnItemClickListener { parent, view, position, id ->
                val selectedHospitalId = filteredList[position].documentID
                val intent = Intent(this, AddDepartmentActivity::class.java)
                intent.putExtra("departmentID", selectedHospitalId)
                startActivity(intent)
            }
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim().lowercase()

                // Only filter the list if there is non-empty search text
                if (searchText.isNotEmpty()) {
                     filteredList = departmentList.filter { departmentList ->
                        departmentList.department!!.lowercase().contains(searchText)
                    }

                    // Update the ListView with the filtered list
                    val departmentAdapter = ArrayAdapter(
                        this@AllDepartmentActivity,
                        android.R.layout.simple_list_item_1,
                        filteredList.map { "${it.department}" }
                    )
                    binding.departmentListview.adapter = departmentAdapter
                } else {
                    // If the search text is empty, show the original list
                    val hospitalAdapter = ArrayAdapter(
                        this@AllDepartmentActivity,
                        android.R.layout.simple_list_item_1,
                       departmentList.map { "${it.department}" }
                    )
                    binding.departmentListview.adapter = hospitalAdapter
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.floatingActionButton.setOnClickListener{
            val intent = Intent(this, AddDepartmentActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
