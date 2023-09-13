package com.example.hospitalmanagementapplication.model

data class AppointmentAvailable(
    val userId: String = "",
    val appointmentStartTime: String = "",
    val appointmentEndTime: String = "",
    val monday: Boolean = true,
    val tuesday: Boolean = true,
    val wednesday: Boolean = true,
    val thursday: Boolean = true,
    val friday: Boolean = true,
    val saturday: Boolean = true,
    val sunday: Boolean = true
)
