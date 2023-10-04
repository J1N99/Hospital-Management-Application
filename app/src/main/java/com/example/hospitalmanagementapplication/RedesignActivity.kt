package com.example.hospitalmanagementapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.hospitalmanagementapplication.databinding.ActivityViewappointmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.databinding.ActivityRedesignBinding
import com.example.hospitalmanagementapplication.model.Hospital
import com.example.hospitalmanagementapplication.model.doctorInformation


class RedesignActivity: AppCompatActivity() {
    private lateinit var binding: ActivityRedesignBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected:Any
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRedesignBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        val items= listOf("Private","Government")
        val autoComplete:AutoCompleteTextView=findViewById(R.id.autoCompleteTextView)
        val adapter=ArrayAdapter(this,R.layout.list_private_government,items)
        autoComplete.setAdapter(adapter)
        autoComplete.onItemClickListener=AdapterView.OnItemClickListener { parent, view, position, id ->
            itemSelected=parent.getItemAtPosition(position)
            Toast.makeText(this,"Item: $itemSelected",Toast.LENGTH_SHORT).show()
        }

        binding.button.setOnClickListener{
            var privateGovernment=itemSelected.toString()
            var hospital=binding.hospitalET.text.toString().trim()
            var address=binding.addressET.text.toString().trim()

            if (privateGovernment.isEmpty() && hospital.isEmpty() && address.isEmpty()) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            }
            else
            {
                val hospitalInfo =
                    Hospital(privateGovernment, hospital, address)
                firestore().createHospital(this, hospitalInfo)
            }
        }

    }


}

