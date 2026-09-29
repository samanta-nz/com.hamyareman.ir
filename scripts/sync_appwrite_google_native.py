#!/usr/bin/env python3
"""Configure Appwrite native Google sign-in with the canonical Web audience."""
from __future__ import annotations

import argparse
import json
import os
from typing import Any

import requests

ENDPOINT = os.getenv("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
PROJECT_ID = os.getenv("APPWRITE_PROJECT_ID", "6abb134a002025222005")
WEB_CLIENT_ID = os.getenv(
    "GOOGLE_WEB_CLIENT_ID",
    "548109780863-84ub4jlu08a436mfi0hmn252kge7gv61.apps.googleusercontent.com",
).strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    api_key = os.getenv("APPWRITE_API_KEY", "").strip()
    if not api_key:
        raise SystemExit("APPWRITE_API_KEY is required")
    if not WEB_CLIENT_ID.endswith(".apps.googleusercontent.com"):
        raise SystemExit("invalid Google Web client ID")

    url = f"{ENDPOINT}/project/oauth2/google"
    headers = {
        "X-Appwrite-Project": PROJECT_ID,
        "X-Appwrite-Key": api_key,
        "Content-Type": "application/json",
    }
    current_response = requests.get(url, headers=headers, timeout=60)
    current_response.raise_for_status()
    current: dict[str, Any] = current_response.json()
    before = [str(value) for value in (current.get("nativeClientIds") or [])]
    enabled = bool(current.get("nativeEnabled"))
    print(json.dumps({"mode": "apply" if args.apply else "plan", "nativeEnabled": enabled,
                      "canonicalWebClientConfigured": WEB_CLIENT_ID in before,
                      "nativeClientIdCount": len(before)}, separators=(",", ":")))

    if args.apply and (not enabled or before != [WEB_CLIENT_ID]):
        response = requests.patch(
            url,
            headers=headers,
            json={"nativeEnabled": True, "nativeClientIds": [WEB_CLIENT_ID]},
            timeout=60,
        )
        response.raise_for_status()

    final_response = requests.get(url, headers=headers, timeout=60)
    final_response.raise_for_status()
    final = final_response.json()
    final_ids = [str(value) for value in (final.get("nativeClientIds") or [])]
    if not bool(final.get("nativeEnabled")) or final_ids != [WEB_CLIENT_ID]:
        raise SystemExit("Appwrite native Google audience verification failed")
    print(json.dumps({"nativeGoogle": "verified", "webClientId": WEB_CLIENT_ID}, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
