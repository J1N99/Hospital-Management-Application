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

        //create collection names, is exist just use
        mFirestore.collection("users")
                //create document id
            .document(user.id)
            // We set the user object in the document, using SetOptions.merge() to merge data if the document already exists
            .set(user, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d("Tag-Document ID", "Document added with ID: $documentReference")

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