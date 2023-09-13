package com.example.hospitalmanagementapplication.firebase

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.hospitalmanagementapplication.HomeActivity
import com.example.hospitalmanagementapplication.model.User
import com.example.hospitalmanagementapplication.userDetailsActivity
import com.google.firebase.auth.FirebaseAuth
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
    private fun getCurrentUserID(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }
    fun getUserDetails(activity: Activity, callback: (User?) -> Unit) {
        // Get user in collection
        mFirestore.collection("users")
            // Get documentation id from the field of users
            .document(getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val user = document.toObject(User::class.java)

                // Pass the user object to the callback
                callback(user)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null) // Notify callback of the error
            }
    }


    fun getUserPosition(activity: Activity, callback: (Int?) -> Unit) {
        // Get user in collection
        mFirestore.collection("users")
            // Get document id from the field of users
            .document(getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->
                val position = document.getLong("position")?.toInt()
                callback(position)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user position: $e")
                callback(null)
            }
    }


    fun getOtherUserDetails(activity: Activity, userId:String,callback: (User?) -> Unit) {
        // Get user in collection
        mFirestore.collection("users")
            // Get documentation id from the field of users
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val user = document.toObject(User::class.java)

                // Pass the user object to the callback
                callback(user)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null) // Notify callback of the error
            }
    }
    fun updateDocument(collectionName: String, documentId: String, data: Map<String, Any>, merge: Boolean = true) {
        val documentReference = mFirestore.collection(collectionName).document(documentId)

        if (merge) {
            // Use merge option to merge the new data with existing data, creating the document if it doesn't exist
            documentReference.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    println("Success update")
                }
                .addOnFailureListener { e ->
                    // Handle errors
                    println("Error updating document: $e")
                }
        } else {
            // Overwrite existing data with the new data
            documentReference.set(data)
                .addOnSuccessListener {
                    println("Success update")
                }
                .addOnFailureListener { e ->
                    // Handle errors
                    println("Error updating document: $e")
                }
        }
    }


    fun getAllUsers(callback: (List<User>) -> Unit) {
        val usersCollection = mFirestore.collection("users")

        usersCollection.get()
            .addOnSuccessListener { result ->
                val userList = mutableListOf<User>()

                for (document in result) {
                    val userId = document.id
                    val firstname = document.getString("firstname").toString()
                    val lastname = document.getString("lastname").toString()
                    val icNumber=document.getString("ic").toString()
                    val gender: Boolean = document.getBoolean("gender") ?: false
                    val dob=document.getString("dob").toString()
                    val position: Int = (document.getLong("position")?.toInt()) ?: 1

                    val user = User(userId, firstname, lastname,gender,dob,icNumber,position )
                    userList.add(user)
                }

                callback(userList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }


    fun updatePosition(
        documentId: String,
        data: Map<String, Any>,
        merge: Boolean = true,
        collectionName: String ="users") {
        val documentReference = mFirestore.collection(collectionName).document(documentId)

        if (merge) {
            // Use merge option to merge the new data with existing data, creating the document if it doesn't exist
            documentReference.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    println("Success update")
                }
                .addOnFailureListener { e ->
                    // Handle errors
                    println("Error updating document: $e")
                }
        } else {
            // Overwrite existing data with the new data
            documentReference.set(data)
                .addOnSuccessListener {
                    println("Success update")
                }
                .addOnFailureListener { e ->
                    // Handle errors
                    println("Error updating document: $e")
                }
        }
    }

}