package com.example.hospitalmanagementapplication

import android.content.Intent
import android.content.Context
import android.app.job.JobParameters
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.hospitalmanagementapplication.firebase.firestore
import com.google.firebase.firestore.FirebaseFirestore


class AppointmentNotification(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.e("AppointmentNotification", "Worker started")
        firestore().checkAndScheduleAppointments(applicationContext) // Use applicationContext for the context
        return Result.success()
    }
}