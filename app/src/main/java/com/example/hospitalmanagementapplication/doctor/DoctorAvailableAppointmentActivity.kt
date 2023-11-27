package com.example.hospitalmanagementapplication.doctor

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.R
import com.example.hospitalmanagementapplication.databinding.ActivityDoctoravailableappointmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.model.AppointmentAvailable
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class DoctorAvailableAppointmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctoravailableappointmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    var shirtstart = false
    var shirtend = false

    //to record the available day
    var monday = false
    var tuesday = false
    var wednesday = false
    var thursday = false
    var friday = false
    var saturday = false
    var sunday = false
    var documentIDs = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctoravailableappointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        var userID = ""


        binding.shiftET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val timePattern = Regex("^([01]?[0-9]|2[0-3])[0-5][0-9]$")

                if (!timePattern.matches(inputText)) {
                    binding.shiftET.error = "Invalid time format"
                } else {
                    binding.shiftET.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for validation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed for validation
            }
        })


        binding.EndshiftET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val timePattern = Regex("^([01]?[0-9]|2[0-3])[0-5][0-9]$")

                if (!timePattern.matches(inputText)) {
                    binding.EndshiftET.error = "Invalid time format"
                } else {
                    binding.EndshiftET.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for validation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed for validation
            }
        })

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
            }
        }


        if (currentUser != null) {
            userID = currentUser.uid
        } else {
            println("No user is currently signed in.")
        }

        firestore().getAppointmentAvailable(this) { documentID, appointmentAvailable ->
            if (appointmentAvailable != null) {
                documentIDs = documentID ?: "DefaultDocumentId"
                binding.button.setText("Save Edit")
                binding.shiftET.setText(appointmentAvailable.appointmentStartTime)
                binding.EndshiftET.setText(appointmentAvailable.appointmentEndTime)
                binding.checkMonday.isChecked = appointmentAvailable.monday
                binding.checkTuesday.isChecked = appointmentAvailable.tuesday
                binding.checkWednesday.isChecked = appointmentAvailable.wednesday
                binding.checkThursday.isChecked = appointmentAvailable.thursday
                binding.checkFriday.isChecked = appointmentAvailable.friday
                binding.checkSaturday.isChecked = appointmentAvailable.saturday
                binding.checkSunday.isChecked = appointmentAvailable.sunday
            } else {
                Log.d("Fail", "Fail")
                Toast.makeText(this, "Fail", Toast.LENGTH_SHORT)
            }
        }



        binding.button.setOnClickListener {

            val time = binding.shiftET.text.toString()
            val timeEnd = binding.EndshiftET.text.toString()
            val isValidFormat = validateTimeFormat(time)
            if (time.length < 4 && timeEnd.length < 4) {
                Toast.makeText(
                    this,
                    "Please enter valid Hours format. For example 0900",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!isValidFormat) {
                Toast.makeText(this, "Please enter valid Hours format", Toast.LENGTH_SHORT).show()
            } else if (timeEnd.toInt() < time.toInt()) {
                Toast.makeText(
                    this,
                    "The end time cannot larger than start time",
                    Toast.LENGTH_SHORT
                ).show()
            } else if ((timeEnd.toInt() - time.toInt()) < 100) {
                Toast.makeText(
                    this,
                    "The appointment must available 1 hour",
                    Toast.LENGTH_SHORT
                ).show()
            } else {

                val daysOfWeek = listOf(
                    binding.checkMonday,
                    binding.checkTuesday,
                    binding.checkWednesday,
                    binding.checkThursday,
                    binding.checkFriday,
                    binding.checkSaturday,
                    binding.checkSunday
                )

                val days = BooleanArray(7)

                for ((index, checkBox) in daysOfWeek.withIndex()) {
                    days[index] = checkBox.isChecked
                }

                var appoinmentStart = binding.shiftET.text.toString()
                var appoinmentEnd = binding.EndshiftET.text.toString()
                monday = days[0]
                tuesday = days[1]
                wednesday = days[2]
                thursday = days[3]
                friday = days[4]
                saturday = days[5]
                sunday = days[6]
                if (!days[0] && !days[1] && !days[2] && !days[3] && !days[4] && !days[5] && !days[6]) {
                    Toast.makeText(
                        this,
                        "Please select at least one day for appointment",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {


                    val appointmentAvailable = AppointmentAvailable(
                        userID,
                        appoinmentStart,
                        appoinmentEnd,
                        monday,
                        tuesday,
                        wednesday,
                        thursday,
                        friday,
                        saturday,
                        sunday
                    )
                    if (binding.button.text == "Submit") {
                        firestore().createAvailableAppointment(this, appointmentAvailable)
                        showDialogSuccessUpdate()
                    } else if (binding.button.text == "Save Edit") {
                        val dataToUpdate = mapOf(
                            "appointmentStartTime" to appoinmentStart,
                            "appointmentEndTime" to appoinmentEnd,
                            "monday" to monday,
                            "tuesday" to tuesday,
                            "wednesday" to wednesday,
                            "thursday" to thursday,
                            "friday" to friday,
                            "saturday" to saturday,
                            "sunday" to sunday
                        )

                        firestore().updateDocument(
                            "appointmentAvailable",
                            documentIDs,
                            dataToUpdate
                        )
                        showDialogSuccessUpdate()
                    }
                }
            }
        }
        binding.shiftET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called before text is changed. You can leave it empty if not needed.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                shirtstart = s?.length ?: 0 >= 4
            }

            override fun afterTextChanged(s: Editable?) {
                val hours = runExpectedEndHours(
                    binding.shiftET.text.toString(),
                    binding.EndshiftET.text.toString()
                )
                binding.totalhours.text = "Total Hours: $hours"
            }
        })

        binding.EndshiftET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called before text is changed. You can leave it empty if not needed.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                shirtend = s?.length == 1
            }

            override fun afterTextChanged(s: Editable?) {
                val hours = runExpectedEndHours(
                    binding.shiftET.text.toString(),
                    binding.EndshiftET.text.toString()
                )
                binding.totalhours.text = "Total Hours: $hours"
            }
        })


    }


    fun runExpectedEndHours(shirtHours: String, EndshiftET: String): Float {
        if (shirtHours.length >= 4 && EndshiftET.length >= 4) {
            val startHour = shirtHours.substring(0, 2).toFloat()
            val startMinute = shirtHours.substring(2).toFloat()
            val endHour = EndshiftET.substring(0, 2).toFloat()
            val endMinute = EndshiftET.substring(2).toFloat()

            val totalStartMinutes = startHour * 60 + startMinute
            val totalEndMinutes = endHour * 60 + endMinute

            val differenceInMinutes = totalEndMinutes - totalStartMinutes

            // Convert the difference back to hours
            val differenceInHours = differenceInMinutes / 60

            return differenceInHours
        } else {
            return -1.0f
        }
    }

    fun validateTimeFormat(hours: String): Boolean {
        val regex = Regex("^([01]\\d|2[0-3])[0-5]\\d\$")
        return regex.matches(hours)
    }

    private fun showDialogSuccessUpdate() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = "The information have updated!"
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        val buttonEmail = dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility = View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, DoctorHomeActivity::class.java)
            startActivity(intent)
        }

        // Add a listener to handle the user's action when dismissing the dialog
        dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            // You can take further action here if needed
            Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show()
        })

        dialog.show()
    }
}