
import json
import time
import math
import statistics
from collections import deque
from datetime import datetime, timezone

import serial
import requests
import paho.mqtt.client as mqtt

# ==========================================
# SAT BOLIVIA - PUENTE FISICO UNIFICADO
# ==========================================

# ARDUINO
SERIAL_PORT = "COM8"  # Cambiar si es necesario
BAUD_RATE = 115200


# Servidor Flask de Saraí
FLASK_HOST = "10.108.113.193"

FLOOD_API = (
    f"http://{FLASK_HOST}:5000/api/v1/telemetry"
)

# Servidor MQTT que ya está funcionando
MQTT_HOST = "10.108.113.243"

STATION_ID = "RIO-01"
MOUNT_HEIGHT_CM = 250.0

# INCENDIOS - MQTT

MQTT_PORT = 1883
MQTT_TOPIC = "sat/forest/I-01-PHYSICAL/raw"

# CONFIGURACION DE MUESTREO
SEND_INTERVAL = 5.0
WINDOW_SIZE = 5

# Historial corto para suavizar distancia
distances = deque(maxlen=WINDOW_SIZE)

# ==========================================
# CLIENTE MQTT
# ==========================================

mqtt_client = mqtt.Client(
    mqtt.CallbackAPIVersion.VERSION2,
    client_id="sat-bolivia-physical-bridge"
)

mqtt_connected = False


def on_connect(client, userdata, flags,
               reason_code, properties):
    global mqtt_connected

    mqtt_connected = (reason_code == 0)

    if mqtt_connected:
        print("[MQTT] Conectado correctamente")
    else:
        print("[MQTT] Conexion rechazada:", reason_code)


def on_disconnect(client, userdata,
                  disconnect_flags,
                  reason_code, properties):
    global mqtt_connected
    mqtt_connected = False
    print("[MQTT] Desconectado:", reason_code)


mqtt_client.on_connect = on_connect
mqtt_client.on_disconnect = on_disconnect

mqtt_client.reconnect_delay_set(
    min_delay=1,
    max_delay=10
)

try:
    mqtt_client.connect_async(
        MQTT_HOST,
        MQTT_PORT,
        keepalive=60
    )
    mqtt_client.loop_start()
except Exception as error:
    print("[MQTT] Error inicial:", error)

# ==========================================
# FUNCIONES
# ==========================================

def utc_now():
    return (
        datetime.now(timezone.utc)
        .isoformat(timespec="milliseconds")
        .replace("+00:00", "Z")
    )


def read_number(value):
    """Acepta solo numeros finitos reales."""
    try:
        if value is None or isinstance(value, bool):
            return None

        number = float(value)

        if not math.isfinite(number):
            return None

        return number

    except (ValueError, TypeError):
        return None


def send_flood(flood):
    """Envia distancia real a Flask."""

    distance = read_number(
        flood.get("distance_cm")
    )

    if distance is None:
        print("[FLOOD] Sin medicion valida")
        return

    # Rango fisico de la maqueta
    if not (2 <= distance <= MOUNT_HEIGHT_CM):
        print(
            "[FLOOD] Distancia fuera de rango:",
            distance
        )
        return

    distances.append(distance)

    # Mediana de las ultimas mediciones
    filtered = statistics.median(distances)

    payload = {
        "station_id": STATION_ID,
        "timestamp": utc_now(),
        "distance_cm": round(filtered, 1),
        "mount_height_cm": MOUNT_HEIGHT_CM,
        "source_mode": "LIVE"
    }

    try:
        response = requests.post(
            FLOOD_API,
            json=payload,
            timeout=3
        )

        if response.status_code in (200, 201):
            print(
                "[FLOOD OK] "
                f"Distancia={filtered:.1f} cm | "
                f"Nivel={MOUNT_HEIGHT_CM-filtered:.1f} cm"
            )
        else:
            print(
                "[FLOOD ERROR]",
                response.status_code,
                response.text[:250]
            )

    except requests.RequestException as error:
        print("[FLOOD] Sin conexion:", error)


def send_fire(fire):
    """Publica la temperatura fisica por MQTT."""

    adc = read_number(
        fire.get("temperature_raw")
    )

    temperature = read_number(
        fire.get("temperature_c")
    )

    if adc is None:
        print("[FIRE] ADC invalido")
        return

    # Temperatura aun no calibrada
    payload = {
        "node_id": "I-01-PHYSICAL",
        "temperature_raw": int(adc),
        "temperature_c": temperature,
        "source": "ARDUINO_MEGA_NTC",
        "temperature_calibrated": False,
        "simulated": False,
        "timestamp": utc_now()
    }

    if not mqtt_connected:
        print(
            "[FIRE] MQTT desconectado. "
            "Lectura no enviada."
        )
        return

    info = mqtt_client.publish(
        MQTT_TOPIC,
        json.dumps(payload),
        qos=1
    )

    if info.rc == mqtt.MQTT_ERR_SUCCESS:
        print(
            "[FIRE MQTT] "
            f"ADC={int(adc)} | "
            f"Temp estimada={temperature} C"
        )
    else:
        print("[FIRE] Error publicando:", info.rc)


# ==========================================
# LECTURA UNICA DEL ARDUINO
# ==========================================

print("=" * 45)
print("SAT BOLIVIA - DOS SENSORES FISICOS")
print("=" * 45)
print("Arduino:", SERIAL_PORT)
print("Flask:", FLOOD_API)
print("MQTT:", MQTT_HOST, MQTT_PORT)
print("Topic:", MQTT_TOPIC)
print("=" * 45)

arduino = None

try:
    arduino = serial.Serial(
        SERIAL_PORT,
        BAUD_RATE,
        timeout=2
    )

    # El Arduino Mega puede reiniciarse
    # al abrir el puerto serial.
    time.sleep(2)

    last_send = 0.0
    latest_flood = None
    latest_fire = None

    while True:
        raw = arduino.readline().decode(
            "utf-8",
            errors="replace"
        ).strip()

        if not raw:
            continue

        try:
            data = json.loads(raw)

            flood = data.get("flood")
            fire = data.get("fire")

            if not isinstance(flood, dict):
                continue

            if not isinstance(fire, dict):
                continue

            latest_flood = flood
            latest_fire = fire

        except json.JSONDecodeError:
            print("[SERIAL] JSON no valido:", raw)
            continue

        # Publicar aproximadamente cada 5 segundos
        now = time.monotonic()

        if now - last_send < SEND_INTERVAL:
            continue

        last_send = now

        print("\n--- NUEVAS LECTURAS FISICAS ---")

        # Los errores de un destino no deben
        # impedir intentar enviar al otro.
        try:
            send_flood(latest_flood)
        except Exception as error:
            print("[FLOOD] Error inesperado:", error)

        try:
            send_fire(latest_fire)
        except Exception as error:
            print("[FIRE] Error inesperado:", error)

except KeyboardInterrupt:
    print("\nPuente detenido por el usuario")

except serial.SerialException as error:
    print("[SERIAL] Error de puerto:", error)

finally:
    if arduino is not None and arduino.is_open:
        arduino.close()

    mqtt_client.disconnect()
    mqtt_client.loop_stop()

    print("Conexiones cerradas")
