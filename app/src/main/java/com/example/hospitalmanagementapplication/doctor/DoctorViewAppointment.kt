package com.example.hospitalmanagementapplication.doctor

import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.Loader
import java.text.SimpleDateFormat
import java.util.Calendar



class DoctorViewAppointment : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterTimeSlots
    private lateinit var progressDialog: Loader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctorviewappointment)

        // Initialize your data (time slots and events)
        val timeSlots = mutableListOf<String>()
        val events = mutableListOf<String>()

        // Get the current date using Calendar
        val calendar = Calendar.getInstance()
        // Create a SimpleDateFormat object to format the date
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        // Format the date as "YYYY/MM/DD"
        val formattedDate = sdf.format(calendar.time)
        Log.d("Date", formattedDate ?: "")

        progressDialog = Loader(this)
        progressDialog.show()

        firestore().doctorGetAndDisplayAppointments(formattedDate) { appointments ->
            for (appointment in appointments) {
                val time = appointment.time
                val userID = appointment.userID

                firestore().getOtherUserDetails(this, userID ?: "") { user ->
                    if (user != null) {
                        events.add("Appointment with " + user.firstname + " " + user.lastname)
                        timeSlots.add(time ?: "")

                        // Check if the number of events matches the number of appointments
                        if (events.size == appointments.size) {
                            // All data has been added, you can now dismiss the progress dialog
                            progressDialog.dismiss()

                            // Initialize the RecyclerView and adapter
                            recyclerView = findViewById(R.id.recyclerView)
                            recyclerView.layoutManager = LinearLayoutManager(this)
                            adapter = AdapterTimeSlots(timeSlots, events)
                            recyclerView.adapter = adapter
                        }
                    }
                }
            }
        }

        // Calculate the minimum date (e.g., one month ago from the current date)
        val minDate = Calendar.getInstance()
        // Calculate the maximum date (e.g., one month from the current date)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 3)

        // Set the minimum and maximum dates for the CalendarView
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        calendarView.minDate = minDate.timeInMillis
        calendarView.maxDate = maxDate.timeInMillis
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            progressDialog = Loader(this)
            progressDialog.show()
            // Clear the timeSlots and events lists
            timeSlots.clear()
            events.clear()

            // Notify the RecyclerView adapter that the data has changed (if you're using a RecyclerView)
            adapter.notifyDataSetChanged()

            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            firestore().doctorGetAndDisplayAppointments(selectedDate) { appointments ->
                for (appointment in appointments) {
                    val time = appointment.time
                    val userID = appointment.userID

                    firestore().getOtherUserDetails(this, userID ?: "") { user ->
                        if (user != null) {
                            events.add("Appointment with " + user.firstname + " " + user.lastname)
                            timeSlots.add(time ?: "")

                            // Check if the number of events matches the number of appointments
                            if (events.size == appointments.size) {
                                // All data has been added, you can now dismiss the progress dialog


                                // Initialize the RecyclerView and adapter
                                recyclerView = findViewById(R.id.recyclerView)
                                recyclerView.layoutManager = LinearLayoutManager(this)
                                adapter = AdapterTimeSlots(timeSlots, events)
                                recyclerView.adapter = adapter
                            }
                        }
                    }
                }
                progressDialog.dismiss()
            }
        }
    }
}