#!/usr/bin/env python3
"""Idempotently provision the grade-nine TablesDB schema on Appwrite.

The historic 23-table snapshot is the baseline. Runtime tables added after that
snapshot are declared below. Existing tables/columns/indexes are validated and
left in place; missing resources are created. No historic user data is copied.
"""
from __future__ import annotations

import argparse
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "archive/backend/appwrite.json"
USER_CREATE = ['create("users")']
USER_READ = ['read("users")']
CONTENT = {"lessons", "quizzes", "recipes", "exercises", "learning_nodes", "art_prompts", "wellness_moves"}


def col(key: str, kind: str = "string", *, size: int | None = None,
        required: bool = False, default: Any = None, minimum: int | float | None = None,
        maximum: int | float | None = None) -> dict[str, Any]:
    value: dict[str, Any] = {"$id": key, "type": kind, "required": required}
    if size is not None:
        value["size"] = size
    if default is not None:
        value["default"] = default
    if minimum is not None:
        value["min"] = minimum
    if maximum is not None:
        value["max"] = maximum
    return value


EXTRA_TABLES: list[dict[str, Any]] = [
    {
        "$id": "student_profiles", "name": "پروفایل دانش‌آموز", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64, required=True), col("email", size=320, required=True),
            col("firstName", size=64, required=True), col("lastName", size=64, required=True),
            col("age", "integer", required=True, minimum=4, maximum=100),
            col("birthDate", size=10), col("grade", size=16, required=True),
            col("phone", size=16, required=True), col("schoolName", size=128),
            col("province", size=64), col("city", size=64), col("county", size=64),
            col("gender", size=8), col("subscription", size=24, default="free"),
            col("subscriptionStartMs", "integer", default=0),
            col("subscriptionEndMs", "integer", default=0), col("updatedAtMs", "integer", default=0),
        ],
        "indexes": [{"$id": "uniq_userId", "type": "unique", "attributes": ["userId"]}],
    },
    {
        "$id": "study_progress", "name": "پیشرفت مطالعه", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64), col("packId", size=128),
            col("srsState", size=100000, default=""), col("attempts", size=200000, default=""),
            col("examLedger", size=200000, default=""), col("extras", size=200000, default=""),
            col("updatedAtIso", size=32, default=""),
        ],
        "indexes": [{"$id": "studyUserPackIdx", "type": "key", "attributes": ["userId", "packId"]}],
    },
    {
        "$id": "lesson_notes", "name": "نکات درسی", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64, required=True), col("payload", size=200000, default=""),
            col("text", size=8192, default=""), col("updatedAt", "integer", default=0),
        ],
        "indexes": [{"$id": "notesUserIdx", "type": "key", "attributes": ["userId"]}],
    },
    {
        "$id": "app_state", "name": "وضعیت برنامه", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64, required=True), col("key", size=128, required=True),
            col("payload", size=200000, default=""), col("updatedAt", "integer", default=0),
        ],
        "indexes": [{"$id": "stateUserKeyIdx", "type": "key", "attributes": ["userId", "key"]}],
    },
    {
        "$id": "users", "name": "نگاشت نام کاربری", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64, required=True), col("username", size=24, required=True),
            col("email", size=320, required=True), col("updatedAt", "integer", default=0),
        ],
        "indexes": [
            {"$id": "usersOwnerIdx", "type": "key", "attributes": ["userId"]},
            {"$id": "usersNameIdx", "type": "key", "attributes": ["username"]},
        ],
    },
    {
        "$id": "subscription_orders", "name": "سفارش‌های اشتراک", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64, required=True), col("email", size=320),
            col("status", size=32, required=True), col("planId", size=32),
            col("createdAtMs", "integer", default=0), col("paidAtMs", "integer", default=0),
            col("refundAtMs", "integer", default=0), col("payload", size=65535, default=""),
            col("adminNote", size=2048, default=""),
        ],
        "indexes": [
            {"$id": "ordersUserIdx", "type": "key", "attributes": ["userId", "createdAtMs"]},
            {"$id": "ordersStatusIdx", "type": "key", "attributes": ["status", "createdAtMs"]},
        ],
    },
    {
        "$id": "installments", "name": "اقساط اشتراک", "rowSecurity": True,
        "$permissions": USER_CREATE,
        "columns": [
            col("userId", size=64, required=True), col("email", size=320), col("planId", size=32),
            col("totalToman", "integer", default=0), col("installmentCount", "integer", default=0),
            col("paidCount", "integer", default=0), col("amountEach", "integer", default=0),
            col("status", size=24, default="active"), col("nextDueMs", "integer", default=0),
            col("createdAtMs", "integer", default=0), col("note", size=4096, default=""),
        ],
        "indexes": [
            {"$id": "installmentsUserIdx", "type": "key", "attributes": ["userId", "createdAtMs"]},
            {"$id": "installmentsStatusIdx", "type": "key", "attributes": ["status", "nextDueMs"]},
        ],
    },
]


