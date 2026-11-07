"""Contrato Android (DTO Kotlin del proyecto unificado) ↔ Flask (backend de inundaciones, sin cambios).

Uso (desde la raíz del proyecto Android, con las dependencias del backend instaladas):
    python docs/verificacion/flood_contract.py RUTA/reto01-inundaciones/backend .
No usa red: Open-Meteo se simula y FIRMS queda sin MAP_KEY (UNAVAILABLE).
"""
import os, re, sys, json
BACKEND = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else "../reto01-inundaciones/backend"
ANDROID = os.path.abspath(sys.argv[2]) if len(sys.argv) > 2 else "."
from datetime import datetime, timedelta, timezone
sys.path.insert(0, BACKEND)
import config
from services import firms, weather
config.FIRMS_MAP_KEY = ""; config.TELEGRAM_BOT_TOKEN = ""
weather.clear_cache(); firms.clear_cache()
weather.rain_forecast_3h = lambda lat, lon: {"rain_forecast_3h_mm": 4.2, "rain_source": "OPEN_METEO", "fetched_at": None}
import app as app_module

U = os.path.join(ANDROID, "app/src/main/java/com/reto01/flood/data/")
models = open(U + "Models.kt", encoding="utf-8").read()
api = open(U + "ApiService.kt", encoding="utf-8").read()

# 1) DTOs Kotlin → {clave JSON: tipo Kotlin}
dtos = {}
for m in re.finditer(r"data class (\w+)\((.*?)\n\)", models, re.S):
    fields = {}
    for fm in re.finditer(r'(?:@SerializedName\("([^"]+)"\)\s*)?val (\w+): ([\w<>?, ]+?)(?: =|,|$)', m.group(2), re.M):
        fields[fm.group(1) or fm.group(2)] = fm.group(3).strip()
    dtos[m.group(1)] = fields

problems, checked = [], 0
def check(dto, obj, path):
    global checked
    for key, ktype in dtos[dto].items():
        checked += 1
        if key not in obj:
            problems.append(f"{path}: falta clave '{key}' ({dto}.{ktype})"); continue
        v = obj[key]
        if v is None: continue
        base = ktype.rstrip("?")
        if base == "Int" and not (isinstance(v, int) or (isinstance(v, float) and v.is_integer())):
            problems.append(f"{path}.{key}: Int esperado, llegó {v!r}")
        elif base == "Double" and not isinstance(v, (int, float)):
            problems.append(f"{path}.{key}: Double esperado, llegó {v!r}")
        elif base == "String" and not isinstance(v, str):
            problems.append(f"{path}.{key}: String esperado, llegó {v!r}")
        elif base.startswith("List<"):
            inner = base[5:-1]
            if not isinstance(v, list): problems.append(f"{path}.{key}: lista esperada")
            elif inner in dtos:
                for i, it in enumerate(v): check(inner, it, f"{path}.{key}[{i}]")
        elif base in dtos:
            check(base, v, f"{path}.{key}")

# 2) Rutas Retrofit del proyecto unificado ↔ url_map de Flask
import tempfile
db = os.path.join(tempfile.mkdtemp(), "contract.db")
flask_app = app_module.create_app(db); flask_app.testing = True
rules = {(r.rule, m) for r in flask_app.url_map.iter_rules() for m in r.methods}
for verb, path in re.findall(r'@(GET|POST)\("([^"]+)"\)', api):
    flask_path = "/" + re.sub(r"\{(\w+)\}", lambda mm: "<int:alert_id>" if mm.group(1) == "alertId" else ("<station_id>" if "station" in path else f"<{mm.group(1)}>"), path)
    ok = any(verb == m and re.sub(r"<[^>]+>", "<>", rule) == re.sub(r"<[^>]+>", "<>", flask_path) for rule, m in rules)
    print(f"  ruta {verb:4} /{path:38} → {'OK' if ok else 'NO EXISTE EN FLASK'}")
    if not ok: problems.append(f"ruta {verb} /{path} no existe en Flask")

