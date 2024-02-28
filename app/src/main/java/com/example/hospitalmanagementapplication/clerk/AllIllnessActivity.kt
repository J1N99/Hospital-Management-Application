package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAllillnessBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.Illness
import com.example.hospitalmanagementapplication.model.User
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AllIllnessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllillnessBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private lateinit var illnessList: List<Illness>
    private lateinit var filteredList: List<Illness>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllillnessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        firestore().getAllIllness { allIllness ->
            illnessList = allIllness
            filteredList=illnessList
            val illnessAdapter = ArrayAdapter(
                this@AllIllnessActivity,
                android.R.layout.simple_list_item_1,
                illnessList.map { "${it.illnessName} " }


            )
            binding.illnessListView.adapter = illnessAdapter

            binding.illnessListView.setOnItemClickListener { parent, view, position, id ->
                val selectedIllnessId = filteredList[position].documentID
                val intent = Intent(this, AddIllnessActivity::class.java)
                intent.putExtra("illnessID", selectedIllnessId)
                startActivity(intent)
            }
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim().lowercase()

                // Only filter the list if there is non-empty search text
                if (searchText.isNotEmpty()) {
                     filteredList = illnessList.filter { illnessList ->
                        illnessList.illnessName.lowercase().contains(searchText)
                    }

                    // Update the ListView with the filtered list
                    val illnessAdapter = ArrayAdapter(
                        this@AllIllnessActivity,
                        android.R.layout.simple_list_item_1,
                        filteredList.map { "${it.illnessName}" }
                    )
                    binding.illnessListView.adapter = illnessAdapter
                } else {
                    // If the search text is empty, show the original list
                    val illnessAdapter = ArrayAdapter(
                        this@AllIllnessActivity,
                        android.R.layout.simple_list_item_1,
                        illnessList.map { "${it.illnessName}" }
                    )
                    binding.illnessListView.adapter = illnessAdapter
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.floatingActionButton.setOnClickListener{
            val intent = Intent(this, AddIllnessActivity::class.java)
            startActivity(intent)
        }
    }
}
