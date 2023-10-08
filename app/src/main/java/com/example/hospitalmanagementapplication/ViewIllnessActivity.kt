package com.example.hospitalmanagementapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hospitalmanagementapplication.databinding.ActivityRedesignBinding
import com.example.hospitalmanagementapplication.databinding.ActivityViewhospitalBinding
import com.example.hospitalmanagementapplication.databinding.ActivityViewillnessBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import java.io.IOException
import java.util.*

class ViewIllnessActivity: AppCompatActivity() {
    private lateinit var binding: ActivityViewillnessBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewillnessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore().getAllIllness { allIllness ->
            // Here, you have access to the list of appointments retrieved from Firestore
            // You can use this list to create card views or update your UI
            for (illness in allIllness) {
                // Create card views or update UI elements with appointment data
                val documentID=illness.documentID
                val illnessName = illness.illnessName
                val actionTaken = illness.actionTaken
                val description = illness.description

                // Create card view or update UI here
                createCardView(documentID,illnessName, actionTaken, description)
            }
        }

        binding.searchBarText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterIllnessCards(query)
            }
        })
    }

    fun createCardView(
        documentID: String?,
        illnessName: String?,
        actionTaken: String?,
        description: String?
    ) {
        // Find the LinearLayout within the ConstraintLayout
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        // Inflate your card view layout here (e.g., from XML)
        val cardView = layoutInflater.inflate(R.layout.illness_card, null)

        // Set margins for the card view to create spacing between them
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.card_margin)
        ) // Adjust the margin as needed
        cardView.layoutParams = layoutParams

        val nameOfIllness = cardView.findViewById<TextView>(R.id.nameOfIllness)
        val descriptionID = cardView.findViewById<TextView>(R.id.description)
        val actionTakenID = cardView.findViewById<TextView>(R.id.actionTaken)


        // Display distance in the card view
        nameOfIllness.text =illnessName
        descriptionID.text =description
        actionTakenID.text =actionTaken

        // Add the card view to the LinearLayout
        cardContainer.addView(cardView)
    }


    fun filterIllnessCards(query: String) {
        val cardContainer = findViewById<LinearLayout>(R.id.cardContainer)

        for (i in 0 until cardContainer.childCount) {
            val cardView = cardContainer.getChildAt(i)
            val nameOfIllness = cardView.findViewById<TextView>(R.id.nameOfIllness)
            val descriptionID = cardView.findViewById<TextView>(R.id.description)
            val actionTakenID = cardView.findViewById<TextView>(R.id.actionTaken)

            val illnessName = nameOfIllness.text.toString()

            if (illnessName.contains(query, ignoreCase = true)) {
                cardView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
            }
        }
    }


}