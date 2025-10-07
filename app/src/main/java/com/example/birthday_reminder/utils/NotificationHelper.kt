package com.example.birthday_reminder.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.birthday_reminder.MainActivity
import com.example.birthday_reminder.R

object NotificationHelper {

    fun showBirthdayNotification(context: Context, name: String, message: String) {
        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cake) // pastikan kamu punya icon ini
            .setContentTitle("🎉 Selamat Ulang Tahun, $name!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
