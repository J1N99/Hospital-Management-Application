package com.example.hospitalmanagementapplication.firebase

import android.content.ContentValues
import android.content.Intent
import android.media.session.MediaSessionManager.RemoteUserInfo
import android.provider.SyncStateContract.Constants
import android.util.Log
import com.example.hospitalmanagementapplication.HomeActivity
import com.example.hospitalmanagementapplication.model.User
import com.example.hospitalmanagementapplication.userDetailsActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class firestore {
    private val mFirestore = FirebaseFirestore.getInstance()

    fun registerUserDetails(activity: userDetailsActivity, user: User) {


        mFirestore.collection("users")

            .document(user.id)

            .set(user, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d(ContentValues.TAG, "DocumentSnapshot added with ID: $documentReference")

                // Create an Intent to start the HomeActivity
                val intent = Intent(activity, HomeActivity::class.java)

                // Start the HomeActivity using the intent
                activity.startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }
}