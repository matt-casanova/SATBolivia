package com.reto01.flood

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reto01.flood.ui.FloodViewModel
import com.reto01.flood.ui.screens.AlertsScreen
import com.reto01.flood.ui.screens.HomeScreen
import com.reto01.flood.ui.screens.SatelliteScreen
import com.reto01.flood.ui.screens.SettingsScreen
import com.reto01.flood.ui.theme.Indigo
import com.reto01.flood.ui.theme.Reto01Theme
import kotlinx.coroutines.launch

/**
 * Raíz del módulo Inundaciones dentro de la app unificada.
 *
 * Es el cuerpo de `setContent { ... }` de la MainActivity original de
 * reto01-inundaciones, copiado sin cambios de comportamiento: mismo tema
 * (Reto01Theme), mismo Scaffold con NavigationBar Río / Alertas / Satélite /
 * Ajustes, badge de alertas PENDING, Snackbar de mensajes del ViewModel y
 * apertura directa de Alertas cuando llega el intent de la notificación.
 *
 * Diferencias deliberadas respecto a la Activity original (todas de integración):
 *  1. La pestaña usa `rememberSaveable` (antes `remember`) para conservar la
 *     pantalla elegida al cambiar entre Incendios e Inundaciones.
 *  2. La petición "abrir alertas" llega como parámetro en vez de un StateFlow
 *     interno de la Activity.
 *  3. El color de la barra de estado (#667EEA, antes en themes.xml) se aplica
 *     aquí porque la Activity anfitriona es edge-to-edge.
 *  4. El polling NO vive aquí: lo arranca la Activity anfitriona con
 *     [startFloodPolling] para que siga activo (y notifique) aunque el usuario
 *     esté en el módulo Incendios, igual que en la app original.
 */
@Composable
fun FloodRoot(
    vm: FloodViewModel = viewModel(),
    openAlertsRequest: Boolean = false,
    onOpenAlertsHandled: () -> Unit = {},
    systemBarsRefreshKey: Any? = null,
) {
    FloodSystemBarsEffect(systemBarsRefreshKey)

    Reto01Theme {
        val state by vm.state.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(openAlertsRequest) { if (openAlertsRequest) { tab = 1; onOpenAlertsHandled() } }
        LaunchedEffect(state.message) {
            state.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
        }
        val pending = state.alerts.count { it.status == "PENDING" }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Home, null) }, label = { Text("Río") })
                        NavigationBarItem(tab == 1, { tab = 1 }, {
                            BadgedBox(badge = { if (pending > 0) Badge { Text("$pending") } }) {
                                Icon(Icons.Filled.Notifications, null)
                            }
                        }, label = { Text("Alertas") })
                        NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Filled.Star, null) }, label = { Text("Satélite") })
                        NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Filled.Settings, null) }, label = { Text("Ajustes") })
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (tab) {
                        0 -> HomeScreen(state, onAck = vm::acknowledge, onOpenAlerts = { tab = 1 },
                            onOpenSatellite = { tab = 2 })
                        1 -> AlertsScreen(state, onAck = vm::acknowledge)
                        2 -> SatelliteScreen(state)
                        else -> SettingsScreen(state, onSave = vm::saveSettings, onHealth = { vm.checkHealth() })
                    }
                }
            }

            // Equivalente a android:statusBarColor="#667EEA" del tema original (Theme.Reto01):
            // con edge-to-edge el contenido pasa bajo la barra de estado, así que se pinta aquí.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Indigo)
            )
        }
    }
}

/**
 * Aplica el estilo de barra de estado de la app de inundaciones (fondo índigo, iconos
 * claros) al entrar al módulo. Al volver a Incendios, AlertSpotTheme reaplica el suyo.
 *
 * [refreshKey] lo reaplica cuando otra capa cambió la barra mientras Inundaciones estaba
 * visible (la alarma de incendios a pantalla completa usa AlertSpotTheme). Se usa
 * DisposableEffect y no SideEffect: en un mismo frame Compose despacha los
 * DisposableEffect antes que los SideEffect, así que mientras la alarma está en pantalla
 * prevalece su estilo y, al cerrarse, este efecto vuelve a aplicar el de inundaciones.
 */
@Composable
private fun FloodSystemBarsEffect(refreshKey: Any?) {
    val view = LocalView.current
    DisposableEffect(view, refreshKey) {
        val window = (view.context as? Activity)?.window
        if (window != null && !view.isInEditMode) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Indigo.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
        onDispose { }
    }
}

/**
 * Refresco cada 5 s SOLO con la app visible (plan H4–H5), idéntico al de la MainActivity
 * original: `lifecycleScope.launch { repeatOnLifecycle(STARTED) { vm.pollLoop() } }`.
 *
 * Lo invoca la MainActivity anfitriona en onCreate, de modo que el polling (y con él las
 * notificaciones locales de alertas PENDING nuevas) no depende del módulo que esté en
 * pantalla.
 */
fun LifecycleOwner.startFloodPolling(vm: FloodViewModel) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { vm.pollLoop() } }
}
