package com.alertspot.ui.unified

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.alertspot.ui.navigation.AppNavigation
import com.alertspot.ui.theme.AlertSpotTheme
import com.alertspot.viewmodel.AlertViewModel
import com.reto01.flood.FloodRoot
import com.reto01.flood.ui.FloodViewModel

/** Módulos de nivel 1 de la app unificada. */
enum class SatModule(val label: String) {
    FIRES("INCENDIOS"),
    FLOODS("INUNDACIONES"),
}

/**
 * Raíz de la app unificada SAT Bolivia.
 *
 * Navegación jerárquica:
 *  - Nivel 1 (borde inferior): [GlobalModuleBar] INCENDIOS | INUNDACIONES.
 *  - Nivel 2 (justo encima): la barra ORIGINAL del módulo activo, sin cambios:
 *      Incendios   → AppNavigation (barra iOS Incendios / Mapa / Alertas, AlertSpotTheme)
 *      Inundaciones→ FloodRoot (NavigationBar Río / Alertas / Satélite / Ajustes, Reto01Theme)
 *
 * Conservación de estado al cambiar de módulo:
 *  - El NavController de Incendios se crea aquí (fuera del módulo) y se pasa a
 *    AppNavigation: su pila y los ViewModels de cada pantalla (FireViewModel,
 *    FireMapViewModel, AlertsViewModel) sobreviven mientras el usuario está en Inundaciones.
 *  - FloodViewModel y AlertViewModel tienen alcance de Activity (no se recrean).
 *  - El estado `rememberSaveable` de cada módulo (pestaña elegida, etc.) se guarda con un
 *    SaveableStateHolder por módulo.
 *
 * Insets: solo [GlobalModuleBar] aplica el inset inferior de la barra de navegación del
 * sistema. El contenedor de módulos lo marca como consumido, así la barra iOS de Incendios
 * (navigationBarsPadding) y la NavigationBar de Inundaciones no lo vuelven a sumar.
 * El inset superior (barra de estado) lo sigue gestionando cada módulo como antes.
 * imePadding + adjustResize reproducen el comportamiento de teclado de la app de
 * inundaciones (Ajustes: URL/operador).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedRoot(
    alertViewModel: AlertViewModel,
    isFireDarkMode: Boolean,
    floodViewModel: FloodViewModel,
    openFloodAlertsRequest: Boolean,
    onFloodAlertsRequestHandled: () -> Unit,
    alarmVisible: Boolean,
) {
    var selected by rememberSaveable {
        mutableStateOf(if (openFloodAlertsRequest) SatModule.FLOODS else SatModule.FIRES)
    }
    val moduleStates = rememberSaveableStateHolder()
    val fireNavController = rememberNavController()

    // Notificación de inundaciones → módulo Inundaciones (FloodRoot abre la pestaña Alertas
    // y marca la petición como atendida).
    LaunchedEffect(openFloodAlertsRequest) {
        if (openFloodAlertsRequest) selected = SatModule.FLOODS
    }

    // Atrás en Inundaciones vuelve al módulo inicial (Incendios). En Incendios no se
    // intercepta: manda la navegación original de AppNavigation y, al final, salir.
    BackHandler(enabled = selected == SatModule.FLOODS) { selected = SatModule.FIRES }

    Column(Modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            moduleStates.SaveableStateProvider(selected.name) {
                when (selected) {
                    SatModule.FIRES -> AlertSpotTheme(darkTheme = isFireDarkMode) {
                        AppNavigation(alertViewModel, navController = fireNavController)
                    }
                    SatModule.FLOODS -> FloodRoot(
                        vm = floodViewModel,
                        openAlertsRequest = openFloodAlertsRequest,
                        onOpenAlertsHandled = onFloodAlertsRequestHandled,
                        systemBarsRefreshKey = alarmVisible,
                    )
                }
            }
        }

        GlobalModuleBar(
            selected = selected,
            onSelect = { selected = it },
            darkTheme = selected == SatModule.FIRES && isFireDarkMode,
        )
    }
}
