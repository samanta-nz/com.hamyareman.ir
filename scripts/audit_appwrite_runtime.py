#!/usr/bin/env python3
"""Read-only Appwrite runtime/schema audit for Hamyar Man.

No user rows are listed and no data is modified. The API key is read only from
APPWRITE_API_KEY in the CI environment.
"""
import json
import os
import sys
import urllib.error
import urllib.request

EP = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
PROJECT = os.environ.get("APPWRITE_PROJECT_ID", "6abb134a002025222005")
DB = os.environ.get("APPWRITE_DATABASE_ID", "6abb238d000d05730d10")
KEY = os.environ.get("APPWRITE_API_KEY", "").strip()

if not KEY:
    raise SystemExit("APPWRITE_API_KEY is missing")

BASE_HEADERS = {
    "X-Appwrite-Project": PROJECT,
    "X-Appwrite-Key": KEY,
    "Accept": "application/json",
}

def get(path):
    req = urllib.request.Request(EP + path, headers=BASE_HEADERS, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.load(resp)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        try:
            body = json.loads(body)
        except Exception:
            pass
        return e.code, body
    except Exception as e:
        return 0, str(e)

fail = 0

def check(name, path, expected_keys=()):
    global fail
    status, data = get(path)
    print(f"[{name}] HTTP {status}")
    if status != 200:
        print(json.dumps(data, ensure_ascii=False)[:1200])
        fail += 1
        return None
    if expected_keys:
        cols = {x.get("key") for x in data.get("columns", []) if isinstance(x, dict)}
        missing = sorted(set(expected_keys) - cols)
        print("columns:", ", ".join(sorted(cols)))
        if missing:
            print("MISSING COLUMNS:", ", ".join(missing))
            fail += 1
    return data

# Public health endpoint is useful to distinguish endpoint outage from auth/schema errors.
health_req = urllib.request.Request(EP + "/health", method="GET")
try:
    with urllib.request.urlopen(health_req, timeout=15) as resp:
        print(f"[health] HTTP {resp.status}")
        if resp.status != 200:
            fail += 1
except Exception as e:
    print(f"[health] FAILED: {e}")
    fail += 1

check("database", f"/tablesdb/{DB}")
check("app_state", f"/tablesdb/{DB}/tables/app_state", ("userId","key","payload","updatedAt"))
check("lesson_notes", f"/tablesdb/{DB}/tables/lesson_notes", ("userId","payload","text","updatedAt"))
check("student_profiles", f"/tablesdb/{DB}/tables/student_profiles", ("userId","email","firstName","lastName","grade","phone"))

status, bucket = get("/storage/buckets/avatars")
print(f"[avatars bucket] HTTP {status}")
if status != 200:
    print(json.dumps(bucket, ensure_ascii=False)[:1200])
    fail += 1
else:
    print("bucketId:", bucket.get("$id"))
    print("fileSecurity:", bucket.get("fileSecurity"))
    print("enabled:", bucket.get("enabled"))
    print("maximumFileSize:", bucket.get("maximumFileSize"))
    if bucket.get("$id") != "avatars":
        fail += 1

print(f"RESULT={'PASS' if fail == 0 else 'FAIL'}")
sys.exit(1 if fail else 0)