# 3) Escenario: subida del río hasta generar alerta PENDING
c = flask_app.test_client()
def ts(mago): return (datetime.now(timezone.utc) - timedelta(minutes=mago)).strftime("%Y-%m-%dT%H:%M:%SZ")
levels = [120, 130, 145, 160, 175, 190, 200, 210]
for i, lvl in enumerate(levels):
    r = c.post("/api/v1/telemetry", json={"station_id": "RIO-01", "timestamp": ts(20 - i), "distance_cm": 250.0 - lvl,
              "mount_height_cm": 250.0, "lat": -14.88, "lon": -66.75, "source_mode": "DEMO"})
    assert r.status_code == 201, r.get_json()

h = c.get("/health"); check("HealthResponse", h.get_json(), "/health")
st = c.get("/api/v1/stations/RIO-01").get_json(); check("FloodStationDto", st, "/stations/RIO-01")
hi = c.get("/api/v1/stations/RIO-01/history?limit=30").get_json(); check("HistoryResponse", hi, "/history")
al = c.get("/api/v1/alerts?status=ALL").get_json(); check("AlertsResponse", al, "/alerts")
fr = c.get("/api/v1/satellite/firms?station_id=RIO-01&days=2"); check("FirmsDto", fr.get_json(), "/satellite/firms")
pending = [a for a in al["items"] if a["status"] == "PENDING"]
print(f"  estación: nivel={st['level_cm']} riesgo={st['risk_score']}/{st['risk_level']} pronóstico30m={st['forecast_level_30m_cm']} data_status={st['data_status']}")
print(f"  historial: {hi['count']} lecturas · alertas: {len(al['items'])} (PENDING={len(pending)}) · FIRMS status={fr.get_json()['status']} (HTTP {fr.status_code})")
assert pending, "no se generó alerta PENDING"
aid = pending[0]["id"]
r1 = c.post(f"/api/v1/alerts/{aid}/ack", json={"operator": "equipo-android"})
print(f"  ACK #{aid} → HTTP {r1.status_code} {r1.get_json()}")
if r1.status_code == 200: check("AckResponse", r1.get_json(), "/ack")
else: problems.append("ACK válido no devolvió 200")
r2 = c.post(f"/api/v1/alerts/{aid}/ack", json={"operator": "equipo-android"})
r3 = c.post("/api/v1/alerts/99999/ack", json={"operator": "equipo-android"})
print(f"  ACK repetido → HTTP {r2.status_code} · ACK inexistente → HTTP {r3.status_code}")
if r2.status_code != 409: problems.append(f"ACK repetido devolvió {r2.status_code}, FloodViewModel espera 409")
if r3.status_code != 404: problems.append(f"ACK inexistente devolvió {r3.status_code}, FloodViewModel espera 404")
al2 = c.get("/api/v1/alerts?status=ALL").get_json()
a2 = next(a for a in al2["items"] if a["id"] == aid)
print(f"  persistencia: alerta #{aid} status={a2['status']} acknowledged_by={a2['acknowledged_by']} acknowledged_at={a2['acknowledged_at']}")
if a2["status"] != "ACKNOWLEDGED" or a2["acknowledged_by"] != "equipo-android": problems.append("ACK no persistido")
# reinicio del servidor (misma BD) → confirmación persiste
c2 = app_module.create_app(db).test_client()
a3 = next(a for a in c2.get("/api/v1/alerts?status=ALL").get_json()["items"] if a["id"] == aid)
print(f"  tras reiniciar Flask: alerta #{aid} status={a3['status']}")
if a3["status"] != "ACKNOWLEDGED": problems.append("ACK no persiste tras reinicio")
print(f"\nClaves de DTO verificadas: {checked}")
print("CONTRATO OK" if not problems else "PROBLEMAS:\n" + "\n".join(problems))
# Nota: 'reading_id' falta en /alerts desde los proyectos originales (solo lo usa el modo MOCK).
