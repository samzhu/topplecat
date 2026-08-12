#!/usr/bin/env bash
# Verifies the bytecode and Gradle metadata of every published ToppleCat module.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

TOPPLECAT_RELEASE_ROOT="$root" python3 - <<'PY'
import json
import os
import pathlib
import struct
import zipfile

root = pathlib.Path(os.environ["TOPPLECAT_RELEASE_ROOT"])
modules = ("topplecat-core", "topplecat-junit", "topplecat-report", "topplecat-gradle-plugin")
expected_major = 65
class_count = 0

for module in modules:
    jars = sorted((root / module / "build" / "libs").glob("*.jar"))
    if not jars:
        raise SystemExit(f"Release gate failed: no published JARs found for {module}")
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            for entry in archive.infolist():
                if not entry.filename.endswith(".class"):
                    continue
                class_count += 1
                data = archive.read(entry)
                if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe":
                    raise SystemExit(
                        f"Release gate failed: malformed class file {entry.filename} in {jar}"
                    )
                minor, major = struct.unpack(">HH", data[4:8])
                if major != expected_major:
                    raise SystemExit(
                        f"Release gate failed: {jar}!{entry.filename} uses class-file "
                        f"major {major}, expected {expected_major} (minor {minor})"
                    )
                if minor == 0xFFFF:
                    raise SystemExit(
                        f"Release gate failed: preview class file {entry.filename} in {jar}"
                    )

metadata = sorted(root.glob("topplecat-*/build/publications/*/module.json"))
if len(metadata) != len(modules):
    raise SystemExit(
        f"Release gate failed: expected {len(modules)} Gradle module metadata files, "
        f"found {len(metadata)}"
    )
for path in metadata:
    document = json.loads(path.read_text())
    for variant in document.get("variants", []):
        attributes = variant.get("attributes", {})
        if attributes.get("org.gradle.libraryelements") == "jar" and attributes.get(
            "org.gradle.usage"
        ) in {"java-api", "java-runtime"}:
            actual = attributes.get("org.gradle.jvm.version")
            if actual != 21:
                raise SystemExit(
                    f"Release gate failed: {path} variant {variant.get('name')} advertises "
                    f"JVM {actual}, expected 21"
                )

if class_count == 0:
    raise SystemExit("Release gate failed: no ToppleCat class files were scanned")
print(
    f"Release verification: scanned {class_count} ToppleCat class files; "
    "all use Java 21 major version 65 with no preview flag and publication metadata agrees."
)
PY
