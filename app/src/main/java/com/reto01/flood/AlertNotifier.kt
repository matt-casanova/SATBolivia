package com.reto01.flood

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.alertspot.MainActivity
import com.reto01.flood.data.AlertDto

/**
 * Notificación local cuando aparece una alerta PENDING nueva (mientras la app
 * consulta el backend). Push en segundo plano requeriría FCM: fuera de alcance.
 */
class AlertNotifier(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "alertas_inundacion"
    }

    init {
        val channel = NotificationChannel(CHANNEL_ID, "Alertas de inundación",
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alertas críticas de la estación hidrométrica"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyAlert(alert: AlertDto, stationName: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        // App unificada: abre la MainActivity anfitriona en Inundaciones › Alertas.
        val intent = Intent(context, MainActivity::class.java).apply {
            action = FloodIntents.ACTION_OPEN_ALERTS
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(FloodIntents.EXTRA_OPEN_ALERTS, true)
        }
        val pi = PendingIntent.getActivity(context, alert.id ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 Alerta ${alert.riskLevel ?: ""} · $stationName")
            .setContentText("Nivel ${alert.levelCm?.let { "%.0f cm".format(it) } ?: "—"} · " +
                "toque para confirmar recepción (#${alert.id})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(FloodIntents.NOTIFICATION_TAG, alert.id ?: 0, n)
        } catch (_: SecurityException) { /* permiso revocado */ }
    }
}
