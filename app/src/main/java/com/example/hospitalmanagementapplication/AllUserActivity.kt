package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAlluserBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.User
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AllUserActivity: AppCompatActivity() {
    private lateinit var binding: ActivityAlluserBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userList: List<User>
    private lateinit var progressDialog: Loader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlluserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }
        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getAllUsers { users ->
            progressDialog = Loader(this)
            progressDialog.show()
            userList = users
            val userAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, users.map { "${it.firstname} ${it.lastname} - ${it.ic}" })
            binding.userListView.adapter = userAdapter
            progressDialog.dismiss()
        }

        binding.userListView.setOnItemClickListener { parent, view, position, id ->
            val selectedUserId = userList[position].id
            val intent = Intent(this, UpdatePositionActivity::class.java)
            intent.putExtra("userId", selectedUserId)
            startActivity(intent)
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the user list based on the search input
                val searchText = s.toString().lowercase()
                val filteredList = userList.filter { user ->
                    user.firstname.lowercase().contains(searchText) ||
                            user.lastname.lowercase().contains(searchText) ||
                            user.ic.lowercase().contains(searchText)
                }

                // Update the ListView with the filtered list
                val userAdapter = ArrayAdapter(this@AllUserActivity, android.R.layout.simple_list_item_1, filteredList.map { "${it.firstname} ${it.lastname} - ${it.ic}" })
                binding.userListView.adapter = userAdapter
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }
}
