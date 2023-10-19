package com.example.hospitalmanagementapplication

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActitvityUpdatepositionBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.CurrentDateTime
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class UpdatePositionActivity : AppCompatActivity() {

    private lateinit var binding: ActitvityUpdatepositionBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userId: String
    private lateinit var itemSelected: Any
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActitvityUpdatepositionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
            }
        }


        var updateID = firebaseAuth.currentUser?.uid
        userId = intent.getStringExtra("userId") ?: ""
        var documentID = ""
        var dbposition = 0

        val items = listOf("Patient", "Doctor", "Clerk")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        val adapter = ArrayAdapter(this, R.layout.list_private_government, items)
        autoComplete.setAdapter(adapter)
        autoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->

                val position= parent.getItemAtPosition(position)
                when (position) {
                    "Patient" -> {
                     itemSelected=1
                    }
                    "Doctor" -> {
                        itemSelected=2
                    }
                    "Clerk" -> {
                        itemSelected=3
                    }

                }
            }

        firestore().getOtherUserDetails(this, userId) { user ->

            if (user != null) {
                documentID = userId
                Log.v("User Position", "${user.position}")
                dbposition = user.position
                binding.NameEt.setText(user.firstname + " " + user.lastname)
                binding.icnumberET.setText(user.ic)
                binding.NameEt.isEnabled = false
                binding.icnumberET.isEnabled = false
                binding.NameEt.setBackgroundResource(android.R.color.darker_gray)
                binding.icnumberET.setBackgroundResource(android.R.color.darker_gray)

                when (user.position) {
                    1 -> {
                        binding.autoCompleteTextView.setText("Patient",false)

                    }
                    2 -> {
                        binding.autoCompleteTextView.setText("Doctor",false)

                    }
                    3 -> {
                        binding.autoCompleteTextView.setText("Clerk",false)

                    }
                    4->{
                        binding.autoCompleteTextView.setText("Super Account",false)
                        binding.autoCompleteTextView.isEnabled=false
                        binding.autoCompleteTextView.setBackgroundResource(android.R.color.darker_gray)
                    }
                }

            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }

        binding.button.setOnClickListener {
            if (::itemSelected.isInitialized) {
                val positionInt = itemSelected

                val dataToUpdate = mapOf(
                    "position" to positionInt,
                    "updateby" to updateID as Any,
                    "updatetime" to CurrentDateTime().getCurrentDateTimeFormatted()
                )

                firestore().updatePosition(documentID, dataToUpdate)
            } else {

                Toast.makeText(
                    this,
                    "Please select position",
                    Toast.LENGTH_LONG
                ).show()


            }
        }
    }
}