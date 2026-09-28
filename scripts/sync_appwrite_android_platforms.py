#!/usr/bin/env python3
"""List and idempotently register Android platforms for grades 4..12."""
from __future__ import annotations

import argparse
import json
import os

import requests

from sync_appwrite_to_arvan import APPWRITE_ENDPOINT, APPWRITE_PROJECT, appwrite_headers, load_local_env

PLATFORMS = [
    ("grade4", "همیار من - پایه چهارم", "com.hamyareman.p04"),
    ("grade5", "همیار من - پایه پنجم", "com.hamyareman.p05"),
    ("grade6", "همیار من - پایه ششم", "com.hamyareman.p06"),
    ("grade7", "همیار من - پایه هفتم", "com.hamyareman.p07"),
    ("grade8", "همیار من - پایه هشتم", "com.hamyareman.p08"),
    ("grade9", "همیار من - پایه نهم", "com.hamyareman.ir"),
    ("grade10", "همیار من - پایه دهم", "com.hamyareman.p10"),
    ("grade11", "همیار من - پایه یازدهم", "com.hamyareman.p11"),
    ("grade12", "همیار من - پایه دوازدهم", "com.hamyareman.p12"),
]


def call(method: str, path: str, **kwargs):
    endpoint = os.getenv("APPWRITE_ENDPOINT", APPWRITE_ENDPOINT).rstrip("/")
    response = requests.request(
        method,
        endpoint + path,
        headers={**appwrite_headers(), "Content-Type": "application/json"},
        timeout=(20, 60),
        **kwargs,
    )
    if response.status_code not in range(200, 300):
        raise RuntimeError(f"HTTP {response.status_code}: {response.text[:400]}")
    return response.json()


def main() -> int:
    load_local_env()
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    current = call("GET", "/project/platforms", params={"total": "true"})
    rows = current.get("platforms") or []
    android = [row for row in rows if row.get("type") == "android"]
    by_app = {row.get("applicationId"): row for row in android}
    missing = [entry for entry in PLATFORMS if entry[2] not in by_app]
    print(json.dumps({
        "mode": "apply" if args.apply else "plan",
        "registeredAndroidApplicationIds": sorted(x for x in by_app if x),
        "missingAndroidApplicationIds": [entry[2] for entry in missing],
        "otherPlatformCount": len(rows) - len(android),
    }, ensure_ascii=False, indent=2))
    if not args.apply:
        return 0

    for grade_id, name, application_id in missing:
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

    after = call("GET", "/project/platforms", params={"total": "true"})
    final_apps = {
        row.get("applicationId")
        for row in after.get("platforms") or []
        if row.get("type") == "android"
    }
    expected = {entry[2] for entry in PLATFORMS}
    if not expected.issubset(final_apps):
        raise RuntimeError("platform registration verification failed")
    print("verified", len(expected), "grade Android platforms")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
