#!/usr/bin/env python3
"""Fail-closed static audit for the nine APK identities and mirrored assets."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "apps/hamyar-app"
EXPECTED = {
    "p04": ("grade4", "com.hamyareman.p04"),
    "p05": ("grade5", "com.hamyareman.p05"),
    "p06": ("grade6", "com.hamyareman.p06"),
    "p07": ("grade7", "com.hamyareman.p07"),
    "p08": ("grade8", "com.hamyareman.p08"),
    "p09": ("grade9", "com.hamyareman.ir"),
    "p10": ("grade10", "com.hamyareman.p10"),
    "p11": ("grade11", "com.hamyareman.p11"),
    "p12": ("grade12", "com.hamyareman.p12"),
}


def fail(message: str) -> None:
    raise SystemExit(f"grade architecture validation failed: {message}")


def main() -> int:
    gradle = (APP / "build.gradle.kts").read_text()
    identities: dict[str, tuple[str, str, str]] = {}
    calls = {
        match.group("flavor"): (match.group("grade"), match.group("package"))
        for match in re.finditer(
            r'gradeApp\("(?P<flavor>p\d{2})",\s*\d+,\s*"(?P<grade>grade\d+)",'
            r'.*?"(?P<package>com\.hamyareman\.(?:ir|p\d{2}))",\s*"Base-\d{2}"',
            gradle,
        )
    }
    for flavor, (grade_id, package_name) in EXPECTED.items():
        if calls.get(flavor) != (grade_id, package_name):
            fail(f"{flavor} identity is not {grade_id}/{package_name}")
        row = f"app_release_{grade_id}"
        identities[flavor] = (grade_id, package_name, row)
    if 'buildConfigField("String", "UPDATE_ROW_ID", "\\"app_release_$id\\"")' not in gradle:
        fail("gradeApp helper does not derive one update row per grade")
    packages = [item[1] for item in identities.values()]
    rows = [item[2] for item in identities.values()]
    if len(set(packages)) != 9 or len(set(rows)) != 9:
        fail("package names and update rows must both be unique")

    signer = (ROOT / "config/release-signing-sha256.txt").read_text().strip().replace(":", "").lower()
    if not re.fullmatch(r"[0-9a-f]{64}", signer):
        fail("release signer fingerprint is not one SHA-256 digest")

    catalog = json.loads((APP / "src/main/assets/content/catalog.json").read_text())
    public_map = json.loads((APP / "src/main/assets/content/server-map.json").read_text())["entries"]
    by_id = {entry["id"]: entry for entry in public_map}
    if len(by_id) != len(public_map):
        fail("public-content-map contains duplicate IDs")
    for item in catalog["items"]:
        mapped = by_id.get(item["aw"])
        if not mapped or mapped["key"] != item["key"]:
            fail(f"catalog mapping mismatch for {item['id']}")
    labs = [item["id"] for item in catalog["items"] if item["id"].startswith("lab-")]
    if any(not re.fullmatch(r"lab-\d{2}-.+\.html", item_id) for item_id in labs):
        fail("a laboratory page is not edition-scoped")

    kotlin = "\n".join(path.read_text(errors="ignore") for path in (APP / "src/main/java").rglob("*.kt"))
    tile_ids = set(re.findall(r'"((?:bl|cl|cy|hl|hyp|jn|jo|ln|mf|mu|pd|sk|th)-[a-z0-9-]+)"', kotlin))
    covers = {path.stem for path in (APP / "src/main/assets/practice-covers").glob("*.jpg")}
    missing = sorted(tile_ids - covers)
    if missing:
        fail(f"missing {len(missing)} tile covers: {missing[:10]}")

    result = {
        "flavors": identities,
        "uniquePackages": len(set(packages)),
        "uniqueUpdateRows": len(set(rows)),
        "catalogItems": len(catalog["items"]),
        "publicMirrorEntries": len(public_map),
        "tileIds": len(tile_ids),
        "covers": len(covers),
        "missingCovers": missing,
        "signerSha256": signer,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
