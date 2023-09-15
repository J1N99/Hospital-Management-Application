package com.example.hospitalmanagementapplication
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.util.*

class BookingActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private val availableDays = arrayOf(Calendar.TUESDAY, Calendar.FRIDAY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        val doctorNameTextView = findViewById<TextView>(R.id.doctorNameTextView)
        val bookButton = findViewById<Button>(R.id.bookButton)

        // Set the doctor's name dynamically (replace with actual data)
        doctorNameTextView.text = "Dr. John Doe"

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
        bookButton.setOnClickListener {
            datePickerDialog.show(supportFragmentManager, "DatePickerDialog")
        }

    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        // Handle date selection here
        // You can perform the booking process here
    }
}
