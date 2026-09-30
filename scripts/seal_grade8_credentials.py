#!/usr/bin/env python3
"""Seal grade-8 storage credentials for one workflow run's public key."""
from __future__ import annotations

import argparse
import base64
import json
import os
import secrets
from pathlib import Path

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise SystemExit(f"Set {name} in the local environment first")
    return value


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("public_key", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    public = serialization.load_pem_public_key(args.public_key.read_bytes())
    aes_key, nonce = secrets.token_bytes(32), secrets.token_bytes(12)
    plain = json.dumps({
        name: required(name)
        for name in ("APPWRITE_API_KEY", "ARVAN_ACCESS_KEY", "ARVAN_SECRET_KEY")
    }, separators=(",", ":")).encode()
    sealed_key = public.encrypt(
        aes_key,
        padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None),
    )
    ciphertext = AESGCM(aes_key).encrypt(nonce, plain, b"hamyar-grade8-update-v1")
    args.output.write_text(json.dumps({
        "sealedKey": base64.b64encode(sealed_key).decode(),
        "nonce": base64.b64encode(nonce).decode(),
        "ciphertext": base64.b64encode(ciphertext).decode(),
    }, separators=(",", ":")) + "\n")
    print(f"sealed credentials written to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
