#!/usr/bin/env python3
"""Publish one file atomically to S3 and prove anonymous readability.

The object is first uploaded to a private staging key and verified. The final
copy is requested with ``public-read``. Some S3-compatible providers support
object ACLs only on upload (not CopyObject/PutObjectAcl), so a direct signed PUT
to the final key is the last fallback; object replacement is atomic on success.
No existing healthy public object is replaced when its size and SHA-256 match.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import sys
import uuid
from pathlib import Path
from typing import Any, Callable
from urllib.parse import quote

import boto3
import requests
from botocore.config import Config
from botocore.exceptions import ClientError


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def error_label(exc: BaseException) -> str:
    if isinstance(exc, ClientError):
        error = exc.response.get("Error") or {}
        return f"{error.get('Code', 'ClientError')}: {str(error.get('Message') or '')[:500]}"
    return f"{type(exc).__name__}: {str(exc)[:500]}"


def head(s3: Any, bucket: str, key: str) -> dict[str, Any] | None:
    try:
        return s3.head_object(Bucket=bucket, Key=key)
    except ClientError as exc:
        status = int((exc.response.get("ResponseMetadata") or {}).get("HTTPStatusCode") or 0)
        code = str((exc.response.get("Error") or {}).get("Code") or "")
        if status == 404 or code in {"404", "NoSuchKey", "NotFound"}:
            return None
        raise


def object_matches(row: dict[str, Any] | None, size: int, sha256: str) -> bool:
    metadata = {str(k).lower(): str(v) for k, v in ((row or {}).get("Metadata") or {}).items()}
    return bool(
        row
        and int(row.get("ContentLength") or -1) == size
        and metadata.get("sha256") == sha256
    )


def put_file(
    s3: Any,
    source: Path,
    bucket: str,
    key: str,
    content_type: str,
    cache_control: str,
    metadata: dict[str, str],
    acl: str | None = None,
) -> None:
    """Single PUT avoids multipart extensions rejected by some S3-compatible APIs."""
    params: dict[str, Any] = {
        "Bucket": bucket,
        "Key": key,
        "ContentType": content_type,
        "CacheControl": cache_control,
        "Metadata": metadata,
        "ContentLength": source.stat().st_size,
    }
    if acl:
        params["ACL"] = acl
    with source.open("rb") as body:
        s3.put_object(Body=body, **params)


def public_probe(url: str, size: int) -> tuple[bool, str]:
    """Read at most one anonymous byte while also checking the total size."""
    try:
        with requests.get(
            url,
            headers={"Range": "bytes=0-0", "Accept-Encoding": "identity"},
            stream=True,
            allow_redirects=True,
            timeout=(20, 60),
        ) as response:
            status = response.status_code
            content_range = response.headers.get("Content-Range", "")
            length = response.headers.get("Content-Length", "")
            total = None
            if "/" in content_range:
                try:
                    total = int(content_range.rsplit("/", 1)[1])
                except ValueError:
                    pass
            elif status == 200 and length.isdigit():
                total = int(length)
            ok = status in {200, 206} and (total is None or total == size)
            return ok, f"HTTP {status}, size={total if total is not None else 'unknown'}"
    except requests.RequestException as exc:
        return False, error_label(exc)


def try_action(name: str, action: Callable[[], Any], public_url: str, size: int) -> bool:
    try:
        action()
    except Exception as exc:  # each provider supports a different ACL subset
        print(f"ACL method {name} failed: {error_label(exc)}")
        return False
    public, detail = public_probe(public_url, size)
    print(f"ACL method {name}: public={public} ({detail})")
    return public


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", type=Path, required=True)
    parser.add_argument("--bucket", required=True)
    parser.add_argument("--key", required=True)
    parser.add_argument("--public-url", default="")
    parser.add_argument("--content-type", default="application/octet-stream")
    parser.add_argument("--cache-control", default="public, max-age=3600, must-revalidate")
    parser.add_argument("--metadata", action="append", default=[], help="key=value; repeatable")
    parser.add_argument("--github-env", default=os.getenv("GITHUB_ENV", ""))
    args = parser.parse_args()

    if not args.file.is_file() or args.file.stat().st_size <= 0:
        raise SystemExit(f"source file missing or empty: {args.file}")
    access = os.getenv("ARVAN_ACCESS_KEY") or os.getenv("ARVAN_ACCESS_KEY_ID")
    secret = os.getenv("ARVAN_SECRET_KEY") or os.getenv("ARVAN_SECRET_ACCESS_KEY")
    if not access or not secret:
        raise SystemExit("missing S3 credentials")

    size = args.file.stat().st_size
    sha256 = sha256_file(args.file)
    metadata = {"sha256": sha256}
    for item in args.metadata:
        key, separator, value = item.partition("=")
        if not separator or not key.strip():
            raise SystemExit(f"invalid metadata: {item!r}")
        metadata[key.strip().lower()] = value.strip()

    endpoint = os.getenv("ARVAN_ENDPOINT", "https://s3.ir-thr-at1.arvanstorage.ir")
    region = os.getenv("ARVAN_REGION", "ir-thr-at1")
    public_url = args.public_url or (
        f"https://{args.bucket}.s3.{region}.arvanstorage.ir/"
        + "/".join(quote(part, safe="") for part in args.key.split("/"))
    )
    s3 = boto3.client(
        "s3",
        aws_access_key_id=access,
        aws_secret_access_key=secret,
        endpoint_url=endpoint,
        region_name=region,
        config=Config(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
            # Arvan's S3 API rejects the optional CRC32 request-checksum headers
            # enabled by recent botocore releases. SigV4 still signs every request.
            request_checksum_calculation="when_required",
            response_checksum_validation="when_required",
            connect_timeout=20,
            read_timeout=240,
            retries={"max_attempts": 8, "mode": "adaptive"},
        ),
    )

    current = head(s3, args.bucket, args.key)
    matched = object_matches(current, size, sha256)
    public, detail = public_probe(public_url, size) if matched else (False, "bytes differ or absent")
    print(f"current bytes_match={matched}, public={public} ({detail})")

    common = {
        "Bucket": args.bucket,
        "Key": args.key,
        "ContentType": args.content_type,
        "CacheControl": args.cache_control,
        "Metadata": metadata,
    }

    if matched and not public:
        # Repair an already complete but private object without re-uploading first.
        if try_action(
            "put-object-acl",
            lambda: s3.put_object_acl(Bucket=args.bucket, Key=args.key, ACL="public-read"),
            public_url,
            size,
        ):
            public = True
        if not public:
            def explicit_acl() -> None:
                existing_acl = s3.get_object_acl(Bucket=args.bucket, Key=args.key)
                owner = existing_acl["Owner"]
                s3.put_object_acl(
                    Bucket=args.bucket,
                    Key=args.key,
                    AccessControlPolicy={
                        "Owner": owner,
                        "Grants": [
                            {"Grantee": {"Type": "CanonicalUser", "ID": owner["ID"]}, "Permission": "FULL_CONTROL"},
                            {
                                "Grantee": {
                                    "Type": "Group",
                                    "URI": "http://acs.amazonaws.com/groups/global/AllUsers",
                                },
                                "Permission": "READ",
                            },
                        ],
                    },
                )
            public = try_action("explicit-object-acl", explicit_acl, public_url, size)
        if not public:
            public = try_action(
                "self-copy-public-read",
                lambda: s3.copy_object(
                    ACL="public-read",
                    CopySource={"Bucket": args.bucket, "Key": args.key},
                    MetadataDirective="REPLACE",
                    **common,
                ),
                public_url,
                size,
            )

    if not matched:
        staging = f".staging/{uuid.uuid4().hex}/{args.key}"
        try:
            put_file(
                s3,
                args.file,
                args.bucket,
                staging,
                args.content_type,
                "no-store",
                metadata,
            )
            staged = head(s3, args.bucket, staging)
            if not object_matches(staged, size, sha256):
                raise RuntimeError("staging verification failed")
            public = try_action(
                "staging-copy-public-read",
                lambda: s3.copy_object(
                    ACL="public-read",
                    CopySource={"Bucket": args.bucket, "Key": staging},
                    MetadataDirective="REPLACE",
                    **common,
                ),
                public_url,
                size,
            )
        finally:
            try:
                s3.delete_object(Bucket=args.bucket, Key=staging)
            except Exception as exc:
                print(f"staging cleanup warning: {error_label(exc)}")

    if not public:
        # A completed S3 PUT replaces the object atomically; a failed request does
        # not expose a partially written APK at the final key.
        public = try_action(
            "direct-put-public-read",
            lambda: put_file(
                s3,
                args.file,
                args.bucket,
                args.key,
                args.content_type,
                args.cache_control,
                metadata,
                acl="public-read",
            ),
            public_url,
            size,
        )

    final = head(s3, args.bucket, args.key)
    if not object_matches(final, size, sha256):
        raise SystemExit("final S3 size/SHA-256 verification failed")
    public, detail = public_probe(public_url, size)
    if not public:
        raise SystemExit(
            "final object is complete but not anonymously readable; grant the Arvan key "
            f"PutObjectAcl/WRITE_ACP or make this prefix public ({detail})"
        )

    if args.github_env:
        with open(args.github_env, "a", encoding="utf-8") as output:
            output.write(f"ARVAN_APK_URL={public_url}\n")
            output.write(f"APK_SIZE={size}\n")
            output.write(f"APK_SHA={sha256}\n")
    print(f"PUBLISHED public object {args.key} size={size} sha256={sha256} url={public_url}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"S3 publish failed: {error_label(exc)}", file=sys.stderr)
        sys.exit(1)
