package com.example.hospitalmanagementapplication

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityBookingBinding
import com.example.hospitalmanagementapplication.databinding.ActivityMainBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: ActivityBookingBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

   // private val availableDay = arrayOf(Calendar.TUESDAY, Calendar.FRIDAY)
    private val availableDays = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.home)
        IntentManager(this, bottomNavigationView)

        var doctorID = intent.getStringExtra("doctorID") ?: ""
        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getOtherUserDetails(this, doctorID) { user ->
            if (user != null) {
                binding.doctorNameTextView.text = "DR" + user.lastname + " " + user.firstname
            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }

        firestore().getDoctorAppointmentAvailable(this, doctorID) { _, appointmentAvailable ->
            if (appointmentAvailable != null) {
                val daysOfWeek = arrayOf(
                    Calendar.MONDAY,
                    Calendar.TUESDAY,
                    Calendar.WEDNESDAY,
                    Calendar.THURSDAY,
                    Calendar.FRIDAY,
                    Calendar.SATURDAY,
                    Calendar.SUNDAY
                )
                val appointmentDays = arrayOf(
                    appointmentAvailable.monday,
                    appointmentAvailable.tuesday,
                    appointmentAvailable.wednesday,
                    appointmentAvailable.thursday,
                    appointmentAvailable.friday,
                    appointmentAvailable.saturday,
                    appointmentAvailable.sunday
                )

                for (i in daysOfWeek.indices) {
                    if (appointmentDays[i]) {
                        availableDays.add(daysOfWeek[i])
                    }
                }


                initializeDatePicker()
            } else {
                Log.d("Fail", "Fail")
                Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeDatePicker() {
        // Initialize date picker
        val currentDate = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog.newInstance(
            this@BookingActivity,
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )

        // Calculate the maximum date (3 months from now)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 3)

        // Set the minimum and maximum date for the date picker
        datePickerDialog.minDate = currentDate
        datePickerDialog.maxDate = maxDate

        // Disable specific days for the next three months
        val disabledDays = ArrayList<Calendar>()

        while (currentDate.before(maxDate) || currentDate == maxDate) {
            if (!availableDays.contains(currentDate.get(Calendar.DAY_OF_WEEK))) {
                Log.d("Days", "$currentDate")
                disabledDays.add(currentDate.clone() as Calendar)
            }

            currentDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        val disabledDaysArray = disabledDays.toTypedArray()
        datePickerDialog.disabledDays = disabledDaysArray

        // Set listener for date picker
        binding.bookingAppointmentET.setOnClickListener {
            Log.d("CLICK", "CLICKED")
            datePickerDialog.show(supportFragmentManager, "DatePickerDialog")
        }
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        // Handle date selection here
        // You can perform the booking process here or set the selected date in the EditText
        val selectedDate = Calendar.getInstance()
        selectedDate.set(year, monthOfYear, dayOfMonth)

        // Format the selected date as needed (e.g., "yyyy-MM-dd")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormat.format(selectedDate.time)

        // Set the formatted date in the EditText
        binding.bookingAppointmentET.setText(formattedDate)
    }

}
