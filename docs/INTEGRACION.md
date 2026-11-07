# Integración SAT Bolivia — auditoría, pruebas y riesgos

Fuentes: `FireHackaton(1).zip` y `reto01-inundaciones(1).zip` (sin historial Git; se usaron copias de trabajo,
los ZIP originales no se modificaron).

Historial en este repositorio (rama `feature/unified-sat`, sin push ni merge):

| Commit | Contenido |
|---|---|
| `main` · Línea base | FireHackaton tal como se recibió (sin `alert-spot.apk` ni `.kotlin/`) |
| Fase 3a | Código de inundaciones importado **literal** (byte a byte) — este commit aislado no compila |
| Fase 3b | Adaptadores mínimos: FloodRoot, FloodIntents, shim BuildConfig, AlertNotifier |
| Fase 2/4 | UnifiedRoot + barra global, MainActivity, AppNavigation, Gradle, Manifest, ProGuard |
| Docs | README, este documento, diff y scripts de verificación |

`git diff a4db5b6 HEAD -- app/src/main/java/com/reto01/flood` muestra exactamente qué se adaptó del código de inundaciones.

---

## 1. Auditoría (Fase 1)

### Componentes originales verificados

| Proyecto | Componentes (existencia verificada) |
|---|---|
| Incendios | `MainActivity`, `AlertSpotApp` (canales `alarm_channel`, `service_channel`, OSMDroid), `ui/navigation/AppNavigation` (barra iOS Incendios/Mapa/Alertas + rutas heredadas add_location/history/edit_location), `ui/fires/FireScreen`+`FireViewModel`, `FireMapScreen`+`FireMapViewModel`, `ui/alerts/AlertsScreen`+`AlertsViewModel`, `data/remote/ApiClient` (`http://192.168.0.13:8000`), `SatApi` (/health, fires/overview, fires/points, fires/history, alerts, confirm, false-alarm), `data/repository/FireRepository`, `viewmodel/AlertViewModel`, `manager/AlarmHandler` (notif. 1001), `PreferencesManager` (`alert_spot_prefs`), `service/LocationForegroundService` (notif. 2001), `GeofenceBroadcastReceiver`, `AlarmActionReceiver`, `BootReceiver`, `ui/component/*`, `ui/screen/*` (heredadas), `ui/theme/*` |
| Inundaciones | `MainActivity` (FloodViewModel, polling repeatOnLifecycle, Scaffold, NavigationBar 4 pestañas, Snackbar, intent `open_alerts`), `AlertNotifier` (canal `alertas_inundacion`), `data/ApiService`, `FloodRepository` (OkHttp+logging, MOCK), `Models`, `Settings` (`flood_settings`, `BuildConfig.BASE_URL`), `ui/FloodViewModel`, `ui/Format`, `ui/screens/Home/Alerts/Satellite/SettingsScreen`, `ui/components/Components`, `ui/theme/Theme` |

### Plan de migración

| Componente | Acción | Resultado |
|---|---|---|
| Todo `com.alertspot` salvo 2 archivos | Conservar | Idéntico (58 de 61 archivos bajo `app/src`) |
| `AppNavigation.kt` | Parámetro opcional `navController` (por defecto el mismo `rememberNavController()`) | Modificado mínimamente |
| `MainActivity.kt` (anfitrión) | Aloja `UnifiedRoot`; FloodViewModel por `viewModels()`; polling; `onNewIntent` | Modificado |
| `com.reto01.flood/data/*`, `ui/*` | Copiar sin cambios | Idénticos byte a byte (12 archivos) |
| `MainActivity.kt` (inundaciones) | **No copiada**: su `setContent` pasa a `FloodRoot` | Extraída |
| `AlertNotifier.kt` | Destino → `com.alertspot.MainActivity`, acción y tag propios | Modificado (3 líneas) |
| `themes.xml` / `ic_launcher.xml` de inundaciones | No migrados (una sola Activity con tema/ícono del anfitrión); `statusBarColor #667EEA` replicado en `FloodRoot` | — |
| `network_security_config` (main/debug) | Migrado y aplicado a toda la app | HTTP solo en debug |

### Adaptadores creados

| Archivo | Motivo |
|---|---|
| `ui/unified/UnifiedRoot.kt` | Navegación jerárquica, estado por módulo, insets, Atrás, apertura por notificación |
| `ui/unified/GlobalModuleBar.kt` | Barra global INCENDIOS / INUNDACIONES |
| `reto01/flood/FloodRoot.kt` | UI de la MainActivity original como composable + `startFloodPolling` |
| `reto01/flood/FloodIntents.kt` | Contrato del intent de notificación |
| `reto01/flood/BuildConfig.kt` | El namespace ahora es `com.alertspot`; mantiene `Settings.kt` intacto |

