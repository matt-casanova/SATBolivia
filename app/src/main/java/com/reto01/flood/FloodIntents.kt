package com.reto01.flood

import android.content.Intent

/**
 * ADAPTADOR DE INTEGRACIÓN: contrato entre la notificación de inundaciones y la
 * MainActivity anfitriona (com.alertspot.MainActivity).
 *
 * - [EXTRA_OPEN_ALERTS] conserva la misma clave que usaba la MainActivity original.
 * - [ACTION_OPEN_ALERTS] hace que el PendingIntent de inundaciones sea distinto del
 *   PendingIntent de la alarma de incendios (AlarmHandler usa requestCode 0 sobre la
 *   misma Activity con FLAG_UPDATE_CURRENT); sin una acción propia, una alerta con id 0
 *   sobrescribiría los extras de la notificación de incendios o viceversa.
 * - [NOTIFICATION_TAG] separa los IDs de notificación de inundaciones (= id de alerta)
 *   de los IDs fijos de incendios (1001 alarma, 2001 servicio en primer plano).
 */
object FloodIntents {
    const val EXTRA_OPEN_ALERTS = "open_alerts"
    const val ACTION_OPEN_ALERTS = "com.reto01.flood.action.OPEN_ALERTS"
    const val NOTIFICATION_TAG = "com.reto01.flood.alert"

    /** true si el intent pide abrir Inundaciones › Alertas. */
    fun isOpenAlertsIntent(intent: Intent?): Boolean =
        intent?.getBooleanExtra(EXTRA_OPEN_ALERTS, false) == true
}
