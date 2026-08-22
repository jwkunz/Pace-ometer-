package com.example.pace_ometer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.pace_ometer.MainActivity
import com.example.pace_ometer.R

object RunNotificationFactory {
    const val CHANNEL_ID = "run_tracking"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Run tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing run tracking status"
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        phase: RunPhase,
        distanceLabel: String,
        durationLabel: String
    ): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeAction = if (phase == RunPhase.RUNNING) {
            NotificationCompat.Action(
                0,
                "Pause",
                servicePendingIntent(context, RunTrackingService.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                0,
                "Resume",
                servicePendingIntent(context, RunTrackingService.ACTION_RESUME)
            )
        }

        val stopAction = NotificationCompat.Action(
            0,
            "Stop",
            servicePendingIntent(context, RunTrackingService.ACTION_STOP)
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle("Pace-ometer — $distanceLabel")
            .setContentText(durationLabel)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun servicePendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, RunTrackingService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
