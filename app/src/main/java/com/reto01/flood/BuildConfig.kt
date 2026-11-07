package com.reto01.flood

/**
 * ADAPTADOR DE INTEGRACIÓN (app unificada SAT Bolivia).
 *
 * En la app original de inundaciones, Gradle generaba `com.reto01.flood.BuildConfig`
 * con el campo `BASE_URL`. En la app unificada el namespace es `com.alertspot`, por lo
 * que Gradle genera `com.alertspot.BuildConfig`.
 *
 * Este objeto conserva el mismo nombre y campo que usa `data/Settings.kt`, de modo que
 * ese archivo se mantiene byte a byte idéntico al original. El valor se define en
 * `app/build.gradle.kts` (`FLOOD_BASE_URL`, sobrescribible desde local.properties) y solo
 * es el valor inicial: el usuario lo cambia y persiste desde Inundaciones › Ajustes.
 */
object BuildConfig {
    val BASE_URL: String = com.alertspot.BuildConfig.FLOOD_BASE_URL
}
