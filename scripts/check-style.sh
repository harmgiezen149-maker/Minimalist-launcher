#!/usr/bin/env bash
# Runs ktlint (formatting, .editorconfig) and detekt (code smells,
# config/detekt.yml) with pinned versions whose checksums are verified.
# Same command locally and in CI. Pass --format to let ktlint fix what it can.
set -euo pipefail
cd "$(dirname "$0")/.."

tools=${TOOLS_DIR:-$HOME/.cache/minimalist-launcher-tools}
mkdir -p "$tools"

# fetch <url> <sha256> <file>
fetch() {
  if [ -f "$3" ] && echo "$2  $3" | sha256sum --check --status; then return; fi
  curl --fail --silent --show-error --location --output "$3.part" "$1"
  echo "$2  $3.part" | sha256sum --check --status || { echo "Checksum mismatch for $1" >&2; exit 1; }
  mv "$3.part" "$3"
}

maven=https://repo1.maven.org/maven2
fetch "$maven/com/pinterest/ktlint/ktlint-cli/1.8.0/ktlint-cli-1.8.0-all.jar" \
  369ad2b789f95a011f807e1fcb690ccef80bd7cd014fd139e73ae82dcc0baeab "$tools/ktlint-1.8.0.jar"
fetch "$maven/io/gitlab/arturbosch/detekt/detekt-cli/1.23.8/detekt-cli-1.23.8-all.jar" \
  3afe89a11120303c73c9bdda3d8fe558dd9070a6937d27819ddc04b275381245 "$tools/detekt-1.23.8.jar"

java -jar "$tools/ktlint-1.8.0.jar" ${1:+"$1"} "**/*.kt" "**/*.kts"
java -jar "$tools/detekt-1.23.8.jar" --input app/src,baselineprofile/src \
  --config config/detekt.yml --build-upon-default-config \
  --baseline config/detekt-baseline.xml
echo "Style checks passed."
