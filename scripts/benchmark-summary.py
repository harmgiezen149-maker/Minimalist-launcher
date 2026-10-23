#!/usr/bin/env python3
"""Prints the startup times from Macrobenchmark result files as a Markdown table.

Usage: benchmark-summary.py <results.json>...   (the "Baseline profile" workflow
prints the output and appends it to the job summary)
"""
import json
import sys


def ms(value):
    return f"{value:.0f} ms"


def main(paths):
    print("### Cold start of the home screen\n")
    print("| test | median | fastest | slowest | runs |")
    print("| --- | --- | --- | --- | --- |")
    found = False
    for path in paths:
        with open(path) as f:
            results = json.load(f)
        for benchmark in results.get("benchmarks", []):
            metric = benchmark.get("metrics", {}).get("timeToInitialDisplayMs")
            if not metric:
                continue
            found = True
            print(
                f"| `{benchmark['name']}` | {ms(metric['median'])} | {ms(metric['minimum'])} "
                f"| {ms(metric['maximum'])} | {len(metric.get('runs', []))} |"
            )
    if not found:
        print("| no results found | | | | |")
    print("\nMeasured on an emulator: compare the two rows, not the absolute times.")


if __name__ == "__main__":
    main(sys.argv[1:])
