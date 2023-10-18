package com.example.hospitalmanagementapplication

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hospitalmanagementapplication.databinding.ActivityBookingBinding
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.Quota.Resource
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import android.util.Base64
import androidx.core.net.toUri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

class BookingActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: ActivityBookingBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressDialog: Loader

    // private val availableDay = arrayOf(Calendar.TUESDAY, Calendar.FRIDAY)
    private val availableDays = mutableListOf<Int>()
    private var appointmentAvailableStartTime = ""
    private var appointmentAvailableEndTime = ""
    private var doctorID = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
            }
        }

        doctorID = intent.getStringExtra("doctorID") ?: ""

        Log.e(doctorID, doctorID)
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
                Toast.makeText(this, "Doctor did not open for appointment yet", Toast.LENGTH_LONG)
                    .show()
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

            firestore().getAppointment(
                doctorID,
                dateAppointment,
                formattedTime
            ) { appointmentExists ->
                if (appointmentExists) {
                    button.isEnabled = false
                    button.setTextColor(Color.parseColor("#808080"))
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
                        val userEmail = currentUser?.email
                        firestore().makeAppointment(
                            doctorID,
                            userId,
                            dateAppointment,
                            selectedTime,
                            {

                                progressDialog = Loader(this@BookingActivity)
                                progressDialog.show()

                                sendEmailInBackground(
                                    userEmail ?: "",
                                    doctorID,
                                    dateAppointment,
                                    selectedTime
                                )


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


    private fun showConfirmationDialog(
        dateAppointment: String,
        time: String,
        callback: (Boolean) -> Unit
    ) {
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


    private fun destroyAllButtons() {
        binding.LayoutButton.removeAllViews() // This will remove all child views (buttons) from the LinearLayout
    }

    private fun sendEmailInBackground(
        to: String,
        doctorID: String,
        dateAppointment: String,
        selectedTime: String
    ) {
        var doctorName = ""
        var hospital = ""
        var doctorHospitalID = ""
        var doctorEmail=""
        firestore().getOtherUserDetails(this, doctorID) { user ->
            if (user != null) {
                doctorName = user.lastname + " " + user.firstname
                doctorEmail=user.email
                Log.e("Doctor Name", doctorName)
            }
        }
        firestore().getDoctorInfo(this@BookingActivity, doctorID ?: "") { doctorInfo ->
            if (doctorInfo != null) {
                doctorHospitalID = doctorInfo.hospital
                firestore().getHospitalDetails(
                    this@BookingActivity,
                    doctorHospitalID
                ) { hospitals ->
                    if (hospitals != null) {
                        hospital = hospitals.hospital


                        // Use Kotlin Coroutine to perform this task in a background thread
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val username = "angwj99@gmail.com"
                                val password = "whrpltsbuwfgirzn"

                                val props = Properties()
                                props["mail.smtp.host"] = "smtp.gmail.com"
                                props["mail.smtp.port"] = "465"
                                props["mail.smtp.auth"] = "true"
                                props["mail.smtp.starttls.enable"] = "true"
                                props["mail.smtp.ssl.enable"] = "true"


                                val session = Session.getInstance(props, object : Authenticator() {
                                    override fun getPasswordAuthentication(): PasswordAuthentication {
                                        return PasswordAuthentication(username, password)
                                    }
                                })

                                val message = MimeMessage(session)
                                message.setFrom(InternetAddress(username))
                                message.addRecipient(Message.RecipientType.TO, InternetAddress(to))
                                message.addRecipient(Message.RecipientType.CC, InternetAddress(doctorEmail)) // cc doctor
                                message.subject = "Your Upcoming Appointment with Dr.$doctorName"









                                Log.e("Image String", hospital)


                                val htmlContent = """
<html>
  <head>
    <style>
      /* Add some basic styling to make the email visually appealing */
      body {
          font-family: Arial, sans-serif;
          background-color: #f4f4f4;
          margin: 0;
          padding: 0;
      }
      .container {
          max-width: 600px;
          margin: 0 auto;
          padding: 20px;
          background-color: #f4f4f4;
      }
      h1 {
          color: #333;
      }
      p {
          color: #555;
      }
      .center{
        text-align: center;
      }
    </style>
  </head>
  <body>
    <div class="container">
      <h1 class="center">Your Upcoming Appointment</h1>
      <p>Hello,</p>
      <p>Your appointment with Dr. $doctorName is confirmed.</p>
      <p><strong>Appointment Details:</strong></p>
      <ul>
        <li><strong>Hospital:</strong> $hospital</li>
        <li><strong>Date:</strong> $dateAppointment</li>
        <li><strong>Time:</strong> $selectedTime</li>
      </ul>
      <p>We look forward to seeing you!</p>
      <p>Best regards,<br />Vantist Team</p>
    </div>
  </body>
</html>

"""
                                message.setContent(htmlContent, "text/html; charset=utf-8")
                                Transport.send(message)
                                Log.e("Doctor Name", doctorName)
                                // Notify the UI thread that the email has been sent
                                withContext(Dispatchers.Main) {
                                    progressDialog.dismiss()

                                }

                                Log.e("Success", "Email sent successfully.")

                                val intent = Intent(
                                    this@BookingActivity,
                                    ViewAppointmentActivity::class.java
                                )
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) {
                                Log.e("Fail", "Error sending email: ${e.message}")
                                Log.e("To", "Error sending email: $to")

                                // Notify the UI thread of the error
                                withContext(Dispatchers.Main) {
                                    progressDialog.dismiss()
                                    Toast.makeText(
                                        this@BookingActivity,
                                        "Error sending email",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

