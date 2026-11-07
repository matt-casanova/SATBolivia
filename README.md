# SAT Bolivia — App Android unificada (Incendios + Inundaciones)

IEEE ComSoc STLC 2026 · Hackathon

Una sola app Android que integra, sin reescribirlas, las dos apps que ya funcionaban por separado:

| Módulo | Origen | Paquete | Backend |
|---|---|---|---|
| **INCENDIOS** (anfitrión) | `FireHackaton(1).zip → Hackaton/` | `com.alertspot` | FastAPI + SQLite, puerto **8000** |
| **INUNDACIONES** | `reto01-inundaciones(1).zip → android/` | `com.reto01.flood` | Flask + SQLite, puerto **5000** |

`applicationId` = `com.alertspot` (el del anfitrión). Una sola `MainActivity` launcher.

> ⚠️ **Estado de compilación:** este proyecto **no se compiló con Gradle** en el entorno donde se integró
> (no había Android SDK ni acceso a red para descargar dependencias). Se hizo verificación estática y pruebas de
> contrato contra el backend Flask real. Ver `docs/INTEGRACION.md` para la tabla PASS / FAIL / NO EJECUTADA.
> La primera sincronización de Gradle en Android Studio es el primer paso de validación.

---

## Navegación

```
┌──────────────────────────────────────────┐
│  Pantalla ORIGINAL del módulo activo     │
│  (sus tarjetas, colores, top bars, etc.) │
├──────────────────────────────────────────┤
│  Nivel 2: barra ORIGINAL del módulo      │  Incendios: barra iOS  Incendios · Mapa · Alertas
│                                          │  Inundaciones: NavigationBar  Río · Alertas · Satélite · Ajustes
├──────────────────────────────────────────┤
│  Nivel 1: barra GLOBAL                   │  🔥 INCENDIOS │ 💧 INUNDACIONES
└──────────────────────────────────────────┘
```

- La barra global es la única que aplica el inset de la barra de navegación del sistema; el contenedor de
  módulos lo marca como consumido, así que no hay doble padding ni barras superpuestas.
- Al cambiar de módulo se conserva la pantalla elegida y el estado de cada uno (pila de navegación y
  ViewModels de Incendios; pestaña y ViewModel de Inundaciones).
- **Atrás:** en Incendios funciona como antes (Mapa/Alertas → Incendios → salir). En Inundaciones vuelve al
  módulo Incendios.
- **Notificación de inundaciones:** abre Inundaciones › Alertas (con la app cerrada o abierta).
- La alarma a pantalla completa de AlertSpot sigue apareciendo encima de todo.

---

## Configurar las IP de los backends (teléfono físico)

El teléfono y las PC de los backends deben estar en la **misma red Wi‑Fi**. No uses `10.0.2.2` (solo sirve
en el emulador). Los backends pueden estar en PC distintas.

**Incendios (FastAPI :8000)** — igual que en el proyecto original:
`app/src/main/java/com/alertspot/data/remote/ApiClient.kt`
```kotlin
var BASE_URL = "http://192.168.0.13:8000"   // ← IP LAN de la PC que corre FastAPI
```

**Inundaciones (Flask :5000)** — igual que en la app original, desde la propia app:
**Inundaciones › Ajustes › URL base** → `http://IP_DE_LA_PC:5000/` → **Guardar** → **Probar /health**.
Se guarda en preferencias (`flood_settings`) y persiste.

El valor *inicial* (primera ejecución) es `http://192.168.0.13:5000/`. Para cambiarlo sin tocar código, añade
a `local.properties`:
```properties
FLOOD_BASE_URL=http://192.168.1.50:5000/
```
(Si la app ya estaba instalada, manda lo guardado en Ajustes; cámbialo ahí o borra los datos de la app.)

---

## Ejecutar

1. **Backends** (cada uno en su propio repositorio, sin cambios):
   - Inundaciones: `cd reto01-inundaciones/backend && python app.py` (escucha en `0.0.0.0:5000`).
     Demo: `python reset_demo.py` (o `--spike` para incluir una lectura sospechosa).
   - Incendios: arranca FastAPI escuchando en todas las interfaces, por ejemplo
     `uvicorn <modulo>:app --host 0.0.0.0 --port 8000` (ajusta `<modulo>` al de vuestro backend).
   - En Windows, permite los puertos 8000 y 5000 en el firewall para redes privadas.
   - Comprueba desde el navegador del teléfono: `http://IP:8000/health` y `http://IP:5000/health`.
2. **Android Studio:** abre la carpeta `SAT_Bolivia_Unificado_Android/` (la misma versión de Android Studio
   con la que ya compilabais FireHackaton: AGP 9.0.1, Gradle 9.1.0, JDK 17+). Deja que sincronice Gradle;
   `local.properties` (sdk.dir) se crea solo.
3. Conecta el teléfono con depuración USB y pulsa **Run ▶** (variante `debug`).
4. Acepta permisos de ubicación y notificaciones (los pide la Activity anfitriona como antes).

> HTTP sin cifrar está permitido **solo en debug** (`app/src/debug/res/xml/network_security_config.xml`).
> En `release` solo se permite HTTPS (`app/src/main/res/xml/network_security_config.xml`).

---

## Estructura

```
app/src/main/java/
├── com/alertspot/                       ← app de INCENDIOS (anfitrión, casi sin cambios)
│   ├── MainActivity.kt                  ← (modificada) aloja UnifiedRoot, polling e intents de inundaciones
│   ├── AlertSpotApp.kt, manager/, service/, model/, viewmodel/   (sin cambios)
│   ├── data/ (ApiClient, SatApi, FireRepository, DTOs)           (sin cambios)
│   └── ui/
│       ├── navigation/AppNavigation.kt  ← (modificada) NavController opcional
│       ├── fires/, alerts/, component/, screen/, theme/          (sin cambios)
│       └── unified/                     ← NUEVO: UnifiedRoot.kt, GlobalModuleBar.kt
└── com/reto01/flood/                    ← app de INUNDACIONES
    ├── data/  ui/                       (idénticos byte a byte al ZIP original)
    ├── AlertNotifier.kt                 ← (modificado) abre la MainActivity anfitriona
    ├── FloodRoot.kt                     ← NUEVO: cuerpo de la MainActivity original como composable
    ├── FloodIntents.kt                  ← NUEVO: extra/acción/tag de notificación
    └── BuildConfig.kt                   ← NUEVO: shim para que data/Settings.kt quede intacto
```

Cada módulo conserva su propio cliente Retrofit, repositorio, DTOs, URL, manejo de errores y base de datos.
No se comparte ni se unifica ningún DTO de alertas.

## Documentación

- `docs/INTEGRACION.md` — auditoría (fase 1), matriz de funcionalidades, pruebas PASS/FAIL/NO EJECUTADA,
  validación manual pendiente en el teléfono y riesgos.
- `docs/integracion.diff` — diff exacto de los archivos originales tocados.
- `docs/verificacion/` — scripts reproducibles (verificación estática y contrato DTO ↔ Flask).
- `README-AlertSpot.md` — README original del proyecto anfitrión.
- Historial Git incluido: rama `main` = línea base de FireHackaton; rama `feature/unified-sat` = integración
  por fases. No se hizo push ni merge.

Excluidos del ZIP (siguen en los ZIP originales): `alert-spot.apk`, `.kotlin/`, `.gradle/`, `build/`, `.idea/`,
`local.properties`, el backend y su `.venv`.
