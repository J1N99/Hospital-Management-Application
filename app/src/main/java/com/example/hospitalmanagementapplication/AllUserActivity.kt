package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAlluserBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AllUserActivity: AppCompatActivity() {
    private lateinit var binding: ActivityAlluserBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlluserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore().getAllUsers { userList ->
            // Once you have the user data, you can populate it into the ListView
            val userAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, userList.map { it.firstname +" " +it.lastname+" - "+it.ic})
            binding.userListView.adapter = userAdapter
        }

        binding.userListView.setOnItemClickListener { parent, view, position, id ->
            firestore().getAllUsers { userList ->

                val selectedUserId = userList[position].id
                val intent = Intent(this, UpdatePositionActivity::class.java)
                intent.putExtra("userId", selectedUserId)
                startActivity(intent)


            }


        }
    }




}

