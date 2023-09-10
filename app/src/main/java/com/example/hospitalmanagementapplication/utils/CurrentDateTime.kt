package com.example.hospitalmanagementapplication.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CurrentDateTime {
    fun getCurrentDateTimeFormatted(): String {
        // Get the current date and time
        val currentDateTime = LocalDateTime.now()

        // Define a date and time format
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Format the current date and time as a string
        return currentDateTime.format(formatter)
    }
}