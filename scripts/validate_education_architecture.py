#!/usr/bin/env python3
"""Fail-closed validation for the 3-stage / 9-app education architecture."""
from __future__ import annotations
import json, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
catalog=json.loads((ROOT/"config/education-catalog.json").read_text())
gradle=(ROOT/"apps/hamyar-app/build.gradle.kts").read_text()

def fail(msg): raise SystemExit("education architecture validation failed: "+msg)

apps=catalog["apps"]
if len(apps)!=9: fail(f"expected 9 apps, got {len(apps)}")
stages=catalog["stages"]
if len(stages)!=3: fail(f"expected 3 stages, got {len(stages)}")
if sorted(g for s in stages for g in s["grades"]) != list(range(4,13)): fail("stage grades must be exactly 4..12")
ids=set()
for app in apps:
    grade=app["grade"]; p=f"p{grade:02d}"; aid=app["applicationId"]
    if p in ids: fail(f"duplicate flavor {p}")
    ids.add(p)
    if app["gradeId"] != f"grade{grade}": fail(f"bad gradeId for {p}")
    if aid != f"com.hamyareman.p{grade:02d}": fail(f"bad applicationId for {p}")
    if app["stageId"] not in {s["id"] for s in stages}: fail(f"unknown stage for {p}")
    if app["contentProfileId"] != app["gradeId"]: fail(f"content profile mismatch for {p}")
    expected_stage="stage1" if grade<=6 else "stage2" if grade<=9 else "stage3"
    if app["stageId"] != expected_stage: fail(f"{p} mapped to wrong stage")

for app in apps:
    p=f'p{app["grade"]:02d}'
    pattern=(rf'gradeApp\("{p}",\s*{app["grade"]},\s*"grade{app["grade"]}".*?'
             rf'"com\.hamyareman\.p{app["grade"]:02d}",\s*"Base-{app["grade"]:02d}",\s*'
             rf'"{app["stageId"]}",\s*"{re.escape(next(s["titleFa"] for s in stages if s["id"]==app["stageId"]))}",\s*'
             rf'"grade{app["grade"]}"')
    if not re.search(pattern,gradle,re.S): fail(f"Gradle flavor metadata mismatch for {p}")

for token in ['EDUCATION_STAGE_ID','EDUCATION_STAGE_FA','CONTENT_PROFILE_ID']:
    if token not in gradle: fail(f"missing BuildConfig field {token}")

print(json.dumps({
    "schemaVersion":catalog["schemaVersion"],
    "stages":len(stages),
    "apps":len(apps),
    "gradeGroups":{"stage1":[4,5,6],"stage2":[7,8,9],"stage3":[10,11,12]},
    "status":"PASS"
},ensure_ascii=False,indent=2))
