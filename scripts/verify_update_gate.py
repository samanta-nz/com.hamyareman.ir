#!/usr/bin/env python3
from pathlib import Path

main = Path("apps/hamyar-app/src/main/java/com/hamyareman/ir/MainActivity.kt").read_text(encoding="utf-8")
gate = Path("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/update/UpdateGate.kt").read_text(encoding="utf-8")

host = "com.hamyareman.ir.ui.update.UpdateGateHost()"
if main.count(host) != 1:
    raise SystemExit(f"expected exactly one UpdateGateHost(), found {main.count(host)}")

if "\n                            " + host in main:
    raise SystemExit("UpdateGateHost() is still nested inside the isUnlocked dashboard branch")

if "\n                        " + host not in main:
    raise SystemExit("UpdateGateHost() is not mounted at Surface root")

if "ZahraNavHost()" not in main:
    raise SystemExit("dashboard host missing")

if "forceNetwork = true" not in gate:
    raise SystemExit("first-open update check must bypass the local TTL cache")

if "UpdateUi.session == null" not in gate:
    raise SystemExit("update dialog must be shown only once per active composition")

print("update gate root/immediate contract: PASS")
