package com.example.hospitalmanagementapplication.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.hospitalmanagementapplication.*
import com.example.hospitalmanagementapplication.clerk.ClerkDashboardActivity
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class IntentManager(
    private val activity: Activity,
    private val bottomNavigationView: BottomNavigationView,
    private val position:Number) {

    init {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    if (position==1) {
                        startActivity(activity, HomeActivity::class.java)
                    }
                    if (position==2) {
                        startActivity(activity, DoctorHomeActivity::class.java)
                    }
                    if (position==3) {
                        startActivity(activity, ClerkDashboardActivity::class.java)
                    }
                    true
                }
                R.id.profile -> {
                    startActivity(activity, ProfileActivity::class.java)
                    true
                }
                R.id.others->{
                    startActivity(activity,healthActivity::class.java)
                    true
                }
                // Add more cases for additional items
                else -> false
            }
        }
    }

    private fun startActivity(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        context.startActivity(intent)
    }
}
