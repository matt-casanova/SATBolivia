package com.reto01.flood.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
private val dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

fun parseInstant(iso: String?): Instant? = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

/** Hora local del teléfono a partir de ISO-8601 UTC del backend. */
fun localTime(iso: String?): String = parseInstant(iso)?.let { timeFmt.format(it) } ?: "—"
fun localDateTime(iso: String?): String = parseInstant(iso)?.let { dateTimeFmt.format(it) } ?: "—"
fun localTime(i: Instant?): String = i?.let { timeFmt.format(it) } ?: "—"

fun ago(iso: String?): String {
    val t = parseInstant(iso) ?: return "—"
    val s = Duration.between(t, Instant.now()).seconds
    return when {
        s < 0 -> "ahora"
        s < 60 -> "hace ${s}s"
        s < 3600 -> "hace ${s / 60} min"
        s < 86400 -> "hace ${s / 3600} h"
        else -> "hace ${s / 86400} d"
    }
}

fun cm(v: Double?): String = v?.let { "%.0f cm".format(it) } ?: "—"
fun signedRate(v: Double?): String = v?.let { (if (it > 0) "+" else "") + "%.1f cm/min".format(it) } ?: "—"
fun mm(v: Double?): String = v?.let { "%.1f mm".format(it) } ?: "no disponible"

fun dataStatusLabel(s: String?): String = when (s) {
    "FRESH" -> "Datos al día"
    "STALE" -> "Datos desactualizados"
    "INSUFFICIENT_HISTORY" -> "Historial insuficiente (<3 lecturas)"
    "SUSPECT_READING" -> "Lectura sospechosa descartada"
    "NO_DATA" -> "Sin lecturas todavía"
    else -> s ?: "—"
}

fun alertStatusLabel(s: String?): String = when (s) {
    "PENDING" -> "PENDIENTE"
    "ACKNOWLEDGED" -> "CONFIRMADA"
    "RESOLVED" -> "RESUELTA"
    else -> s ?: "—"
}
