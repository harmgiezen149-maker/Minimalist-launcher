#!/usr/bin/env bash
# Creates the release signing key, encrypted with SIGNING_PASSPHRASE, in
# <key-dir>: release.p12.gpg (the key) and release-cert.sha256 (the
# certificate fingerprint every published APK is checked against).
#
# The private key only ever exists unencrypted in a temporary directory that
# is removed on exit. Refuses to replace an existing key: a new key would stop
# every installed copy from accepting updates.
#
# Usage: SIGNING_PASSPHRASE=... create-key.sh <key-dir>
set -euo pipefail

key_dir=${1:?usage: create-key.sh <key-dir>}
: "${SIGNING_PASSPHRASE:?SIGNING_PASSPHRASE is not set}"

if [ "${#SIGNING_PASSPHRASE}" -lt 20 ]; then
  echo "SIGNING_PASSPHRASE is shorter than 20 characters; use a long generated one." >&2
  exit 1
fi
if [ -e "$key_dir/release.p12.gpg" ]; then
  echo "$key_dir/release.p12.gpg already exists; refusing to replace the signing key." >&2
  exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

keytool -genkeypair -noprompt \
  -keystore "$work/release.p12" -storetype PKCS12 \
  -storepass:env SIGNING_PASSPHRASE -keypass:env SIGNING_PASSPHRASE \
  -alias launcher -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Minimalist Launcher" >/dev/null 2>&1

keytool -exportcert -rfc -alias launcher \
  -keystore "$work/release.p12" -storetype PKCS12 \
  -storepass:env SIGNING_PASSPHRASE >"$work/cert.pem" 2>/dev/null

mkdir -p "$key_dir"
openssl x509 -in "$work/cert.pem" -noout -fingerprint -sha256 |
  cut -d= -f2 | tr -d ':' | tr 'A-F' 'a-f' >"$key_dir/release-cert.sha256"

printf '%s' "$SIGNING_PASSPHRASE" |
  gpg --quiet --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
    --symmetric --cipher-algo AES256 \
    --output "$key_dir/release.p12.gpg" "$work/release.p12"

echo "Created $key_dir/release.p12.gpg"
echo "Certificate SHA-256: $(cat "$key_dir/release-cert.sha256")"
