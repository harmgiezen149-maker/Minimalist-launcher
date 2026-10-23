#!/usr/bin/env python3
"""Prints line and branch coverage of a JaCoCo XML report as a Markdown table.

Usage: coverage-summary.py <report.xml>   (CI appends the output to the job summary)
"""
import sys
import xml.etree.ElementTree as ET


def counters(node):
    found = {c.get("type"): (int(c.get("covered")), int(c.get("missed"))) for c in node.findall("counter")}
    return found.get("LINE", (0, 0)), found.get("BRANCH", (0, 0))


def percent(covered_missed):
    covered, missed = covered_missed
    total = covered + missed
    return f"{100 * covered / total:.1f}% ({covered}/{total})" if total else "–"


def main(path):
    root = ET.parse(path).getroot()
    print("### Unit test coverage\n")
    print("| package | lines | branches |")
    print("| --- | --- | --- |")
    for package in sorted(root.findall("package"), key=lambda p: p.get("name")):
        lines, branches = counters(package)
        print(f"| `{package.get('name').replace('/', '.')}` | {percent(lines)} | {percent(branches)} |")
    lines, branches = counters(root)
    print(f"| **total** | **{percent(lines)}** | **{percent(branches)}** |")


if __name__ == "__main__":
    main(sys.argv[1])
