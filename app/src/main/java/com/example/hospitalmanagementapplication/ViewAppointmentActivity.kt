package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAlluserBinding
import com.example.hospitalmanagementapplication.databinding.ActivityViewappointmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ViewAppointmentActivity: AppCompatActivity() {
    private lateinit var binding: ActivityViewappointmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewappointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setSelectedItemId(R.id.others);
        IntentManager(this, bottomNavigationView)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore().getAndDisplayAppointments { appointments ->
            // Here, you have access to the list of appointments retrieved from Firestore
            // You can use this list to create card views or update your UI
            for (appointment in appointments) {
                // Create card views or update UI elements with appointment data
                val documentID=appointment.documentID
                val dateAppointment = appointment.dateAppointment
                val doctorId = appointment.doctorId
                val time = appointment.time

                // Create card view or update UI here
                createCardView(documentID,dateAppointment, doctorId, time)
            }
        }


    }

    fun createCardView(documentID:String?,date: String?, doctorId: String?, time: String?) {
        // Find the LinearLayout within the ConstraintLayout
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        // Inflate your card view layout here (e.g., from XML)
        val cardView = LayoutInflater.from(this).inflate(R.layout.appointment_card_view, null)

        // Bind data to card view elements (TextViews, etc.)
        val dateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val doctorTextView = cardView.findViewById<TextView>(R.id.doctorTextView)
        val timeTextView = cardView.findViewById<TextView>(R.id.timeTextView)
        val cancelAppointment=cardView.findViewById<TextView>(R.id.cancelAppointment)

        var doctorName=""
        var doctorID=doctorId?:""
        var StringDocumentID=documentID?:""
        Log.d("1","$doctorID")
        Log.d("2","$doctorId")
        firestore().getOtherUserDetails(this, doctorID) { user ->
            if (user != null) {
                doctorName="DR "+user.firstname +" "+ user.lastname
                Log.d("3","$doctorName")
                doctorTextView.text = "Doctor: $doctorName"
            }
        }
        // Set the appointment data to the TextViews
        dateTextView.text = "Date: $date"

        Log.d("4","$doctorName")
        timeTextView.text = "Time: $time"

        cancelAppointment.setOnClickListener{
            firestore().deleteDocument(StringDocumentID,"appointments",
                onSuccess = {
                    // Create an Intent to restart the current activity
                    val intent = intent
                    finish() // Finish the current activity
                    startActivity(intent) // Start a new instance of the current activity
                },
                onFailure = { e ->
                    Log.w("ERROR", "Error deleting document", e)
                })
        }
        // Add the card view to the LinearLayout
        cardContainer.addView(cardView)
    }





}

