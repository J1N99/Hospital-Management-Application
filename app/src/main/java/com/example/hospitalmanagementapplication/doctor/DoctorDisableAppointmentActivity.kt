package com.example.hospitalmanagementapplication.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.UpdatePositionActivity
import com.example.hospitalmanagementapplication.databinding.ActivityBookingBinding
import com.example.hospitalmanagementapplication.databinding.ActivityDoctordisableappointmentdateBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class DoctorDisableAppointmentActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: ActivityDoctordisableappointmentdateBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    // private val availableDay = arrayOf(Calendar.TUESDAY, Calendar.FRIDAY)
    private val availableDays = mutableListOf<Int>()
    private var appointmentAvailableStartTime = ""
    private var appointmentAvailableEndTime = ""
    private var doctorID=""
    private var userID=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctordisableappointmentdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.home)
        IntentManager(this, bottomNavigationView)


        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser= firebaseAuth.currentUser
        userID=currentUser?.uid?:""

        firestore().getAllDisableAppointment (userID){ DisableAppointment ->
            // Once you have the user data, you can populate it into the ListView
            val disableAppointment = ArrayAdapter(this, android.R.layout.simple_list_item_1, DisableAppointment.map { it.dateAppointment })
            binding.disableAppointmentList.adapter = disableAppointment
        }

        binding.disableAppointmentList.setOnItemClickListener { parent, view, position, documentID ->
            firestore().getAllDisableAppointment (userID){ DisableAppointment ->
                val selectedDocumentId = DisableAppointment[position].documentID
                val selectedDate = DisableAppointment[position].dateAppointment

                showConfirmationDialog(selectedDocumentId?:"", selectedDate?:"")
            }


        }



        firestore().getOtherUserDetails(this, userID?:"") { user ->
            if (user != null) {
                binding.doctorNameTextView.text = "DR " + user.lastname + " " + user.firstname
            } else {
                Toast.makeText(this, "User is null", Toast.LENGTH_SHORT).show()
            }
        }

        firestore().getDoctorAppointmentAvailable(this, userID?:"") { _, appointmentAvailable ->

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
                Log.d("Fail", "Fail")
                Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
            }
        }


        binding.button.setOnClickListener{
            val dates = binding.bookingAppointmentET.text.toString()
            if (dates.isNotEmpty()) {
                // Handle button click for the specific time
                showConfirmationDialog(dates) { confirmed ->
                    if (confirmed) {
                        // Store the selected time in Firestore
                        val currentUser = firebaseAuth.currentUser
                        val userId = currentUser?.uid.toString()

                        firestore().disableAppointment(
                            dates,
                            {
                                refreshActivity()
                            },
                            { errorMessage ->
                                // Handle the error while storing in Firestore
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }else
            {
                Toast.makeText(this,"Please select dates",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeDatePicker() {
        // Initialize date picker
        val currentDate = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog.newInstance(
            this,
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.MONTH, 3)
        // Calculate the maximum date (3 months from now)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 6)
        currentDate.add(Calendar.DAY_OF_MONTH, 1)
        // Set the minimum and maximum date for the date picker
        datePickerDialog.minDate = minDate
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

        firestore().disableAppointment(userID) { disableAppointmentDate ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (disableAppointmentDates in disableAppointmentDate) {
                try {
                    val disableAppointmentDate = disableAppointmentDates.dateAppointment
                    val date = dateFormat.parse(disableAppointmentDate)

                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        disabledDays.add(calendar)
                        Log.e("Success","Success")
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


    private fun refreshActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }

    private fun showConfirmationDialog(dateAppointment:String, callback: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Appointment")
        builder.setMessage("Do you want to book the appointment at $dateAppointment?")

        builder.setPositiveButton("Yes") { _, _ ->
            callback(true)
        }

        builder.setNegativeButton("No") { _, _ ->
            callback(false)
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun showConfirmationDialog(disabelAppointmentDateId: String, selectedDate: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Confirm Deletion")
        alertDialogBuilder.setMessage("Are you sure you want to delete the appointment on $selectedDate?")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, which ->
            firestore().deleteDocument(disabelAppointmentDateId,"disableAppointmentDate",
                onSuccess = {
                    // Create an Intent to restart the current activity
                    val intent = intent
                    finish() // Finish the current activity
                    startActivity(intent) // Start a new instance of the current activity
                },
                onFailure = { e ->
                    Log.w("ERROR", "Error deleting document", e)
                })
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, which ->
            // User canceled, do nothing or handle it accordingly
        }
        alertDialogBuilder.show()
    }

}