"""Read-only verification; plaintext and key stay in RAM and are never logged."""
import json
from pathlib import Path
import requests
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from verify_music_bucket_files import fetch_key

url = "https://c539776.parspack.net/Bucket/Html-files/app/calm/journey/11-calm-waterspace.html"
r = requests.get(url, headers={"Accept-Encoding": "identity"}, timeout=(20, 120))
r.raise_for_status()
blob = r.content
assert blob[:4] in (b"HMK1", b"MHK1"), "Unsupported envelope"
plain = AESGCM(fetch_key()).decrypt(blob[4:16], blob[16:], None)
assert plain.lstrip(b"\xef\xbb\xbf \t\r\n").startswith(b"<"), "Decrypted payload is not HTML"
report = {"url": url, "status": r.status_code, "magic": blob[:4].decode(), "encryptedBytes": len(blob), "authenticationVerified": True, "htmlVerified": True}
Path("ci-report").mkdir(exist_ok=True)
Path("ci-report/journey-envelope.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report))
