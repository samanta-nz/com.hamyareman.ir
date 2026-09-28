#!/usr/bin/env python3
"""استخراجِ شیمِ سیک از MathLessonScreen.kt به JS خالص (برای تست با node/jsdom).

用法:  python3 tools/seek-shim/extract_shim.py > /tmp/shim.js
"""
import sys

KT = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MathLessonScreen.kt"


def unescape_kotlin(t: str) -> str:
    out, i = [], 0
    while i < len(t):
        c = t[i]
        if c == "\\" and i + 1 < len(t):
            n = t[i + 1]
            if n in ('"', "\\", "$"):
                out.append(n); i += 2; continue
            if n == "u":
                out.append(t[i:i + 6]); i += 6; continue
            if n == "n":
                out.append("\n"); i += 2; continue
            if n == "t":
                out.append("\t"); i += 2; continue
            out.append(c); i += 1; continue
        out.append(c); i += 1
    return "".join(out)


def main() -> int:
    lines = open(KT, encoding="utf-8").read().split("\n")
    i0 = next(i for i, l in enumerate(lines) if "val shim = " in l)
    i1 = next(i for i, l in enumerate(lines) if "val i = html.lastIndexOf" in l)
    parts = []
    for ln in range(i0, i1):
        l = lines[ln].strip()
        if l.startswith("val shim = "):
            l = l[len("val shim = "):].strip()
        if not l.startswith('"'):
            continue
        body = l[1:]
        if body.endswith("+"):
            body = body[:-1].rstrip()
        assert body.endswith('"'), (ln + 1, body[-40:])
        parts.append(unescape_kotlin(body[:-1]))
    js = "".join(parts)
    assert js.startswith("<script>") and js.endswith("</script>"), "shim markup changed"
    sys.stdout.write(js[len("<script>"):-len("</script>")])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
