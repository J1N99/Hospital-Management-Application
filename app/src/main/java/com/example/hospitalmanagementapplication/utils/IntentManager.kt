package com.example.hospitalmanagementapplication.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.hospitalmanagementapplication.HomeActivity
import com.example.hospitalmanagementapplication.ProfileActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.AllUserActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class IntentManager(
    private val activity: Activity,
    private val bottomNavigationView: BottomNavigationView
) {

    init {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(activity, HomeActivity::class.java)
                    true
                }
                R.id.profile -> {
                    startActivity(activity, ProfileActivity::class.java)
                    true
                }
                R.id.settings->{
                    startActivity(activity,AllUserActivity::class.java)
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
