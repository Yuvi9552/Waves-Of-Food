package com.example.wavesoffood

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // 🔄 Get dynamic title and message from the intent
        val title = intent.getStringExtra("title") ?: "Order Update"
        val message = intent.getStringExtra("message") ?: "You have a new update!"

        val channelId = "waves_food_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔔 Create notification channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Waves Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Waves Of Food app"
            }
            manager.createNotificationChannel(channel)
        }

        // 🔁 Open MainActivity when user taps the notification
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 🚀 Build and show the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(title)      // ⬅️ dynamic title
            .setContentText(message)     // ⬅️ dynamic message
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        // 🆔 Use unique notification ID (based on time)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