class Api:
    def __init__(self, endpoint: str, project: str, key: str):
        self.endpoint = endpoint.rstrip("/")
        self.headers = {
            "X-Appwrite-Project": project.strip(),
            "X-Appwrite-Key": key.strip(),
            "Content-Type": "application/json",
        }

    def call(self, method: str, path: str, body: dict[str, Any] | None = None,
             ok: tuple[int, ...] = (200, 201, 202)) -> tuple[int, dict[str, Any]]:
        data = json.dumps(body, ensure_ascii=False).encode() if body is not None else None
        req = urllib.request.Request(self.endpoint + path, data=data, method=method, headers=self.headers)
        try:
            with urllib.request.urlopen(req, timeout=90) as response:
                raw = response.read()
                return response.status, json.loads(raw or b"{}")
        except urllib.error.HTTPError as exc:
            raw = exc.read()
            try:
                parsed = json.loads(raw or b"{}")
            except Exception:
                parsed = {"message": raw.decode(errors="replace")[:500]}
            if exc.code in ok:
                return exc.code, parsed
            raise RuntimeError(f"Appwrite {method} {path}: HTTP {exc.code}: {parsed.get('message', parsed)}") from exc


class Provisioner:
    def __init__(self, api: Api, database: str):
        self.api = api
        self.database = urllib.parse.quote(database, safe="")
        self.created = 0
        self.existing = 0

    def table_path(self, table: str = "") -> str:
        base = f"/tablesdb/{self.database}/tables"
        return base + ("/" + urllib.parse.quote(table, safe="") if table else "")

    def get_or_none(self, path: str) -> dict[str, Any] | None:
        try:
            return self.api.call("GET", path)[1]
        except RuntimeError as exc:
            if "HTTP 404" in str(exc):
                return None
            raise

    @staticmethod
    def column_key(value: dict[str, Any]) -> str:
        return str(value.get("key") or value.get("$id") or "")

    def ensure_table(self, spec: dict[str, Any]) -> None:
        table_id = spec["$id"]
        path = self.table_path(table_id)
        current = self.get_or_none(path)
        if current is None:
            body = {
                "tableId": table_id,
                "name": spec.get("name") or table_id,
                "permissions": spec.get("$permissions") or [],
                "rowSecurity": bool(spec.get("rowSecurity") or spec.get("documentSecurity")),
                "enabled": True,
            }
            self.api.call("POST", self.table_path(), body)
            print(f"CREATE table {table_id}")
            self.created += 1
        else:
            print(f"EXISTS table {table_id}")
            self.existing += 1

        current = self.api.call("GET", path)[1]
        columns = {self.column_key(x): x for x in current.get("columns", [])}
        for column in spec.get("columns", []):
            key = self.column_key(column)
            if key in columns:
                found = columns[key]
                expected = "float" if column.get("type") == "double" else column.get("type")
                actual = "float" if found.get("type") == "double" else found.get("type")
                string_types = {"string", "varchar", "text", "mediumtext", "longtext"}
                compatible = expected == actual or (expected == "string" and actual in string_types)
                if not compatible:
                    raise RuntimeError(f"schema mismatch {table_id}.{key}: {actual} != {expected}")
                self.existing += 1
                continue
            self.create_column(table_id, column)
            print(f"CREATE column {table_id}.{key}")
            self.created += 1

        self.wait_columns(table_id)
        refreshed = self.api.call("GET", path)[1]
        indexes = {str(x.get("key") or x.get("$id")): x for x in refreshed.get("indexes", [])}
        for index in spec.get("indexes", []):
            key = str(index.get("key") or index.get("$id"))
            if key in indexes:
                self.existing += 1
                continue
            attrs = index.get("attributes") or index.get("columns") or []
            body = {"key": key, "type": index.get("type", "key"), "columns": attrs}
            orders = index.get("orders")
            if orders:
                body["orders"] = orders
            self.api.call("POST", path + "/indexes", body)
            print(f"CREATE index {table_id}.{key}")
            self.created += 1
            self.wait_indexes(table_id)

    def create_column(self, table_id: str, column: dict[str, Any]) -> None:
        kind = str(column.get("type"))
        # Varchar-like columns count against MariaDB's row-size budget (UTF-8 may
        # consume four bytes per character). Large JSON/body fields belong in
        # LONGTEXT and remain ordinary Kotlin Strings to clients.
        endpoint_kind = {"double": "float"}.get(kind, kind)
        if endpoint_kind == "string" and int(column.get("size") or 255) > 2048:
            endpoint_kind = "longtext"
        body: dict[str, Any] = {
            "key": self.column_key(column),
            "required": bool(column.get("required", False)),
            "array": bool(column.get("array", False)),
        }
        if endpoint_kind == "string":
            body["size"] = int(column.get("size") or 255)
            if column.get("encrypt") is not None:
                body["encrypt"] = bool(column["encrypt"])
        if not body["required"] and "default" in column:
            body["default"] = column["default"]
        if endpoint_kind in {"integer", "float"}:
            if column.get("min") is not None:
                body["min"] = column["min"]
            if column.get("max") is not None:
                body["max"] = column["max"]
        if endpoint_kind == "enum":
            body["elements"] = column.get("elements") or []
        self.api.call("POST", self.table_path(table_id) + f"/columns/{endpoint_kind}", body)

    def wait_columns(self, table_id: str) -> None:
        for _ in range(90):
            value = self.api.call("GET", self.table_path(table_id))[1]
            pending = [self.column_key(x) for x in value.get("columns", [])
                       if x.get("status") not in (None, "available", "stale")]
            failed = [self.column_key(x) for x in value.get("columns", []) if x.get("status") == "failed"]
            if failed:
                raise RuntimeError(f"failed columns in {table_id}: {failed}")
            if not pending:
                return
            time.sleep(2)
        raise RuntimeError(f"timed out waiting for columns in {table_id}")

    def wait_indexes(self, table_id: str) -> None:
        for _ in range(90):
            value = self.api.call("GET", self.table_path(table_id))[1]
            statuses = [x.get("status") for x in value.get("indexes", [])]
            if "failed" in statuses:
                raise RuntimeError(f"failed index in {table_id}")
            if all(x in (None, "available", "stale") for x in statuses):
                return
            time.sleep(2)
        raise RuntimeError(f"timed out waiting for indexes in {table_id}")

    def seed_release_row(self) -> None:
        table = "app_state"
        row_id = "app_release_grade9"
        path = self.table_path(table) + "/rows/" + row_id
        if self.get_or_none(path) is not None:
            print(f"EXISTS row {table}.{row_id}")
            self.existing += 1
            return
        payload = {
            "latest": 0, "name": "", "min": 0, "url": "", "externalUrl": "",
            "internalUrl": "", "size": 0, "sha256": "", "notes": [],
            "chan": "stable", "rollout": 100, "packageName": "com.hamyareman.p09",
            "gradeId": "grade9", "signingSha256": "",
        }
        body = {
            "rowId": row_id,
            "data": {"userId": "global", "key": row_id,
                     "payload": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
                     "updatedAt": int(time.time() * 1000)},
            "permissions": USER_READ,
        }
        self.api.call("POST", self.table_path(table) + "/rows", body)
        print(f"CREATE row {table}.{row_id}")
        self.created += 1


