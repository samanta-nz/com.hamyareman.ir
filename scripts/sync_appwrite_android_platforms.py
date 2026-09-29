#!/usr/bin/env python3
"""Register grade Android platforms and isolated update-channel rows.

Each grade has a distinct Android applicationId and a distinct Appwrite row:
`app_release_grade4` … `app_release_grade12`.  Existing rows are never
rewritten here; publishing is performed by publish-update.yml with a strictly
increasing versionCode guard.
"""
from __future__ import annotations

import argparse
import json
import os
import time
from typing import Any

import requests

APPWRITE_ENDPOINT = "https://sgp.cloud.appwrite.io/v1"


def load_local_env() -> None:
    return


def appwrite_headers() -> dict[str, str]:
    key = os.getenv("APPWRITE_API_KEY", "").strip()
    if not key:
        raise RuntimeError("missing APPWRITE_API_KEY")
    return {
        "X-Appwrite-Project": os.getenv("APPWRITE_PROJECT_ID", "6abb134a002025222005"),
        "X-Appwrite-Key": key,
    }


PLATFORMS = [
    ("grade4", "همیار من - پایه چهارم", "com.hamyareman.p04"),
    ("grade5", "همیار من - پایه پنجم", "com.hamyareman.p05"),
    ("grade6", "همیار من - پایه ششم", "com.hamyareman.p06"),
    ("grade7", "همیار من - پایه هفتم", "com.hamyareman.p07"),
    ("grade8", "همیار من - پایه هشتم", "com.hamyareman.p08"),
    ("grade9", "همیار من - پایه نهم", "com.hamyareman.p09"),
    ("grade10", "همیار من - پایه دهم", "com.hamyareman.p10"),
    ("grade11", "همیار من - پایه یازدهم", "com.hamyareman.p11"),
    ("grade12", "همیار من - پایه دوازدهم", "com.hamyareman.p12"),
]
DATABASE_ID = os.getenv("APPWRITE_DATABASE_ID", "6abb238d000d05730d10")
COLLECTION_ID = "app_state"


def raw_call(method: str, path: str, **kwargs: Any) -> requests.Response:
    endpoint = os.getenv("APPWRITE_ENDPOINT", APPWRITE_ENDPOINT).rstrip("/")
    return requests.request(
        method,
        endpoint + path,
        headers={**appwrite_headers(), "Content-Type": "application/json"},
        timeout=(20, 60),
        **kwargs,
    )


def call(method: str, path: str, **kwargs: Any) -> dict[str, Any]:
    response = raw_call(method, path, **kwargs)
    if response.status_code not in range(200, 300):
        raise RuntimeError(f"HTTP {response.status_code}: {response.text[:400]}")
    return response.json()


def release_path(row_id: str = "") -> str:
    base = f"/tablesdb/{DATABASE_ID}/tables/{COLLECTION_ID}/rows"
    return base + (f"/{row_id}" if row_id else "")


def existing_release_rows() -> set[str]:
    found: set[str] = set()
    for grade_id, _, _ in PLATFORMS:
        row_id = f"app_release_{grade_id}"
        response = raw_call("GET", release_path(row_id))
        if response.status_code in range(200, 300):
            found.add(row_id)
        elif response.status_code != 404:
            raise RuntimeError(f"release row probe failed for {row_id}: HTTP {response.status_code}")
    return found


def create_release_row(grade_id: str, package_name: str) -> None:
    row_id = f"app_release_{grade_id}"
    # A safe disabled channel. It cannot trigger a download until the publishing
    # workflow atomically writes URL/hash/version/signing identity.
    payload = {
        "latest": 0,
        "name": "",
        "min": 0,
        "url": "",
        "externalUrl": "",
        "internalUrl": "",
        "size": 0,
        "sha256": "",
        "chan": "stable",
        "rollout": 100,
        "notes": [],
        "packageName": package_name,
        "gradeId": grade_id,
        "signingSha256": "",
    }
    call(
        "POST",
        release_path(),
        json={
            "rowId": row_id,
            "data": {
                "userId": "global",
                "key": row_id,
                "payload": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
                "updatedAt": int(time.time() * 1000),
            },
            "permissions": ['read("users")'],
        },
    )


def main() -> int:
    load_local_env()
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    current = call("GET", "/project/platforms", params={"total": "true"})
    rows = current.get("platforms") or []
    android = [row for row in rows if row.get("type") == "android"]
    by_app = {row.get("applicationId"): row for row in android}
    missing_platforms = [entry for entry in PLATFORMS if entry[2] not in by_app]
    existing_rows = existing_release_rows()
    missing_rows = [
        (grade_id, application_id)
        for grade_id, _, application_id in PLATFORMS
        if f"app_release_{grade_id}" not in existing_rows
    ]
    print(json.dumps({
        "mode": "apply" if args.apply else "plan",
        "registeredAndroidApplicationIds": sorted(x for x in by_app if x),
        "missingAndroidApplicationIds": [entry[2] for entry in missing_platforms],
        "existingGradeUpdateRows": sorted(existing_rows),
        "missingGradeUpdateRows": [f"app_release_{grade_id}" for grade_id, _ in missing_rows],
        "otherPlatformCount": len(rows) - len(android),
    }, ensure_ascii=False, indent=2))
    if not args.apply:
        return 0

    for grade_id, name, application_id in missing_platforms:
        call(
            "POST",
            "/project/platforms/android",
            json={
                "platformId": f"android-{grade_id}",
                "name": name,
                "applicationId": application_id,
            },
        )
        print("registered", application_id)

    for grade_id, application_id in missing_rows:
        create_release_row(grade_id, application_id)
        print("created", f"app_release_{grade_id}")

    after = call("GET", "/project/platforms", params={"total": "true"})
    final_apps = {
        row.get("applicationId")
        for row in after.get("platforms") or []
        if row.get("type") == "android"
    }
    expected_apps = {entry[2] for entry in PLATFORMS}
    if not expected_apps.issubset(final_apps):
        raise RuntimeError("platform registration verification failed")
    final_rows = existing_release_rows()
    expected_rows = {f"app_release_{entry[0]}" for entry in PLATFORMS}
    if not expected_rows.issubset(final_rows):
        raise RuntimeError("grade update-row verification failed")
    print("verified", len(expected_apps), "grade platforms and", len(expected_rows), "update rows")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
