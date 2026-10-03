#!/usr/bin/env python3
"""خلاصه‌ساز خروجی بیلد CI.

لاگ Gradle (ci-report/gradle.log)، نتیجه‌ی تست‌ها و گزارش‌های لینت را می‌خواند و
یک خلاصه‌ی فشرده در ci-report/summary.md می‌نویسد تا بدون دانلود آرتیفکت‌های
Actions هم قابل خواندن باشد.

این اسکریپت فقط می‌خواند و می‌نویسد؛ هیچ تماس شبکه‌ای ندارد.
"""

from __future__ import annotations

import html
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPORT_DIR = ROOT / "ci-report"
GRADLE_LOG = REPORT_DIR / "gradle.log"

# «w: file:///home/runner/work/repo/repo/shared/...» → مسیر نسبی
# (greedy تا آخرین تکرارِ نام مخزن برداشته شود، چون مسیر رانر دوبار آن را دارد)
FILE_URL = re.compile(r"file://\S*/com\.hamyareman\.ir/")
KOTLIN_WARN = re.compile(r"^w: ")
KOTLIN_ERROR = re.compile(r"^e: ")
TASK_FAILED = re.compile(r"^> Task (\S+) FAILED")
WHAT_WENT_WRONG = re.compile(r"^\* What went wrong:")


def rel(text: str) -> str:
    """مسیرهای مطلقِ رانر را به مسیر نسبی مخزن تبدیل می‌کند."""
    return FILE_URL.sub("", text).replace("/home/runner/work/com.hamyareman.ir/com.hamyareman.ir/", "")


def read_log() -> list[str]:
    if not GRADLE_LOG.exists():
        return []
    return GRADLE_LOG.read_text(encoding="utf-8", errors="replace").splitlines()


def collect_gradle(lines: list[str]) -> dict:
    warnings: Counter[str] = Counter()
    errors: list[str] = []
    failed_tasks: list[str] = []
    failure_blocks: list[str] = []

    for index, raw in enumerate(lines):
        line = rel(raw.rstrip())
        if KOTLIN_WARN.match(line):
            warnings[line[3:].strip()] += 1
        elif KOTLIN_ERROR.match(line):
            errors.append(line[3:].strip())
        elif TASK_FAILED.match(line):
            failed_tasks.append(TASK_FAILED.match(line).group(1))
        elif WHAT_WENT_WRONG.match(line):
            block = [rel(x.rstrip()) for x in lines[index : index + 12]]
            failure_blocks.append("\n".join(block))

    return {
        "warnings": warnings,
        "errors": errors,
        "failed_tasks": failed_tasks,
        "failure_blocks": failure_blocks,
    }


def collect_tests() -> tuple[list[tuple[str, int, int, int, int]], list[str]]:
    rows: list[tuple[str, int, int, int, int]] = []
    failures: list[str] = []
    for xml_path in sorted(ROOT.glob("*/*/build/test-results/**/*.xml")):
        try:
            suite = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue
        if suite.tag != "testsuite":
            continue
        name = suite.get("name", xml_path.stem)
        tests = int(suite.get("tests", 0))
        fails = int(suite.get("failures", 0))
        errs = int(suite.get("errors", 0))
        skipped = int(suite.get("skipped", 0))
        rows.append((name, tests, fails, errs, skipped))
        for case in suite.iter("testcase"):
            for bad in list(case.findall("failure")) + list(case.findall("error")):
                message = (bad.get("message") or "").strip().splitlines()
                head = message[0] if message else bad.get("type", "?")
                failures.append(f"{name}.{case.get('name')} — {head}")
    return rows, failures


def collect_lint() -> tuple[dict[str, Counter], dict[str, list[str]]]:
    per_report: dict[str, Counter] = {}
    samples: dict[str, list[str]] = defaultdict(list)
    for xml_path in sorted(ROOT.glob("*/*/build/reports/lint-results-*.xml")):
        try:
            root = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue
        label = str(xml_path.relative_to(ROOT))
        counts: Counter[str] = Counter()
        for issue in root.iter("issue"):
            severity = issue.get("severity", "?")
            issue_id = issue.get("id", "?")
            counts[f"{severity}:{issue_id}"] += 1
            if severity in {"Error", "Fatal"} and len(samples[issue_id]) < 5:
                location = issue.find("location")
                where = ""
                if location is not None:
                    where = f"{location.get('file', '')}:{location.get('line', '')}"
                samples[issue_id].append(
                    f"{rel(where)} — {html.unescape(issue.get('message', ''))}"
                )
        per_report[label] = counts
    return per_report, samples


def collect_apks() -> list[str]:
    found = []
    for apk in sorted(ROOT.glob("apps/*/build/outputs/apk/**/*.apk")):
        size_mb = apk.stat().st_size / (1024 * 1024)
        found.append(f"{apk.relative_to(ROOT)} — {size_mb:.1f} MB")
    return found


