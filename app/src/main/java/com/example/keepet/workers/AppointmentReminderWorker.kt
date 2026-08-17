package com.example.keepet.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.keepet.util.NotificationHelper

class AppointmentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val petName = inputData.getString("petName") ?: "Tu mascota"
        val serviceType = inputData.getString("serviceType") ?: "Cita"
        val appointmentTime = inputData.getString("appointmentTime") ?: ""
        val notificationId = inputData.getInt("notificationId", 0)

        NotificationHelper.showNotification(
            applicationContext,
            "Recordatorio de KeePet",
            "Recuerda que $petName tiene una cita de $serviceType a las $appointmentTime.",
            notificationId
        )

        return Result.success()
    }
}