### Dependencias añadidas (sin actualizar las del anfitrión)

| Dependencia | Motivo | Efecto |
|---|---|---|
| `androidx.lifecycle:lifecycle-runtime-ktx:2.8.3` | `lifecycleScope`/`repeatOnLifecycle` del polling | Ya llegaba transitivamente; misma versión que el anfitrión |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` | Uso directo en FloodViewModel | Ya llegaba transitivamente |
| `com.squareup.okhttp3:logging-interceptor:4.12.0` | `FloodRepository` | **Sube OkHttp transitivo 3.14.9 → 4.12.0** (combinación ya usada por la app de inundaciones) |
| Compose BOM | Se mantiene **2024.06.00** del anfitrión (inundaciones usaba 2024.09.03) | APIs usadas por inundaciones revisadas: existen en Material3 1.2.1 |
| `material-icons-core` | Cubierto por `material-icons-extended` del anfitrión | — |

### Conflictos detectados y resueltos

| Conflicto | Resolución |
|---|---|
| `Settings.kt` usa `com.reto01.flood.BuildConfig` (ya no generado) | Shim con el mismo nombre; valor desde `FLOOD_BASE_URL` |
| URL por defecto de inundaciones `10.0.2.2` | `FLOOD_BASE_URL` = IP LAN, sobrescribible en `local.properties` |
| PendingIntent de inundaciones (requestCode = id de alerta) chocaba con el de la alarma de incendios (requestCode 0, misma Activity, `FLAG_UPDATE_CURRENT`) | Acción propia `com.reto01.flood.action.OPEN_ALERTS` |
| IDs de notificación de inundaciones (= id de alerta) podían coincidir con 1001/2001 de AlertSpot | `notify(tag, id, …)` con tag propio |
| Canales | Ya distintos: `alarm_channel`, `service_channel`, `alertas_inundacion` |
| SharedPreferences | Ya distintas: `alert_spot_prefs`, `flood_settings` |
| Doble petición de `POST_NOTIFICATIONS` | Se mantiene solo la del anfitrión (mismo permiso) |
| Barra iOS de incendios aplica `navigationBarsPadding()` | El contenedor consume el inset inferior; solo la barra global lo aplica |
| `AlertSpotTheme` fija color/iconos de la barra de estado en cada composición | Ahora envuelve solo al módulo Incendios y a la alarma; Inundaciones aplica su #667EEA |
| `usesCleartextTraffic="true"` global | Sustituido por `networkSecurityConfig` (HTTP solo en debug) |
| `adjustResize` (inundaciones) | Añadido a la Activity + `imePadding()` en la raíz (teclado en Ajustes) |

---

## 2. Matriz de funcionalidades a conservar

| Funcionalidad | Dónde vive ahora | Cómo se conserva |
|---|---|---|
| Incendios: riesgo 0–100, NORMAL/WARNING/CRITICAL, clima, humo, focos, DEMO/API | `FireScreen`/`FireViewModel` | Sin cambios; NavController elevado conserva el ViewModel |
| Mapa OSMDroid, sensores, focos, colores, marcadores | `FireMapScreen`/`OsmMapView`, `AlertSpotApp` | Sin cambios |
| Alertas incendios: PENDING/CONFIRMED/FALSE_ALARM, confirmar, falsa alarma, errores | `AlertsScreen`/`AlertsViewModel`/`FireRepository` | Sin cambios; mismos endpoints `/confirm`, `/false-alarm` |
| Servicios, receivers, permisos, alarma, overlay, geofencing heredado | Manifest, `service/`, `manager/`, `AlertViewModel` | Sin cambios; overlay sigue sobre toda la UI |
| Barra iOS Incendios/Mapa/Alertas, tema, animaciones | `AppNavigation` | Sin cambios visuales |
| Río: nivel, tendencia, velocidad, pronóstico 30 min, riesgo, gráfico, estación, clima, sospechosas | `HomeScreen`/`Components` | Idénticos |
| Alertas inundaciones: PENDING/ACKNOWLEDGED/RESOLVED, ACK, operador, hora, 404/409, badge | `AlertsScreen`/`FloodViewModel` | Idénticos; badge en `FloodRoot` |
| Satélite FIRMS: estado, detecciones, fecha, MOCK/real | `SatelliteScreen` | Idéntico |
| Ajustes: URL, operador, MOCK, /health, persistencia | `SettingsScreen`/`Settings` | Idénticos |
| Polling 5 s solo con app visible | `startFloodPolling` (Activity anfitriona) | Mismo `repeatOnLifecycle(STARTED)`; sigue activo aunque se vea Incendios |
| Notificación local de alerta PENDING nueva | `FloodViewModel.maybeNotify`/`AlertNotifier` | Igual; abre Inundaciones › Alertas |
| Snackbar de mensajes (ACK, ajustes, health) | `FloodRoot` | Igual; si se cambia de módulo, el mensaje se muestra al volver |
| Nunca simular un ACK exitoso | `FloodViewModel.acknowledge` | Sin cambios (NetworkError → "NO se registró") |

---

## 3. Pruebas realizadas

Leyenda: **PASS** ejecutada y correcta · **FAIL** ejecutada con error · **NO EJECUTADA** requiere teléfono,
Gradle/SDK o un backend que no estaba disponible.

### Verificación del proyecto

| # | Prueba | Resultado |
|---|---|---|
| V1 | Compilación Gradle (`assembleDebug`) | **NO EJECUTADA** — sin Android SDK ni red para descargar dependencias |
| V2 | Sintaxis Kotlin (lexer Pygments, balance de `() [] {}`) en 56 archivos | **PASS** — únicos avisos: 7 falsos positivos del lexer con strings anidados (`"${x ?: "—"}"`), idénticos en el código original de inundaciones |
| V3 | Imports internos (`com.alertspot.*`, `com.reto01.flood.*`) resuelven a declaraciones existentes | **PASS** |
| V4 | Sin clases duplicadas por paquete; sin referencias a la MainActivity de inundaciones | **PASS** |
| V5 | Manifest XML válido; 1 sola `<activity>` y 1 LAUNCHER; recursos referenciados existen | **PASS** |
| V6 | `data/` y `ui/` de inundaciones idénticos al ZIP (`cmp`) | **PASS** (12/12) |
| V7 | Código de incendios intacto salvo lo documentado (`cmp`) | **PASS** (58/61 idénticos; cambian MainActivity, AppNavigation, Manifest) |
| V8 | Librerías importadas por archivos nuevos/modificados cubiertas por dependencias declaradas | **PASS** (revisión manual) |

### Backend de inundaciones (Flask original, ejecutado localmente sin red)

| # | Prueba | Resultado |
|---|---|---|
| B1 | Suite original `tests/test_api.py` | **PASS** 26/26 |
| B2 | Rutas Retrofit del proyecto unificado ↔ rutas Flask | **PASS** 6/6 |
| B3 | Contrato JSON: claves y tipos de los DTO Kotlin vs respuestas reales | **PASS** 155 claves — observación preexistente: `reading_id` no viene en `/alerts` (solo lo usa el MOCK; Gson deja null) |
| B4 | Rampa de nivel → alerta PENDING (riesgo 75 CRITICAL, pronóstico 30 min, data_status FRESH) | **PASS** |
| B5 | ACK → 200 con operador y hora | **PASS** |
| B6 | ACK repetido → 409; ACK inexistente → 404 (casos que maneja FloodViewModel) | **PASS** |
| B7 | ACK persiste en SQLite tras reiniciar Flask | **PASS** |
| B8 | FIRMS sin MAP_KEY → `UNAVAILABLE` con HTTP 200 | **PASS** |

### Pruebas obligatorias de Incendios (sección 9)

El backend FastAPI **no viene** en `FireHackaton(1).zip` (solo el proyecto Android), así que no se pudo
levantar. El código de estas funciones no se modificó.

| # | Prueba | Resultado |
|---|---|---|
| 1–7 | Riesgo y score, lectura FastAPI, mapa OSMDroid, sensores, focos, 75→100 con foco DEMO, DEMO/LIVE/STALE | **NO EJECUTADA** (requiere FastAPI + teléfono) |
| 8–11 | PENDING, CONFIRMED, FALSE_ALARM, persistencia | **NO EJECUTADA** (requiere FastAPI + teléfono) |
| 12–13 | Refresco, desconexiones | **NO EJECUTADA** (requiere teléfono) |
| 14 | Servicios y notificaciones | Manifest: servicio, 3 receivers y 12 permisos conservados → **PASS (estático)**; funcionamiento **NO EJECUTADA** |

### Pruebas obligatorias de Inundaciones (sección 10)

| # | Prueba | Backend/contrato | App en teléfono |
|---|---|---|---|
| 1–5 | Nivel, tendencias, predicción, histórico, meteorología | PASS (B3–B4) | NO EJECUTADA |
| 6–7 | Estado de sensores, lecturas sospechosas | PASS (suite B1) | NO EJECUTADA |
| 8 | Satélite y estados FIRMS | PASS (B8) | NO EJECUTADA |
| 9–12 | PENDING, ACK, persistencia, 404/409 | PASS (B4–B7) | NO EJECUTADA |
| 13–14 | Notificaciones y apertura directa desde notificación | — | NO EJECUTADA |
| 15–18 | IP/puerto, operador, MOCK, persistencia de ajustes | — | NO EJECUTADA |
| 19–20 | Reintento tras pérdida de conexión, actualización automática | — | NO EJECUTADA |

---

## 4. Validación manual pendiente en el teléfono

1. **Gradle sync + Run (debug)**. Si falla, anotar el primer error (ver riesgos R1).
2. **Barra global:** alternar varias veces; la barra interna de cada módulo queda justo encima, nada tapado,
   sin hueco extra con navegación por gestos ni con 3 botones; probar también en horizontal.
3. **Estado:** Incendios › Mapa → Inundaciones › Satélite → volver: debe seguir en Mapa, y al regresar a
   Inundaciones seguir en Satélite. El mapa no debe recargar datos más de lo normal.
4. **Atrás:** Inundaciones → Incendios; Mapa/Alertas → Incendios; Incendios → sale de la app.
5. **Incendios (FastAPI encendido):** score, badges DEMO / API REAL, mapa con sensores y focos, confirmar y
   falsa alarma; apagar FastAPI → mensaje de error y "Reintentar".
6. **Inundaciones (Flask encendido + `reset_demo.py --spike`):** banner de sospechosa, gráfico subiendo,
   badge en Alertas, notificación, **Confirmar recepción** → snackbar con operador y hora; repetir → 409;
   apagar Flask → banner "sin conexión" y botón deshabilitado; encender → recupera solo en ≤5 s.
7. **Notificación:** con la app en segundo plano y con la app cerrada, tocarla → Inundaciones › Alertas.
   Con la app viendo Incendios también debe notificar (el polling no depende del módulo visible).
8. **Ajustes de inundaciones:** cambiar URL/operador/MOCK, cerrar la app, reabrir → persisten; el teclado no
   tapa el campo que se edita.
9. **Barra de estado:** índigo con iconos blancos en Inundaciones; la de AlertSpot en Incendios (claro/oscuro).
10. **Alarma de geocerca de AlertSpot** (si se usa): el overlay rojo cubre todo, incluida la barra global.

---

## 5. Riesgos pendientes

| # | Riesgo | Mitigación |
|---|---|---|
| R1 | **No compilado.** Puntos con más probabilidad de ajuste en el primer sync: código de inundaciones escrito para Compose BOM 2024.09.03 corriendo con 2024.06.00 (APIs revisadas: `LinearProgressIndicator(progress = {…}, strokeCap)`, `NavigationBarItem`, `BadgedBox`, `Card(border=…)` existen en Material3 1.2.1); `consumeWindowInsets` (con `@OptIn(ExperimentalLayoutApi)`) | Corregir solo el error puntual; no subir el BOM salvo necesidad (cambiaría la apariencia de incendios) |
| R2 | OkHttp transitivo de Retrofit en Incendios pasa de 3.14.9 a 4.12.0 | Compatible a nivel de API; es la combinación que ya usaba inundaciones. Probar confirmar/falsa alarma |
| R3 | `release`: HTTP bloqueado (antes Incendios lo permitía). Además, los DTO `com.alertspot.data.model` no tienen regla `-keep` (preexistente) y con `isMinifyEnabled = true` Gson podría fallar | Usar `debug` (es el escenario del prototipo) o backends HTTPS + regla keep si se publica |
| R4 | Nuevo comportamiento de Atrás en Inundaciones (antes salía de la app) | Decisión de navegación estándar; fácil de revertir en `UnifiedRoot` |
| R5 | Polling simultáneo con la app visible: 3 ViewModels de incendios cada 15 s + inundaciones cada 5 s | Mismo comportamiento que cada app por separado |
| R6 | URL de incendios fija en `ApiClient.kt` (preexistente, sin ajuste en la app) | Editar antes de compilar |
| R7 | Si la alarma de AlertSpot se cierra estando en Inundaciones, el color de barra de estado se reaplica vía `systemBarsRefreshKey` | Validar visualmente (punto 9 de la lista) |
| R8 | `reading_id` ausente en `/alerts` (preexistente entre app y backend originales) | Sin efecto en la UI |
