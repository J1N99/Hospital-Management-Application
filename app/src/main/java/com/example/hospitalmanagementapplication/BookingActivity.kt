package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityBookingBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: ActivityBookingBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    // private val availableDay = arrayOf(Calendar.TUESDAY, Calendar.FRIDAY)
    private val availableDays = mutableListOf<Int>()
    private var appointmentAvailableStartTime = ""
    private var appointmentAvailableEndTime = ""
    private var doctorID=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        doctorID= intent.getStringExtra("doctorID") ?: ""

        Log.e(doctorID,doctorID)
        firebaseAuth = FirebaseAuth.getInstance()


        firestore().getOtherUserDetails(this, doctorID) { user ->
            if (user != null) {
                binding.doctorNameTextView.text = "DR " + user.lastname + " " + user.firstname
            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }

        firestore().getDoctorAppointmentAvailable(this, doctorID) { _, appointmentAvailable ->

            appointmentAvailableStartTime = appointmentAvailable?.appointmentStartTime ?: ""
            appointmentAvailableEndTime = appointmentAvailable?.appointmentEndTime ?: ""
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
                val handler = Handler(Looper.getMainLooper())

                // Define a delay of 3 seconds (3000 milliseconds)
                val delayMillis: Long = 3000

                // Use the Handler to post a delayed action
                handler.postDelayed({
                    // Create an Intent to navigate to the target activity
                    val intent = Intent(this, BookingActivity::class.java)

                    // Start the new activity
                    startActivity(intent)

                    // Finish the current activity (splash screen)
                    finish()
                }, delayMillis)
                Toast.makeText(this, "Doctor did not open for appointment yet", Toast.LENGTH_LONG).show()
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
        currentDate.add(Calendar.DAY_OF_MONTH, 1)
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
        firestore().disableAppointment(doctorID) { disableAppointmentDate ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (disableAppointmentDates in disableAppointmentDate) {
                try {
                    val disableAppointmentDate = disableAppointmentDates.dateAppointment
                    val date = dateFormat.parse(disableAppointmentDate)

                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        disabledDays.add(calendar)
                        Log.e("Success", "Success")
                    } else {
                        Log.e("ParsingError", "Failed to parse date: $disableAppointmentDate")
                    }
                } catch (e: Exception) {
                    Log.e("ParsingError", "Exception while parsing date: ${e.message}")
                }
            }


            for (calendar in disabledDays) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)
                Log.e("DisabledDate", formattedDate)
            }

            val disabledDaysArray = disabledDays.toTypedArray()
            datePickerDialog.disabledDays = disabledDaysArray
        }

        // Set listener for date picker
        binding.bookingAppointmentET.setOnClickListener {
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
        destroyAllButtons()
        generateButtonsBetweenTimes(appointmentAvailableStartTime, appointmentAvailableEndTime)
    }

    private fun generateButtonsBetweenTimes(startTime: String, endTime: String) {
        var intStartTime = startTime.toInt()
        val intEndTime = endTime.toInt()
        val timeInterval = 20 // Assuming the interval is 20 minutes

        // Find the existing GridLayout from the XML layout
        val gridLayout = findViewById<GridLayout>(R.id.LayoutButton)

        // Set the number of buttons to display per row
        val buttonsPerRow = 3
        gridLayout.columnCount = buttonsPerRow

        while (intStartTime < intEndTime) {
            val button = Button(this)
            val hours = intStartTime / 100
            val minutes = intStartTime % 100
            val formattedTime = String.format("%02d:%02d", hours, minutes)
            val dateAppointment = binding.bookingAppointmentET.text.toString()
            button.text = formattedTime

            firestore().getAppointment(doctorID, dateAppointment, formattedTime) { appointmentExists ->
                if (appointmentExists) {
                    button.isEnabled = false
                }
            }

            button.setOnClickListener {
                // Handle button click for the specific time
                showConfirmationDialog(dateAppointment, formattedTime) { confirmed ->
                    if (confirmed) {
                        // Store the selected time in Firestore
                        val selectedTime = formattedTime
                        val currentUser = firebaseAuth.currentUser
                        val userId = currentUser?.uid.toString()

                        firestore().makeAppointment(
                            doctorID,
                            userId,
                            dateAppointment,
                            selectedTime,
                            {
                                // Once success, refresh the page
                                refreshActivity()
                            },
                            { errorMessage ->
                                // Handle the error while storing in Firestore
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Add layout parameters for the button (width and height)
            val buttonParams = GridLayout.LayoutParams()
            buttonParams.width = 0 // Set width to 0 to evenly distribute buttons
            buttonParams.height = GridLayout.LayoutParams.WRAP_CONTENT
            buttonParams.columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED, // Let the GridLayout handle the column placement
                GridLayout.FILL,
                1f // Set column weight to evenly distribute buttons
            )

            button.layoutParams = buttonParams

            // Add the button to the GridLayout
            gridLayout.addView(button)

            // Increment intStartTime by the time interval, considering rollover at 60 minutes
            val newMinutes = (minutes + timeInterval) % 60
            val hourAdjustment = (minutes + timeInterval) / 60
            intStartTime = (hours + hourAdjustment) * 100 + newMinutes
        }
    }







    private fun showConfirmationDialog(dateAppointment:String,time: String, callback: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Appointment")
        builder.setMessage("Do you want to book the appointment at $dateAppointment $time?")

        builder.setPositiveButton("Yes") { _, _ ->
            callback(true)
        }

        builder.setNegativeButton("No") { _, _ ->
            callback(false)
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun refreshActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }

    private fun destroyAllButtons() {
        binding.LayoutButton.removeAllViews() // This will remove all child views (buttons) from the LinearLayout
    }
}

