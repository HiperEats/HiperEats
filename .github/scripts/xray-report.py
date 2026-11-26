"""Builds the one JUnit XML report that CI imports into Xray (HE-41).

The listener of xray-junit-extensions writes one report per test engine and Maven run in
backend/target/xray. This script keeps only the test cases linked to a story (property
"requirements", from @Requirement or the @HE-n tag of a Gherkin feature) and writes them as one
<testsuites> document, so that a single import creates the Test Execution with all of them.
The JVM properties of each suite are left out: Xray does not need them.

Usage: python3 xray-report.py <reports directory> <output file>
Prints the number of test cases written; writes no file when there are none.
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def is_linked_to_a_story(testcase):
    return any(
        prop.get("name") == "requirements" and prop.get("value", "").strip()
        for prop in testcase.iter("property")
    )


def outcome(testcase):
    for kind in ("failure", "error", "skipped"):
        if testcase.find(kind) is not None:
            return kind
    return "passed"


def total_time(testcases):
    """The time of the kept test cases only, not of the whole suite."""
    seconds = 0.0
    for case in testcases:
        try:
            seconds += float(case.get("time", "0"))
        except ValueError:
            pass
    return f"{seconds:.3f}"


def suites_of(report):
    root = ET.parse(report).getroot()
    return [root] if root.tag == "testsuite" else root.findall("testsuite")


def story_suites(reports_dir):
    for report in sorted(Path(reports_dir).glob("*.xml")):
        for suite in suites_of(report):
            cases = [case for case in suite.findall("testcase") if is_linked_to_a_story(case)]
            if not cases:
                continue
            outcomes = [outcome(case) for case in cases]
            story_suite = ET.Element("testsuite", {
                "name": suite.get("name", report.stem),
                "tests": str(len(cases)),
                "failures": str(outcomes.count("failure")),
                "errors": str(outcomes.count("error")),
                "skipped": str(outcomes.count("skipped")),
                "time": total_time(cases),
            })
            if suite.get("timestamp"):
                story_suite.set("timestamp", suite.get("timestamp"))
            story_suite.extend(cases)
            yield story_suite


def main(reports_dir, output):
    if not Path(reports_dir).is_dir():
        sys.exit(f"No reports directory: {reports_dir}")
    root = ET.Element("testsuites")
    root.extend(story_suites(reports_dir))
    count = sum(int(suite.get("tests")) for suite in root)
    if count:
        ET.ElementTree(root).write(output, encoding="UTF-8", xml_declaration=True)
    print(count)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
