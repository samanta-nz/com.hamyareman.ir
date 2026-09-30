#!/usr/bin/env python3
"""خواندن پیکربندی سرورهای S3 از config/storage-internal.json.

هیچ کلیدی در این فایل یا در آن JSON نیست؛ کلیدها فقط از متغیرهای محیطیِ رانر
خوانده می‌شوند (نام متغیر در خود JSON آمده است).
"""

from __future__ import annotations

import json
import os
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import quote

import boto3
from botocore.client import Config

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "storage-internal.json"


@dataclass(frozen=True)
class Target:
    name: str
    label: str
    endpoint: str
    region: str
    bucket: str
    public_url_style: str
    access_key: str
    secret_key: str
    addressing_style: str = "path"
    signature_version: str = "s3v4"
    list_objects_version: int = 2

    @property
    def host(self) -> str:
        return self.endpoint.split("://", 1)[-1].rstrip("/")

    def public_url(self, key: str) -> str:
        encoded = "/".join(quote(part, safe="") for part in key.split("/"))
        if self.public_url_style == "virtual-host":
            return f"https://{self.bucket}.{self.host}/{encoded}"
        return f"{self.endpoint.rstrip('/')}/{self.bucket}/{encoded}"

    def client(self, *, addressing: str | None = None):
        # boto3 با endpoint سفارشی به‌طور پیش‌فرض virtual-host می‌سازد؛ بعضی
        # ارائه‌دهنده‌ها فقط path-style را امضا می‌کنند و وگرنه
        # SignatureDoesNotMatch می‌دهند.
        extra: dict = {"s3": {"addressing_style": addressing or self.addressing_style}}
        return boto3.client(
            "s3",
            endpoint_url=self.endpoint,
            region_name=self.region,
            aws_access_key_id=self.access_key,
            aws_secret_access_key=self.secret_key,
            config=Config(
                # بعضی نصب‌های Ceph فقط SigV2 را می‌پذیرند و با SigV4
                # SignatureDoesNotMatch می‌دهند.
                signature_version=self.signature_version,
                # از botocore 1.36 چک‌سام CRC32 روی هر PutObject اجباری می‌شود و
                # ارائه‌دهنده‌های S3-سازگار آن را رد می‌کنند.
                request_checksum_calculation="when_required",
                response_checksum_validation="when_required",
                connect_timeout=20,
                read_timeout=240,
                retries={"max_attempts": 6, "mode": "standard"},
                **extra,
            ),
        )


def load_config() -> dict:
    return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))


def target(name: str | None = None, *, require_keys: bool = True) -> Target:
    config = load_config()
    chosen = name or config.get("active") or "parspack"
    raw = config["providers"][chosen]
    access = os.getenv(raw["accessKeyEnv"], "").strip()
    secret = os.getenv(raw["secretKeyEnv"], "").strip()
    if require_keys and (not access or not secret):
        raise SystemExit(
            f"کلیدهای «{raw['label']}» تنظیم نشده‌اند — "
            f"{raw['accessKeyEnv']} و {raw['secretKeyEnv']} را در Secrets مخزن بگذارید."
        )
    return Target(
        name=chosen,
        label=raw["label"],
        endpoint=os.getenv(f"{chosen.upper()}_ENDPOINT", "").strip() or raw["endpoint"],
        region=os.getenv(f"{chosen.upper()}_REGION", "").strip() or raw["region"],
        bucket=os.getenv(f"{chosen.upper()}_BUCKET", "").strip() or raw["bucket"],
        public_url_style=raw.get("publicUrlStyle", "path"),
        access_key=access,
        secret_key=secret,
        addressing_style=raw.get("addressingStyle", "path"),
        signature_version=raw.get("signatureVersion", "s3v4"),
        list_objects_version=int(raw.get("listObjectsVersion", 2)),
    )


def list_all(client, bucket: str, target_info: "Target", prefix: str = "") -> list[dict]:
    """فهرست همهٔ اشیاء؛ اگر ListObjectsV2 پشتیبانی نشود به نسخهٔ ۱ برمی‌گردد."""
    if target_info.list_objects_version == 2:
        try:
            found: list[dict] = []
            for page in client.get_paginator("list_objects_v2").paginate(Bucket=bucket, Prefix=prefix):
                found.extend(page.get("Contents") or [])
            return found
        except Exception:  # noqa: BLE001 — به نسخهٔ ۱ برمی‌گردیم
            pass
    found = []
    for page in client.get_paginator("list_objects").paginate(Bucket=bucket, Prefix=prefix):
        found.extend(page.get("Contents") or [])
    return found
