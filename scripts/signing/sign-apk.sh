#!/usr/bin/env bash
# Signs an unsigned APK with the encrypted release key from <key-dir> and
# checks that the result carries exactly the expected certificate.
#
# Usage: SIGNING_PASSPHRASE=... sign-apk.sh <unsigned.apk> <signed.apk> <key-dir>
set -euo pipefail

unsigned=${1:?usage: sign-apk.sh <unsigned.apk> <signed.apk> <key-dir>}
signed=${2:?usage: sign-apk.sh <unsigned.apk> <signed.apk> <key-dir>}
key_dir=${3:?usage: sign-apk.sh <unsigned.apk> <signed.apk> <key-dir>}
: "${SIGNING_PASSPHRASE:?SIGNING_PASSPHRASE is not set}"
: "${ANDROID_HOME:?ANDROID_HOME is not set}"

build_tools=$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

printf '%s' "$SIGNING_PASSPHRASE" |
  gpg --quiet --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
    --decrypt --output "$work/release.p12" "$key_dir/release.p12.gpg"

"$build_tools/zipalign" -f -p 4 "$unsigned" "$work/aligned.apk"
"$build_tools/apksigner" sign \
  --ks "$work/release.p12" --ks-type PKCS12 --ks-key-alias launcher \
  --ks-pass env:SIGNING_PASSPHRASE --key-pass env:SIGNING_PASSPHRASE \
  --out "$signed" "$work/aligned.apk"

# Every signer line must carry the expected certificate; the line prefix
# differs between apksigner versions, so match on the digest label only.
actual=$("$build_tools/apksigner" verify --print-certs "$signed" |
  sed -n 's/^.*certificate SHA-256 digest: //p' | sort -u)
expected=$(cat "$key_dir/release-cert.sha256")
if [ "$actual" != "$expected" ]; then
  echo "Signed with certificate(s) [$actual], expected $expected." >&2
  exit 1
fi
echo "Signed $signed (certificate SHA-256 $actual)"
