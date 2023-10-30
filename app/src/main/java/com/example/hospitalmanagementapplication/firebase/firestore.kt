package com.example.hospitalmanagementapplication.firebase

import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.hospitalmanagementapplication.clerk.ClerkDashboardActivity
import com.example.hospitalmanagementapplication.doctor.DoctorAvailableAppointmentActivity
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.example.hospitalmanagementapplication.model.*
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
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.os.Process
import com.example.hospitalmanagementapplication.*
import java.text.SimpleDateFormat
import java.util.Date


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

    fun getCurrentUserID(): String {
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
        val credential: AuthCredential =
            EmailAuthProvider.getCredential(user?.email ?: "", oldPassword)

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

    fun getEmailFromIC(
        ic: String,
        email: String,
        onComplete: (User?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        mFirestore.collection("users")
            .whereEqualTo("ic", ic)
            .whereEqualTo("email", email)
            .get()
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

    fun getOtherUserDetails(activity: Activity, userId: String, callback: (User?) -> Unit) {
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

    fun updateDocument(
        collectionName: String,
        documentId: String,
        data: Map<String, Any>,
        merge: Boolean = true
    ) {
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
                    val email = document.getString("email").toString()
                    val firstname = document.getString("firstname").toString()
                    val lastname = document.getString("lastname").toString()
                    val icNumber = document.getString("ic").toString()
                    val gender: Boolean = document.getBoolean("gender") ?: false
                    val dob = document.getString("dob").toString()
                    val position: Int = (document.getLong("position")?.toInt()) ?: 1

                    val user =
                        User(userId, email, firstname, lastname, gender, dob, icNumber, position)
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
        collectionName: String = "users"
    ) {
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

    fun createAvailableAppointment(
        activity: DoctorAvailableAppointmentActivity,
        appointmentAvailable: AppointmentAvailable
    ) {
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

    fun getAppointmentAvailable(
        activity: Activity,
        callback: (String?, AppointmentAvailable?) -> Unit
    ) {
        // Get user in collection
        mFirestore.collection("appointmentAvailable")
            // Get documentation id from the field of users
            .whereEqualTo("userId", getCurrentUserID())
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (!documentSnapshot.isEmpty) {
                    // Assuming there is only one matching document, you can retrieve the first one
                    val document = documentSnapshot.documents[0]
                    // Get the document ID
                    val documentId = document.id
                    // Received the document ID and convert it into the User Data model object
                    val appointmentAvailable = document.toObject(AppointmentAvailable::class.java)

                    // Pass the user object to the callback
                    callback(documentId, appointmentAvailable)
                } else {
                    // No documents found, notify callback with null values
                    callback(null, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null, null) // Notify callback of the error
            }
    }

    fun getDoctorAppointmentAvailable(
        activity: Activity,
        doctorID: String,
        callback: (String?, AppointmentAvailable?) -> Unit
    ) {
        // Get user in collection
        mFirestore.collection("appointmentAvailable")
            // Get documentation id from the field of users
            .whereEqualTo("userId", doctorID)
            .get()
            .addOnSuccessListener { document ->
                if (!document.isEmpty) {
                    Log.d(activity.javaClass.simpleName, document.toString())
                    // Assuming there is only one matching document, you can retrieve the first one
                    val document = document.documents[0]
                    // Get the document ID
                    val documentId = document.id
                    // Received the document ID and convert it into the User Data model object
                    val appointmentAvailable = document.toObject(AppointmentAvailable::class.java)

                    // Pass the user object to the callback
                    callback(documentId, appointmentAvailable)
                } else {
                    // No documents found, notify callback with null values
                    callback(null, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null, null) // Notify callback of the error
            }
    }

    fun getAppointment(
        doctorId: String,
        dateAppointment: String,
        formattedTime: String,
        callback: (Boolean) -> Unit
    ) {
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


                    val user = User(userId, firstname, lastname)
                    doctorList.add(user)
                }

                callback(doctorList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }


    fun makeAppointment(
        doctorId: String,
        userId: String,
        dateAppointment: String,
        time: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
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
                    val documentID = document.id
                    val userID = getCurrentUserID()
                    // Create a data class for Appointment
                    val appointment =
                        Appointment(documentID, dateAppointment, doctorId, time, userID)
                    appointmentsList.add(appointment)
                }

                // Sort the list of appointments by date and time
                val sortedAppointments = appointmentsList.sortedWith(compareByDescending<Appointment> { it.dateAppointment }.thenByDescending { it.time })


                // Pass the list of appointments to the callback function
                callback(sortedAppointments)
            }
            .addOnFailureListener { exception ->
                // Handle any errors that occurred during the query
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }


    fun doctorGetAndDisplayAppointments(
        formattedDate: String,
        callback: (List<Appointment>) -> Unit
    ) {
        mFirestore.collection("appointments")
            .whereEqualTo("doctorId", getCurrentUserID())
            .whereEqualTo("dateAppointment", formattedDate)
            .get()
            .addOnSuccessListener { documents ->
                val appointmentsList = mutableListOf<Appointment>()

                for (document in documents) {

                    val dateAppointment = document.getString("dateAppointment")
                    val doctorId = document.getString("doctorId")
                    val time = document.getString("time")
                    val documentID = document.id
                    val userID = document.getString("userId")
                    // Create a data class for Appointment
                    val appointment =
                        Appointment(documentID, dateAppointment, doctorId, time, userID)
                    appointmentsList.add(appointment)
                    Log.e("WHY", dateAppointment ?: "")

                }
                // Sort the list by time
                val sortedAppointmentsList = appointmentsList.sortedWith(compareBy { it.time })

                // Pass the sorted list of appointments to the callback function
                callback(sortedAppointmentsList)

            }
            .addOnFailureListener { exception ->
                // Handle any errors that occurred during the query
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }


    fun deleteDocument(
        documentId: String,
        collectionName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
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


    fun disableAppointment(
        dateAppointment: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val disableAppointmentDate = hashMapOf(
            "doctorId" to getCurrentUserID(),
            "dateAppointment" to dateAppointment,

            )

        mFirestore.collection("disableAppointmentDate")
            .add(disableAppointmentDate)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Failed to store: $e") }
    }

    fun disableAppointment(doctorID: String, callback: (List<DisableAppointment>) -> Unit) {
        // Create a Firestore query to check if an appointment exists
        val query = mFirestore.collection("disableAppointmentDate")
            .whereEqualTo("doctorId", doctorID)


        query.get()
            .addOnSuccessListener { documents ->

                val disableAppointmentsList = mutableListOf<DisableAppointment>()

                for (document in documents) {
                    val documentID = document.id
                    val dateAppointment = document.getString("dateAppointment")
                    val doctorId = doctorID
                    // Create a data class for Appointment
                    val disableAppointment =
                        DisableAppointment(documentID, dateAppointment, doctorId)
                    disableAppointmentsList.add(disableAppointment)


                }

                // Pass the list of appointments to the callback function
                callback(disableAppointmentsList)
            }
            .addOnFailureListener { e ->
                // Handle the query failure if needed
                // You can add logging or error handling here
                Log.w("Firestore", "Error getting documents: ", e)
            }
    }


    fun getAllDisableAppointment(doctorID: String, callback: (List<DisableAppointment>) -> Unit) {
        val disableAppointmentCollection = mFirestore.collection("disableAppointmentDate")
        val query: Query = disableAppointmentCollection.whereEqualTo("doctorId", doctorID)

        query.get()
            .addOnSuccessListener { result ->
                val disableAppointmentDateList = mutableListOf<DisableAppointment>()

                for (document in result) {
                    val documentID = document.id
                    val disableAppointmentDate = document.getString("dateAppointment").toString()


                    val disableAppointment =
                        DisableAppointment(documentID, disableAppointmentDate, doctorID)
                    disableAppointmentDateList.add(disableAppointment)
                }

                callback(disableAppointmentDateList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }

    fun createDoctorInformation(activity: Activity, doctorInfo: doctorInformation) {

        //create collection names, is exist just use
        mFirestore.collection("doctorInformation")
            //create document id
            .document(doctorInfo.userID ?: "")
            // We set the user object in the document, using SetOptions.merge() to merge data if the document already exists
            .set(doctorInfo, SetOptions.merge())
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


    fun getDoctorInfo(
        activity: Activity,
        doctorID: String,
        callback: (doctorInformation?) -> Unit
    ) {
        // Get user in collection
        mFirestore.collection("doctorInformation")
            // Get documentation id from the field of users
            .document(doctorID)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val doctorInfo = document.toObject(doctorInformation::class.java)

                // Pass the user object to the callback
                callback(doctorInfo)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null) // Notify callback of the error
            }
    }

    fun getAllDoctorFromDoctorInfo(callback: (List<doctorInformation>) -> Unit) {
        mFirestore.collection("doctorInformation")
            .get()
            .addOnSuccessListener { documents ->
                Log.e("WHY", "KENAPA")
                val doctorList = mutableListOf<doctorInformation>()

                for (document in documents) {
                    Log.e("got mah", "got")
                    val doctorId = document.getString("userID") ?: ""
                    val department = document.getString("department") ?: ""
                    val hospital = document.getString("hospital") ?: ""
                    val profileImageUri = document.getString("profileImageUri") ?: ""
                    val quanlification = document.getString("quanlification") ?: ""
                    // Create a data class for Appointment
                    val doctorInfo = doctorInformation(
                        doctorId,
                        department,
                        quanlification,
                        profileImageUri,
                        hospital
                    )
                    doctorList.add(doctorInfo)
                }

                // Pass the list of appointments to the callback function
                callback(doctorList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors that occurred during the query
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }


    fun createHospital(activity: Activity, hospitalInfo: Hospital) {

        //create collection names, is exist just use
        mFirestore.collection("hospital")
            //create document id
            .document()
            // We set the user object in the document, using SetOptions.merge() to merge data if the document already exists
            .set(hospitalInfo, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d("Tag-Document ID", "Document added with ID: $documentReference")
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }


    fun getAllHospital(callback: (List<Hospital>) -> Unit) {

        mFirestore.collection("hospital").get()
            .addOnSuccessListener { result ->
                val hospitalList = mutableListOf<Hospital>()

                for (document in result) {
                    val documentId = document.id
                    val hospital = document.getString("hospital").toString()
                    val address = document.getString("address").toString()
                    val privateGovernment = document.getString("privateGovernment").toString()

                    val hospitals = Hospital(privateGovernment, hospital, address, documentId)
                    hospitalList.add(hospitals)
                }

                callback(hospitalList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }


    fun getHospitalDetails(activity: Activity, hospitalId: String, callback: (Hospital?) -> Unit) {
        // Get user in collection
        mFirestore.collection("hospital")
            // Get documentation id from the field of users
            .document(hospitalId)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val hospital = document.toObject(Hospital::class.java)

                // Pass the user object to the callback
                callback(hospital)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null) // Notify callback of the error
            }
    }


    fun getIllnessActivity(activity: Activity, illnessID: String, callback: (Illness?) -> Unit) {
        // Get user in collection
        mFirestore.collection("illness")
            // Get documentation id from the field of users
            .document(illnessID)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val illness = document.toObject(Illness::class.java)
                if (illness != null) {
                    Log.e("XIXI", illness.illnessName)
                }
                // Pass the user object to the callback
                callback(illness)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting illness details: $e")
                callback(null) // Notify callback of the error
            }
    }

    fun createIllness(activity: Activity, illnessInfo: Illness) {

        //create collection names, is exist just use
        mFirestore.collection("illness")
            //create document id
            .document()
            // We set the user object in the document, using SetOptions.merge() to merge data if the document already exists
            .set(illnessInfo, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d("Tag-Document ID", "Document added with ID: $documentReference")

                // Create an Intent to start the HomeActivity
                val intent = Intent(activity, ClerkDashboardActivity::class.java)
                // Start the HomeActivity using the intent
                activity.startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }


    fun getAllIllness(callback: (List<Illness>) -> Unit) {

        mFirestore.collection("illness").get()
            .addOnSuccessListener { result ->
                val illnessList = mutableListOf<Illness>()

                for (document in result) {
                    val documentId = document.id
                    val nameOfIllness = document.getString("illnessName").toString()
                    val description = document.getString("description").toString()
                    val actionTaken = document.getString("actionTaken").toString()
                    Log.e(nameOfIllness, nameOfIllness)
                    val illness = Illness(documentId, nameOfIllness, description, actionTaken)
                    illnessList.add(illness)
                }

                callback(illnessList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }

    fun createOrUpdateAnnouncement(announcementTitle: String, announcement: String): Task<Void> {
        val documentPath = "announcements/3kRiDZZwywBhqcnwi9DC"
        // Create a map to hold the title and content
        val announcementData = hashMapOf(
            "announcementTitle" to announcementTitle,
            "announcement" to announcement
        )

        // Use set with merge option to create or update the document
        return mFirestore.document(documentPath)
            .set(announcementData, SetOptions.merge())
    }

    fun getAnnouncement(callback: (Map<String, Any>?) -> Unit) {
        mFirestore.collection("announcements")
            .document("3kRiDZZwywBhqcnwi9DC")
            .get()
            .addOnSuccessListener { documentSnapshot ->

                if (documentSnapshot.exists()) {
                    val announcementData = documentSnapshot.data
                    callback(announcementData)
                } else {
                    // Document does not exist
                    callback(null)
                }

            }
            .addOnFailureListener { exception ->
                // Handle any errors that occurred during the query
                Log.e("Firestore", "Error getting document: $exception")
                callback(null)
            }
    }

    fun createPDFInfo(activity: Activity, pdfInfos: PDFInfo, callback: (String?) -> Unit) {
        // Create or update a document in the "pdfinfo" collection
        mFirestore.collection("pdfinfo")
            .add(pdfInfos) // No need to specify a document ID here
            .addOnSuccessListener { documentReference ->
                // The document has been successfully added or updated
                val documentId = documentReference.id
                Log.d("Tag-Document ID", "Document added with ID: $documentId")
                callback(documentId) // Call the callback with the document ID
            }
            .addOnFailureListener { e ->
                // Handle the error if document creation/update fails
                Log.w("Tag-Document ID", "Error adding/updating document", e)
                callback(null) // Call the callback with null to indicate failure
            }
    }


    //this is for print out PDF
    fun getPDFInfo(activity: Activity, pdfDocumentID: String, callback: (PDFInfo?) -> Unit) {
        // Get user in the collection
        mFirestore.collection("pdfinfo")
            // Get document by ID
            .document(pdfDocumentID)
            .get()
            .addOnSuccessListener { document ->
                val documentId = document.id
                val illness = document.getString("illness") ?: ""
                val medicine = document.getString("medicine") ?: ""
                val action = document.getString("action") ?: ""
                val pdfInfo = PDFInfo(documentId, illness, medicine, action, "")
                callback(pdfInfo)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting PDF info: $e")
                callback(null)
            }
    }


    //retrieve to see the data
    fun checkPDFandDisplay(
        activity: Activity,
        appointmentID: String,
        callback: (List<PDFInfo>) -> Unit
    ) {
        // Create a Firestore query to check if an appointment exists
        val query = mFirestore.collection("pdfinfo")
            .whereEqualTo("appointmentID", appointmentID)

        query.get()
            .addOnSuccessListener { documents ->
                val pdfInfoList = mutableListOf<PDFInfo>()

                for (document in documents) {
                    val documentID = document.id
                    val action = document.getString("action") ?: ""
                    val illness = document.getString("illness") ?: ""
                    val medicine = document.getString("medicine") ?: ""
                    val patientID = document.getString("patientID") ?: ""
                    val pdfname = document.getString("pdfname") ?: ""

                    val pdfinfo = PDFInfo(
                        documentID,
                        illness,
                        medicine,
                        action,
                        patientID,
                        appointmentID,
                        pdfname
                    )
                    pdfInfoList.add(pdfinfo)
                }

                // Pass the list of PDFInfo objects to the callback function
                callback(pdfInfoList)
            }
            .addOnFailureListener { e ->
                // Handle the query failure if needed
                // You can add logging or error handling here
                Log.w("Firestore", "Error getting documents: ", e)
                // Notify the callback of the failure by passing an empty list or an error indicator
                callback(emptyList())
            }
    }


    fun getAllMedicine(callback: (List<Medicine>) -> Unit) {

        mFirestore.collection("medicine").get()
            .addOnSuccessListener { result ->
                val medicineList = mutableListOf<Medicine>()

                for (document in result) {
                    val documentId = document.id
                    val medicineName = document.getString("medicineName").toString()
                    val description = document.getString("description").toString()
                    val medicationTime = document.getString("medicationTime").toString()

                    val medicine = Medicine(documentId, medicineName, description, medicationTime)
                    medicineList.add(medicine)
                }

                callback(medicineList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }


    fun getMedicineActivity(activity: Activity, medicineID: String, callback: (Medicine?) -> Unit) {
        // Get user in collection
        mFirestore.collection("medicine")
            // Get documentation id from the field of users
            .document(medicineID)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val medicine = document.toObject(Medicine::class.java)
                if (medicine != null) {
                    Log.e("XIXI", medicine.medicineName)
                }
                // Pass the user object to the callback
                callback(medicine)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting illness details: $e")
                callback(null) // Notify callback of the error
            }
    }

    fun createMedicine(activity: Activity, medicineInfo: Medicine) {

        //create collection names, is exist just use
        mFirestore.collection("medicine")
            //create document id
            .document()
            // We set the user object in the document, using SetOptions.merge() to merge data if the document already exists
            .set(medicineInfo, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d("Tag-Document ID", "Document added with ID: $documentReference")

            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }


    suspend fun getIllnessByName(
        activity: Activity,
        medicineName: String
    ): Medicine? {
        try {
            val querySnapshot = mFirestore.collection("medicine")
                .whereEqualTo("medicineName", medicineName)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                return document.toObject(Medicine::class.java)
            } else {
                return null
            }
        } catch (e: Exception) {
            Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
            return null
        }
    }


        fun checkAndScheduleAppointments(context: Context) {
            // Get the current date and time
            val currentDateTime = Date()

            // Set the date format for parsing appointment date and time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

            mFirestore.collection("appointments")
                .whereEqualTo("userId", getCurrentUserID())
                .get()
                .addOnSuccessListener { queryDocumentSnapshots ->
                    for (document in queryDocumentSnapshots) {
                        // Parse appointment data
                        val appointmentId = document.id
                        val dateAppointmentString = document.getString("dateAppointment")
                        val timeString = document.getString("time")

                        // Convert dateAppointmentString and timeString to Date objects
                        val appointmentDateTimeString = "$dateAppointmentString $timeString"
                        val appointmentDateTime = dateFormat.parse(appointmentDateTimeString)

                        // Calculate time difference
                        val currentTime = currentDateTime.time
                        val appointmentTimeMillis = appointmentDateTime.time
                        val timeDiff = appointmentTimeMillis - currentTime

                        // Set the notification threshold to 2 hour (in milliseconds)
                        val notificationThreshold = 7200000  // 2 hour in milliseconds

                        Log.e("Second",timeDiff.toString())
                        Log.e("Third",notificationThreshold.toString())



                        if (timeDiff >=0 && timeDiff<=notificationThreshold) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel = NotificationChannel("Appoint Reminder", "Appointment Reminders", NotificationManager.IMPORTANCE_HIGH)
                                val notificationManager = context.getSystemService(NotificationManager::class.java)
                                notificationManager?.createNotificationChannel(channel)
                            }
                            Log.e("RUN","POKAI")

                            val intent = Intent(context, ViewAppointmentActivity::class.java)
                            val pendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )


                            // Schedule a notification for this appointment
                            val builder = NotificationCompat.Builder(context, "Appoint Reminder")
                                .setSmallIcon(R.drawable.logo)
                                .setContentTitle("Appointment Reminder")
                                .setContentText("Your $appointmentDateTimeString appointment is in 2 hour.")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setContentIntent(pendingIntent)

                            // Create a unique notification ID based on appointment time
                            val notificationId = appointmentTimeMillis.toInt()

                            // Show the notification
                            val notificationManager = NotificationManagerCompat.from(context)
                            notificationManager.notify(notificationId, builder.build())
                        }
                    }
                }
        }


    fun getHealthReport(
        activity: Activity,
        callback: (List<healthReport>) -> Unit
    ) {
        // Create a Firestore query to get the user's health report
        val query = mFirestore.collection("healthReport")
            .document(getCurrentUserID())

        query.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val healthReportList = mutableListOf<healthReport>()

                    // Extract data from the document snapshot
                    val data = documentSnapshot.data
                    if (data != null) {
                        val bloodType = data["bloodType"] as String? ?: ""
                        val height = data["height"] as String? ?: ""
                        val weight = data["weight"] as String? ?: ""

                        val healthReport = healthReport(bloodType, height, weight)
                        healthReportList.add(healthReport)
                    }

                    // Pass the list of HealthReport objects to the callback function
                    callback(healthReportList)
                } else {
                    // If the document doesn't exist, pass an empty list to the callback
                    callback(emptyList())
                }
            }
            .addOnFailureListener { e ->
                // Handle the query failure if needed
                // You can add logging or error handling here
                Log.w("Firestore", "Error getting document: ", e)

                // Notify the callback of the failure by passing an empty list or an error indicator
                callback(emptyList())
            }
    }

    fun getAllDepartment(callback: (List<department>) -> Unit) {

        mFirestore.collection("department").get()
            .addOnSuccessListener { result ->
                val departmentList = mutableListOf<department>()

                for (document in result) {
                    val documentId = document.id
                    val department = document.getString("department").toString()


                    val departmentDetails = department(documentId, department)
                    departmentList.add(departmentDetails)
                }

                callback(departmentList)
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                callback(emptyList()) // You can also return an empty list or handle errors differently
            }
    }


    fun getDepartmentDetails(activity: Activity, departmentID: String, callback: (department?) -> Unit) {
        // Get user in collection
        mFirestore.collection("department")
            // Get documentation id from the field of users
            .document(departmentID)
            .get()
            .addOnSuccessListener { document ->
                Log.d(activity.javaClass.simpleName, document.toString())

                // Received the document ID and convert it into the User Data model object
                val departments = document.toObject(department::class.java)

                // Pass the user object to the callback
                callback(departments)
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting user details: $e")
                callback(null) // Notify callback of the error
            }
    }

    fun createDepartment(activity: Activity, department: department) {

        //create collection names, is exist just use
        mFirestore.collection("department")
            //create document id
            .document()
            // We set the user object in the document, using SetOptions.merge() to merge data if the document already exists
            .set(department, SetOptions.merge())
            .addOnSuccessListener { documentReference ->
                Log.d("Tag-Document ID", "Document added with ID: $documentReference")
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }






}