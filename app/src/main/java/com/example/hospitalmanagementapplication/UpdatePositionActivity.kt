package com.example.hospitalmanagementapplication

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActitvityUpdatepositionBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.CurrentDateTime
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class UpdatePositionActivity: AppCompatActivity() {

    private lateinit var binding: ActitvityUpdatepositionBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userId:String
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActitvityUpdatepositionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        bottomNavigationView = findViewById(com.example.hospitalmanagementapplication.R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(com.example.hospitalmanagementapplication.R.id.others);
        IntentManager(this, bottomNavigationView)

        val spinner: Spinner = findViewById(com.example.hospitalmanagementapplication.R.id.spinner)
        var updateID=firebaseAuth.currentUser?.uid
        userId = intent.getStringExtra("userId") ?: ""
        var documentID=""
        var dbposition=0

        firestore().getOtherUserDetails(this,userId) { user ->

            if (user != null) {
                documentID=userId
                Log.v("User Position", "${user.position}")
                dbposition=user.position
                binding.NameEt.setText(user.firstname+" "+ user.lastname)
                binding.icnumberET.setText(user.ic)
                binding.NameEt.isEnabled=false
                binding.icnumberET.isEnabled=false
                binding.NameEt.setBackgroundResource(android.R.color.darker_gray)
                binding.icnumberET.setBackgroundResource(android.R.color.darker_gray)

                // Define an array of items to display in the Spinner
                val positions = arrayOf("Please select position", "Patient","Doctor", "Clerk",)

                // Create an ArrayAdapter using the defined items and a default layout
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positions)

                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                // Set the ArrayAdapter to the Spinner
                spinner.adapter = adapter
                Log.v("Dbposition", "$dbposition")
                // Set default selection
                val defaultSelection = dbposition
                Log.v("Default Selection", "$defaultSelection")
                spinner.setSelection(defaultSelection)
            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }





        binding.button.setOnClickListener{
            val selectedPosition = spinner.selectedItem.toString()
            Log.d("Selected Position",selectedPosition)
            var position = when (selectedPosition) {
                "Please select position"->   "0"
                "Patient"->"1"
                "Doctor"->"2"
                "Clerk" -> "3"
                else -> "Unknown position"
            }
            Log.d(" Position",position)
            var positionInt=position.toInt()
            Log.d(" PositionInt","$positionInt")
            if(positionInt==0)
            {
                Toast.makeText(
                    this,
                    "Please select position",
                    Toast.LENGTH_LONG
                ).show()
            }
            else
            {

                val dataToUpdate=mapOf("position" to positionInt,"updateby" to updateID as Any,"updatetime" to CurrentDateTime().getCurrentDateTimeFormatted())

                firestore().updatePosition(documentID,dataToUpdate)
            }
        }
    }
}