def bucket(warning: str) -> str:
    """دسته‌بندی هشدارهای کاتلین بر اساس نوع پیام."""
    lowered = warning.lower()
    if "is deprecated" in lowered:
        return "Deprecated API"
    if "never used" in lowered or "is unused" in lowered:
        return "Unused code"
    if "unnecessary" in lowered or "no cast needed" in lowered or "always" in lowered:
        return "Redundant code"
    if "opt-in" in lowered or "experimental" in lowered:
        return "Opt-in / Experimental"
    if "shadow" in lowered or "name shadowed" in lowered:
        return "Name shadowing"
    return "Other"


def main() -> None:
    REPORT_DIR.mkdir(exist_ok=True)
    lines = read_log()
    gradle = collect_gradle(lines)
    test_rows, test_failures = collect_tests()
    lint_reports, lint_samples = collect_lint()
    apks = collect_apks()

    out: list[str] = ["# گزارش بیلد کامل (پایهٔ نهم — اپ بیس)", ""]

    total_tests = sum(r[1] for r in test_rows)
    total_fail = sum(r[2] + r[3] for r in test_rows)
    out += [
        "## وضعیت کلی",
        "",
        f"- تسک‌های شکست‌خورده: **{len(gradle['failed_tasks'])}**",
        f"- خطای کامپایل کاتلین: **{len(gradle['errors'])}**",
        f"- هشدار کامپایلر (یکتا): **{len(gradle['warnings'])}**",
        f"- تست واحد: **{total_tests}** اجرا، **{total_fail}** ناموفق",
        f"- APK ساخته‌شده: **{len(apks)}**",
        "",
    ]

    if gradle["failed_tasks"]:
        out += ["## تسک‌های شکست‌خورده", ""]
        out += [f"- `{t}`" for t in gradle["failed_tasks"]] + [""]

    if gradle["errors"]:
        out += ["## خطاهای کامپایل", "", "```"]
        out += gradle["errors"][:200] + ["```", ""]

    if gradle["failure_blocks"]:
        out += ["## جزئیات شکست Gradle", "", "```"]
        out += gradle["failure_blocks"][:5] + ["```", ""]

    if apks:
        out += ["## خروجی‌ها", ""] + [f"- `{a}`" for a in apks] + [""]

    if test_rows:
        out += ["## تست واحد", "", "| کلاس | تست | ناموفق | خطا | رد‌شده |", "|---|---:|---:|---:|---:|"]
        for name, tests, fails, errs, skipped in sorted(test_rows):
            out.append(f"| `{name}` | {tests} | {fails} | {errs} | {skipped} |")
        out.append("")
    if test_failures:
        out += ["### تست‌های ناموفق", ""] + [f"- {f}" for f in test_failures[:50]] + [""]

    if lint_reports:
        out += ["## لینت", ""]
        for label, counts in lint_reports.items():
            errors = sum(v for k, v in counts.items() if k.startswith(("Error", "Fatal")))
            warns = sum(v for k, v in counts.items() if k.startswith("Warning"))
            out.append(f"### `{label}` — {errors} خطا، {warns} هشدار")
            out.append("")
            for key, count in counts.most_common(25):
                out.append(f"- `{key}` × {count}")
            out.append("")
        if lint_samples:
            out += ["### نمونهٔ خطاهای لینت", ""]
            for issue_id, items in sorted(lint_samples.items()):
                out.append(f"- **{issue_id}**")
                out += [f"  - {i}" for i in items]
            out.append("")

    if gradle["warnings"]:
        grouped: dict[str, list[tuple[str, int]]] = defaultdict(list)
        for warning, count in gradle["warnings"].most_common():
            grouped[bucket(warning)].append((warning, count))
        out += ["## هشدارهای کامپایلر کاتلین", ""]
        for group, items in sorted(grouped.items(), key=lambda kv: -len(kv[1])):
            out.append(f"### {group} — {len(items)} مورد یکتا")
            out.append("")
            for warning, count in items[:40]:
                suffix = f" (×{count})" if count > 1 else ""
                out.append(f"- `{warning}`{suffix}")
            if len(items) > 40:
                out.append(f"- … و {len(items) - 40} مورد دیگر (ci-report/warnings.txt)")
            out.append("")

        (REPORT_DIR / "warnings.txt").write_text(
            "\n".join(f"{c}\t{w}" for w, c in gradle["warnings"].most_common()) + "\n",
            encoding="utf-8",
        )

    (REPORT_DIR / "summary.md").write_text("\n".join(out) + "\n", encoding="utf-8")

    # لاگ خام حذف می‌شود تا مخزن سنگین نشود؛ فقط خطوط مهم می‌ماند.
    if GRADLE_LOG.exists():
        keep = [
            rel(line.rstrip())
            for line in lines
            if line.startswith(("w: ", "e: ", "> Task", "FAILURE", "* What went wrong", "BUILD "))
        ]
        GRADLE_LOG.write_text("\n".join(keep) + "\n", encoding="utf-8")

    print(f"summary.md نوشته شد — {len(out)} خط")


if __name__ == "__main__":
    main()
