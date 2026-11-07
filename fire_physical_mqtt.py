
import json
import time
import serial
import paho.mqtt.client as mqtt

PORT = "COM8"  # Tu puerto real
MQTT_HOST = "10.108.113.243"  # IP del broker
MQTT_PORT = 1883

TOPIC = "sat/forest/I-01-PHYSICAL/raw"

client = mqtt.Client(
    mqtt.CallbackAPIVersion.VERSION2
)
client.connect(MQTT_HOST, MQTT_PORT, 60)
client.loop_start()

arduino = serial.Serial(PORT, 115200, timeout=2)
time.sleep(2)

print("PUBLICANDO TEMPERATURA FISICA EN MQTT")

try:
    while True:
        raw = arduino.readline().decode(
            "utf-8", errors="replace"
        ).strip()

        try:
            data = json.loads(raw)
            fire = data["fire"]

            temperature = fire.get("temperature_c")
            adc = fire.get("temperature_raw")

            if temperature is None:
                continue

            payload = {
                "node_id": "I-01-PHYSICAL",
                "temperature_c": temperature,
                "temperature_raw": adc,
                "source": "ARDUINO_MEGA_NTC",
                "temperature_calibrated": False,
                "simulated": False
            }

            result = client.publish(
                TOPIC,
                json.dumps(payload),
                qos=1
            )

            result.wait_for_publish(timeout=5)

            print(
                f"MQTT: ADC={adc} | "
                f"Temp estimada={temperature} C"
            )

        except (json.JSONDecodeError, KeyError, TypeError):
            continue

finally:
    arduino.close()
    client.loop_stop()
    client.disconnect()
