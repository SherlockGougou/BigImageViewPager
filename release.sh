#!/usr/bin/env bash

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ENV_FILE="${RELEASE_ENV_FILE:-$HOME/.config/BigImageViewPager/release.env}"
readonly VERSION="$(sed -n 's/^VERSION_NAME=//p' "$PROJECT_ROOT/gradle.properties" | tail -n 1)"

ASSUME_YES=false
SKIP_CHECKS=false

usage() {
  cat <<'USAGE'
Usage:
  ./release.sh [--yes] [--skip-checks]

Run from a clean master checkout after changing VERSION_NAME and committing the
release changes. The script builds, packages, uploads, validates and publishes
both artifacts to Maven Central, then verifies the public artifacts.

Environment:
  RELEASE_ENV_FILE  Credentials file, default ~/.config/BigImageViewPager/release.env
USAGE
}

die() {
  echo "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

for argument in "$@"; do
  case "$argument" in
    --yes)
      ASSUME_YES=true
      ;;
    --skip-checks)
      SKIP_CHECKS=true
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 1
      ;;
  esac
done

[[ -n "$VERSION" ]] || die "gradle.properties is missing VERSION_NAME"
[[ "$VERSION" != *-SNAPSHOT ]] || die "Release version cannot end with -SNAPSHOT"
require_command git
require_command rg
[[ "$(git -C "$PROJECT_ROOT" branch --show-current)" == "master" ]] ||
  die "Release must run on master"
[[ -z "$(git -C "$PROJECT_ROOT" status --porcelain)" ]] ||
  die "Working tree is not clean; commit release changes first"
if git -C "$PROJECT_ROOT" rev-parse -q --verify "refs/tags/$VERSION" >/dev/null; then
  die "Tag already exists: $VERSION"
fi
rg -q "当前版本：\`$VERSION\`" "$PROJECT_ROOT/README.md" ||
  die "README current version does not match VERSION_NAME=$VERSION"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
else
  echo "Credentials file not found: $ENV_FILE" >&2
  [[ -n "${SIGNING_PASSWORD:-}" && -n "${CENTRAL_USERNAME:-${OSSRH_USERNAME:-}}" ]] ||
    die "Set release credentials in the environment or $ENV_FILE"
fi

: "${SIGNING_KEY_ID:?Missing SIGNING_KEY_ID}"
: "${SIGNING_PASSWORD:?Missing SIGNING_PASSWORD}"
: "${SIGNING_SECRET_KEY_RING_FILE:?Missing SIGNING_SECRET_KEY_RING_FILE}"
CENTRAL_USERNAME="${CENTRAL_USERNAME:-${OSSRH_USERNAME:-}}"
CENTRAL_PASSWORD="${CENTRAL_PASSWORD:-${OSSRH_PASSWORD:-}}"
[[ -n "$CENTRAL_USERNAME" && -n "$CENTRAL_PASSWORD" ]] ||
  die "Missing CENTRAL_USERNAME/CENTRAL_PASSWORD (or OSSRH_USERNAME/OSSRH_PASSWORD)"
export CENTRAL_USERNAME CENTRAL_PASSWORD

if [[ "$ASSUME_YES" != true ]]; then
  read -r -p "Build and publish $VERSION to Maven Central? [y/N] " answer
  case "$answer" in
    y | Y | yes | YES) ;;
    *) die "Release cancelled" ;;
  esac
fi

cd "$PROJECT_ROOT"

if [[ "$SKIP_CHECKS" != true ]]; then
  ./gradlew \
    :library:assembleRelease \
    :library-video-media3:assembleRelease \
    :sample:assembleDebug \
    test lint
fi

git diff --check
./package-central.sh

bundle="$PROJECT_ROOT/build/central-publishing/BigImageViewPager-$VERSION-central-bundle.zip"
[[ -s "$bundle" ]] || die "Central bundle is missing: $bundle"

upload_log="$(mktemp "${TMPDIR:-/tmp}/bigimage-upload.XXXXXX")"
trap 'rm -f "$upload_log"' EXIT HUP INT TERM

./upload-central.sh --yes "$bundle" | tee "$upload_log"
deployment_id="$(awk '/Deployment ID:/{print $3}' "$upload_log" | tail -n 1)"
[[ "$deployment_id" =~ ^[0-9A-Fa-f-]{36}$ ]] ||
  die "Could not parse deployment ID from upload output"

./publish-central.sh publish "$deployment_id" --yes
./publish-central.sh verify "$VERSION"

cat <<EOF

Maven Central release completed: $VERSION

Next:
  git tag -a "$VERSION" -m "$VERSION"
  git push origin master "$VERSION"
  gh release create "$VERSION" --title "$VERSION" \\
    --notes-file <release-notes.md> \\
    "$PROJECT_ROOT/build/central-publishing/$VERSION/repository/com/gouqinglin/BigImageViewPager/$VERSION/BigImageViewPager-$VERSION.aar" \\
    "$PROJECT_ROOT/build/central-publishing/$VERSION/repository/com/gouqinglin/BigImageViewPager-media3/$VERSION/BigImageViewPager-media3-$VERSION.aar"

Then comment and close the related issues. See doc/RELEASE.md.
EOF
