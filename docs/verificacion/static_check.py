"""Verificación estática del proyecto unificado (sustituto parcial de la compilación)."""
import os, re, sys, collections
from pygments.lexers import KotlinLexer
from pygments.token import Error, String, Comment, Punctuation, Operator
import xml.etree.ElementTree as ET

ROOT = sys.argv[1]
SRC = os.path.join(ROOT, "app/src/main/java")
problems = []
kt_files = []
for d, _, fs in os.walk(SRC):
    for f in fs:
        if f.endswith(".kt"):
            kt_files.append(os.path.join(d, f))

lexer = KotlinLexer()
decls = collections.defaultdict(set)   # package -> top-level names
file_pkg = {}
for p in kt_files:
    s = open(p, encoding="utf-8").read().replace("\r\n", "\n")
    # 1) tokens de error y balance de () [] {} fuera de strings/comentarios
    stack = []
    pairs = {")": "(", "]": "[", "}": "{"}
    line = 1
    for tok, val in lexer.get_tokens(s):
        if tok in Error:
            problems.append(f"{p}:{line}: token de error {val!r}")
        if tok not in String and tok not in Comment:
            for ch in val:
                if ch in "([{": stack.append((ch, line))
                elif ch in ")]}":
                    if not stack or stack[-1][0] != pairs[ch]:
                        problems.append(f"{p}:{line}: cierre '{ch}' sin apertura"); break
                    stack.pop()
        line += val.count("\n")
    if stack:
        problems.append(f"{p}: aperturas sin cerrar {stack[:3]}")
    # 2) paquete y declaraciones de primer nivel
    m = re.search(r"^package\s+([\w.]+)", s, re.M)
    pkg = m.group(1) if m else ""
    file_pkg[p] = pkg
    rel = os.path.relpath(os.path.dirname(p), SRC).replace(os.sep, ".")
    if rel != pkg:
        problems.append(f"{p}: package '{pkg}' no coincide con la carpeta '{rel}'")
    for m in re.finditer(r"^(?:@\w+(?:\([^)]*\))?\s*)*(?:(?:private|internal|public|data|sealed|enum|abstract|open|inline|const|annotation)\s+)*"
                         r"(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)", s, re.M):
        decls[pkg].add(m.group(1))

# 3) imports internos resolubles
GENERATED = {"com.alertspot": {"BuildConfig", "R"}}
for p in kt_files:
    s = open(p, encoding="utf-8").read()
    for m in re.finditer(r"^import\s+((?:com\.alertspot|com\.reto01)[\w.]*)\.(\w+|\*)\s*$", s, re.M):
        pkg, name = m.group(1), m.group(2)
        if name == "*":
            if pkg not in decls: problems.append(f"{p}: import {pkg}.* — paquete inexistente")
            continue
        ok = name in decls.get(pkg, set()) or name in GENERATED.get(pkg, set())
        # import de miembro anidado (p.ej. Clase.Companion) no aplica aquí
        if not ok:
            problems.append(f"{p}: import sin resolver {pkg}.{name}")

# 4) declaraciones públicas duplicadas en un mismo paquete (choque de clases)
seen = collections.defaultdict(list)
for p in kt_files:
    s = open(p, encoding="utf-8").read()
    for m in re.finditer(r"^(?:public\s+)?(?:data\s+|sealed\s+|enum\s+|abstract\s+|open\s+)*(class|object|interface)\s+(\w+)", s, re.M):
        seen[(file_pkg[p], m.group(2))].append(os.path.relpath(p, SRC))
for k, v in seen.items():
    if len(v) > 1: problems.append(f"Clase duplicada {k}: {v}")

# 5) referencias residuales a la Activity original de inundaciones
for p in kt_files:
    s = open(p, encoding="utf-8").read()
    if re.search(r"com\.reto01\.flood\.MainActivity|MainActivity\.EXTRA_OPEN_ALERTS", s):
        problems.append(f"{p}: referencia residual a la MainActivity de inundaciones")

# 6) manifest y recursos referenciados
man = os.path.join(ROOT, "app/src/main/AndroidManifest.xml")
ET.parse(man)
mtxt = open(man, encoding="utf-8").read()
res_root = os.path.join(ROOT, "app/src/main/res")
def res_exists(kind, name):
    for d in os.listdir(res_root):
        if d == kind or d.startswith(kind + "-"):
            for f in os.listdir(os.path.join(res_root, d)):
                if os.path.splitext(f)[0] == name: return True
            if kind == "values" : pass
    return False
for kind, name in re.findall(r"@(\w+)/([\w.]+)", mtxt):
    if kind in ("string", "style"):
        vals = "".join(open(os.path.join(res_root, "values", f), encoding="utf-8").read() for f in os.listdir(os.path.join(res_root, "values")))
        if f'name="{name}"' not in vals: problems.append(f"Manifest: @{kind}/{name} no existe")
    elif not res_exists(kind, name):
        problems.append(f"Manifest: @{kind}/{name} no existe")
for x in ["app/src/main/res/xml/network_security_config.xml", "app/src/debug/res/xml/network_security_config.xml"]:
    ET.parse(os.path.join(ROOT, x))
launchers = mtxt.count("android.intent.category.LAUNCHER")
activities = len(re.findall(r"<activity\b", mtxt))
print(f"Archivos Kotlin: {len(kt_files)} | paquetes: {len(decls)} | <activity>: {activities} | LAUNCHER: {launchers}")
if launchers != 1: problems.append("Debe haber exactamente un LAUNCHER")
print("\n".join(problems) if problems else "SIN PROBLEMAS DETECTADOS")
sys.exit(1 if problems else 0)
