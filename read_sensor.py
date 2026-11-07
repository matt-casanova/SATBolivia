
import json
import time
import serial

PORT = "COM8"  # Cambia por tu COM real

arduino = serial.Serial(
    PORT,
    115200,
    timeout=2
)

time.sleep(2)

print("SAT BOLIVIA - DOS SENSORES FÍSICOS")

while True:
    raw = arduino.readline().decode(
        "utf-8",
        errors="replace"
    ).strip()

    try:
        data = json.loads(raw)
        flood = data["flood"]
        fire = data["fire"]

        print(
            f"INUNDACIÓN: "
            f"{flood['distance_cm']} cm | "
            f"Nivel: {flood['water_level_cm']} cm"
        )

        print(
            f"INCENDIOS: "
            f"ADC: {fire['temperature_raw']} | "
            f"Temperatura estimada: "
            f"{fire['temperature_c']} °C"
        )

        print("----------------------")

    except (json.JSONDecodeError, KeyError, TypeError):
        if raw:
            print("Lectura no procesable:", raw)