def normalize_baseline() -> list[dict[str, Any]]:
    raw = json.loads(BASELINE.read_text(encoding="utf-8"))
    result: list[dict[str, Any]] = []
    for source in raw["tablesDB"]:
        table = dict(source)
        table_id = table["$id"]
        table["name"] = table.get("name") or table_id
        if table_id in CONTENT:
            table["$permissions"] = USER_READ
            table["rowSecurity"] = False
        else:
            table["$permissions"] = USER_CREATE
            table["rowSecurity"] = True
        # بعضی snapshotهای قدیمی `$id` و `key` متفاوت دارند. API ستون را با
        # `key` می‌سازد، پس references ایندکس نیز باید به همان key نرمال شوند.
        column_keys = {str(c.get("$id")): str(c.get("key") or c.get("$id")) for c in table.get("columns", [])}
        for index in table.get("indexes", []):
            field = "attributes" if index.get("attributes") is not None else "columns"
            index[field] = [column_keys.get(str(value), str(value)) for value in index.get(field, [])]
        result.append(table)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--endpoint", default=os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1"))
    parser.add_argument("--project", default=os.environ.get("APPWRITE_PROJECT_ID", ""))
    parser.add_argument("--database", default=os.environ.get("APPWRITE_DATABASE_ID", ""))
    parser.add_argument("--api-key", default=os.environ.get("APPWRITE_API_KEY", ""))
    args = parser.parse_args()
    if not args.project or not args.database or not args.api_key:
        parser.error("project, database and API key are required")
    api = Api(args.endpoint, args.project, args.api_key)
    db = api.call("GET", f"/databases/{urllib.parse.quote(args.database, safe='')}")[1]
    print(f"DATABASE {db.get('name', args.database)} ({args.database})")
    provisioner = Provisioner(api, args.database)
    specs = normalize_baseline() + EXTRA_TABLES
    seen: set[str] = set()
    for spec in specs:
        if spec["$id"] in seen:
            raise RuntimeError(f"duplicate schema table {spec['$id']}")
        seen.add(spec["$id"])
        provisioner.ensure_table(spec)
    provisioner.seed_release_row()
    print(json.dumps({"database": args.database, "tables": len(specs),
                      "created": provisioner.created, "existing": provisioner.existing}, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
