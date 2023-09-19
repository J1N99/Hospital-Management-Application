package com.example.hospitalmanagementapplication.firebase

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.util.Log
import com.example.hospitalmanagementapplication.HomeActivity
import com.example.hospitalmanagementapplication.doctor.DoctorAvailableAppoinmentActivity
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.example.hospitalmanagementapplication.model.Appointment
import com.example.hospitalmanagementapplication.model.AppointmentAvailable
import com.example.hospitalmanagementapplication.model.User
import com.example.hospitalmanagementapplication.userDetailsActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions


class firestore {
    private val mFirestore = FirebaseFirestore.getInstance()
    private val mFirebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

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


    fun changePasswordWithReauthentication(
        activity: Activity,
        oldPassword: String,
        newPassword: String,
        onCompleteListener: OnCompleteListener<Void>
    ) {
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        // Create a credential with the user's email and old password
        val credential: AuthCredential = EmailAuthProvider.getCredential(user?.email ?: "", oldPassword)

        // Re-authenticate the user with the provided credential
        user?.reauthenticate(credential)
            ?.addOnCompleteListener { reauthTask: Task<Void> ->
                if (reauthTask.isSuccessful) {
                    // Re-authentication successful, change the password
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { passwordUpdateTask: Task<Void> ->
                            onCompleteListener.onComplete(passwordUpdateTask)
                        }
                } else {

                    // Re-authentication failed. Show an alert to the user.
                    val alertDialog = AlertDialog.Builder(activity)
                        .setTitle("Authentication Failed")
                        .setMessage("The old password you entered is incorrect.")
                        .setPositiveButton("OK", null)
                        .create()

                    alertDialog.show()

                }
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
    fun getUserByIC(ic: String): Task<QuerySnapshot> {
        val usersCollection = mFirestore.collection("users")
        return usersCollection.whereEqualTo("ic", ic).get()
    }

    fun getEmailFromIC(ic: String, onComplete: (User?) -> Unit, onError: (Exception) -> Unit) {
        getUserByIC(ic)
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Get the first matching document
                    val userId = document.getString("id") ?: ""
                    val userIC = document.getString("ic") ?: ""
                    onComplete(User(userId, userIC))
                } else {
                    onComplete(null) // No matching document found
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun getUserByEmail(uid: String): Task<DocumentSnapshot> {
        val usersCollection = mFirestore.collection("users")
        return usersCollection.document(uid).get()
    }

    fun getEmailByUID(uid: String, onComplete: (String?) -> Unit, onError: (Exception) -> Unit) {
        getUserByEmail(uid)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val email = documentSnapshot.getString("email")
                    onComplete(email)
                } else {
                    onComplete(null) // No document found with the specified UID
                }
            }
            .addOnFailureListener { e ->
                onError(e)
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
                    val email=document.getString("email").toString()
                    val firstname = document.getString("firstname").toString()
                    val lastname = document.getString("lastname").toString()
                    val icNumber=document.getString("ic").toString()
                    val gender: Boolean = document.getBoolean("gender") ?: false
                    val dob=document.getString("dob").toString()
                    val position: Int = (document.getLong("position")?.toInt()) ?: 1

                    val user = User(userId,email ,firstname, lastname,gender,dob,icNumber,position )
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
    fun createAvailableAppointment(activity: DoctorAvailableAppoinmentActivity, appointmentAvailable: AppointmentAvailable) {
        mFirestore.collection("appointmentAvailable")
            .document()
            .set(appointmentAvailable, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d("Tag-Document ID", "Document added with ID: $documentReference")
                val intent = Intent(activity, DoctorHomeActivity::class.java)
                activity.startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }

    fun getAppointmentAvailable(activity: Activity, callback: (String?,AppointmentAvailable?) -> Unit) {
        // Get user in collection
        mFirestore.collection("appointmentAvailable")
            // Get documentation id from the field of users
            .whereEqualTo("userId",getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())
                // Assuming there is only one matching document, you can retrieve the first one
                val document = document.documents[0]
                // Get the document ID
                val documentId = document.id
                // Received the document ID and convert it into the User Data model object
                val appointmentAvailable = document.toObject(AppointmentAvailable::class.java)

                // Pass the user object to the callback
                callback(documentId,appointmentAvailable)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null,null) // Notify callback of the error
            }
    }
    fun getDoctorAppointmentAvailable(activity: Activity,doctorID:String, callback: (String?,AppointmentAvailable?) -> Unit) {
        // Get user in collection
        mFirestore.collection("appointmentAvailable")
            // Get documentation id from the field of users
            .whereEqualTo("userId",doctorID)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())
                // Assuming there is only one matching document, you can retrieve the first one
                val document = document.documents[0]
                // Get the document ID
                val documentId = document.id
                // Received the document ID and convert it into the User Data model object
                val appointmentAvailable = document.toObject(AppointmentAvailable::class.java)

                // Pass the user object to the callback
                callback(documentId,appointmentAvailable)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null,null) // Notify callback of the error
            }
    }
    fun getAppointment(doctorId: String, dateAppointment: String, formattedTime: String, callback: (Boolean) -> Unit) {
        // Create a Firestore query to check if an appointment exists
        val query = mFirestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("dateAppointment", dateAppointment)
            .whereEqualTo("time", formattedTime)

        query.get()
            .addOnSuccessListener { documents ->
                val appointmentExists = !documents.isEmpty
                callback(appointmentExists) // Call the callback with the result
            }
            .addOnFailureListener { e ->
                // Handle the query failure if needed
                // You can add logging or error handling here
                callback(false) // Call the callback with false in case of failure
            }
    }

    //select doctor
    fun getAllDoctor(callback: (List<User>) -> Unit) {
        val usersCollection = mFirestore.collection("users")
        val query: Query = usersCollection.whereEqualTo("position", 2)

        query.get()
            .addOnSuccessListener { result ->
                val doctorList = mutableListOf<User>()

                for (document in result) {
                    val userId = document.id
                    val firstname = document.getString("firstname").toString()
                    val lastname = document.getString("lastname").toString()


                    val user = User(userId, firstname, lastname )
                    doctorList.add(user)
                }

                callback(doctorList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }


    fun makeAppointment(doctorId: String, userId: String,dateAppointment:String,time: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val appointment = hashMapOf(
            "doctorId" to doctorId,
            "userId" to userId,
            "dateAppointment" to dateAppointment,
            "time" to time
        )

        mFirestore.collection("appointments")
            .add(appointment)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Failed to store: $e") }
    }

    fun getAndDisplayAppointments(callback: (List<Appointment>) -> Unit) {
        mFirestore.collection("appointments").whereEqualTo("userId", getCurrentUserID())
            .get()
            .addOnSuccessListener { documents ->
                val appointmentsList = mutableListOf<Appointment>()

                for (document in documents) {
                    val dateAppointment = document.getString("dateAppointment")
                    val doctorId = document.getString("doctorId")
                    val time = document.getString("time")
                    val documentID=document.id
                    // Create a data class for Appointment
                    val appointment = Appointment(documentID,dateAppointment, doctorId, time)
                    appointmentsList.add(appointment)
                }

                // Pass the list of appointments to the callback function
                callback(appointmentsList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors that occurred during the query
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }





        fun deleteDocument(documentId: String, collectionName: String,onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
            val collectionReference = mFirestore.collection(collectionName)
            val documentReference = collectionReference.document(documentId)

            documentReference
                .delete()
                .addOnSuccessListener {
                    // Document successfully deleted
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    // Handle errors here
                    onFailure(e)
                }

    }

}