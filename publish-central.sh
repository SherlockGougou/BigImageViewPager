#!/usr/bin/env bash

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ENV_FILE="${RELEASE_ENV_FILE:-$HOME/.config/BigImageViewPager/release.env}"
readonly CENTRAL_STATUS_ENDPOINT="https://central.sonatype.com/api/v1/publisher/status"
readonly CENTRAL_DEPLOYMENT_ENDPOINT="https://central.sonatype.com/api/v1/publisher/deployment"
readonly MAVEN_REPOSITORY_BASE="https://repo.maven.apache.org/maven2/com/gouqinglin"
readonly POLL_INTERVAL_SECONDS="${CENTRAL_POLL_INTERVAL_SECONDS:-15}"
readonly POLL_ATTEMPTS="${CENTRAL_POLL_ATTEMPTS:-120}"

CENTRAL_CONFIG=""
VERIFY_TMP=""

die() {
  echo "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

load_credentials() {
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
  fi

  CENTRAL_USERNAME="${CENTRAL_USERNAME:-${OSSRH_USERNAME:-}}"
  CENTRAL_PASSWORD="${CENTRAL_PASSWORD:-${OSSRH_PASSWORD:-}}"
  [[ -n "$CENTRAL_USERNAME" && -n "$CENTRAL_PASSWORD" ]] ||
    die "Missing CENTRAL_USERNAME/CENTRAL_PASSWORD (or OSSRH_USERNAME/OSSRH_PASSWORD)"

  require_command curl
  require_command base64
  require_command jq

  CENTRAL_CONFIG="$(mktemp "${TMPDIR:-/tmp}/bigimage-central.XXXXXX")"
  chmod 600 "$CENTRAL_CONFIG"
  printf 'header = "Authorization: Bearer %s"\n' \
    "$(printf '%s:%s' "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 | tr -d '\r\n')" \
    >"$CENTRAL_CONFIG"
}

cleanup() {
  if [[ -n "$CENTRAL_CONFIG" ]]; then
    rm -f "$CENTRAL_CONFIG"
  fi
  if [[ -n "$VERIFY_TMP" ]]; then
    rm -rf "$VERIFY_TMP"
  fi
}
trap cleanup EXIT HUP INT TERM

api_post() {
  curl --silent --show-error --fail-with-body --config "$CENTRAL_CONFIG" --request POST "$1"
}

deployment_status() {
  api_post "$CENTRAL_STATUS_ENDPOINT?id=$1"
}

print_status() {
  local body="$1"
  printf '%s\n' "$body" | jq '{deploymentId, deploymentName, deploymentState, errors, warnings}'
}

wait_for_validation() {
  local deployment_id="$1"
  local attempt state body errors

  for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
    body="$(deployment_status "$deployment_id")"
    state="$(printf '%s' "$body" | jq -r '.deploymentState')"
    errors="$(printf '%s' "$body" | jq -r '.errors // {} | length')"
    printf 'validation poll=%02d state=%s errors=%s\n' "$attempt" "$state" "$errors"

    case "$state" in
      VALIDATED)
        [[ "$errors" == "0" ]] || die "Central validation reported errors"
        return 0
        ;;
      FAILED | DROPPED)
        print_status "$body" >&2
        die "Central deployment is $state"
        ;;
      PUBLISHED)
        echo "Deployment is already PUBLISHED."
        return 2
        ;;
    esac
    sleep "$POLL_INTERVAL_SECONDS"
  done

  die "Timed out waiting for Central validation"
}

publish_deployment() {
  local deployment_id="$1"
  local assume_yes="${2:-false}"
  local body state errors code

  body="$(deployment_status "$deployment_id")"
  state="$(printf '%s' "$body" | jq -r '.deploymentState')"
  if [[ "$state" == "PUBLISHED" ]]; then
    echo "Deployment is already PUBLISHED."
    return 0
  fi

  if [[ "$state" != "VALIDATED" ]]; then
    wait_for_validation "$deployment_id" || {
      local rc=$?
      if [[ "$rc" == "2" ]]; then
        return 0
      fi
      exit "$rc"
    }
    body="$(deployment_status "$deployment_id")"
    state="$(printf '%s' "$body" | jq -r '.deploymentState')"
  fi

  errors="$(printf '%s' "$body" | jq -r '.errors // {} | length')"
  [[ "$state" == "VALIDATED" && "$errors" == "0" ]] ||
    die "Deployment is not valid for publishing (state=$state, errors=$errors)"

  if [[ "$assume_yes" != "true" ]]; then
    local answer
    read -r -p "Publish deployment $deployment_id to Maven Central? [y/N] " answer
    case "$answer" in
      y | Y | yes | YES) ;;
      *) die "Publish cancelled" ;;
    esac
  fi

  code="$(curl --silent --show-error --fail-with-body --config "$CENTRAL_CONFIG" \
    --request POST --output /dev/null --write-out '%{http_code}' \
    "$CENTRAL_DEPLOYMENT_ENDPOINT/$deployment_id")"
  [[ "$code" == "200" || "$code" == "204" ]] ||
    die "Central publish request failed with HTTP $code"

  local attempt
  for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
    body="$(deployment_status "$deployment_id")"
    state="$(printf '%s' "$body" | jq -r '.deploymentState')"
    printf 'publish poll=%02d state=%s\n' "$attempt" "$state"
    case "$state" in
      PUBLISHED)
        print_status "$body"
        return 0
        ;;
      FAILED | DROPPED)
        print_status "$body" >&2
        die "Central deployment is $state"
        ;;
    esac
    sleep "$POLL_INTERVAL_SECONDS"
  done

  die "Timed out waiting for Central publish"
}

verify_public_artifacts() {
  local version="$1"
  local artifact suffix url code remote_sha local_sha local_aar tmp

  [[ "$version" =~ ^[A-Za-z0-9._-]+$ ]] || die "Invalid version: $version"
  require_command curl
  require_command shasum
  require_command gpg

  VERIFY_TMP="$(mktemp -d "${TMPDIR:-/tmp}/bigimage-verify.XXXXXX")"
  tmp="$VERIFY_TMP"

  for artifact in BigImageViewPager BigImageViewPager-media3; do
    for suffix in pom aar pom.asc pom.sha1; do
      url="$MAVEN_REPOSITORY_BASE/$artifact/$version/$artifact-$version.$suffix"
      code="$(curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' "$url")"
      [[ "$code" == "200" ]] || die "Public artifact returned HTTP $code: $url"
      echo "OK $url"
    done

    curl --silent --show-error --location \
      --output "$tmp/$artifact-$version.aar" \
      "$MAVEN_REPOSITORY_BASE/$artifact/$version/$artifact-$version.aar"
    remote_sha="$(curl --silent --show-error --location \
      "$MAVEN_REPOSITORY_BASE/$artifact/$version/$artifact-$version.aar.sha1" |
      tr -d '[:space:]')"
    local_aar="$PROJECT_ROOT/build/central-publishing/$version/repository/com/gouqinglin/$artifact/$version/$artifact-$version.aar"
    if [[ -f "$local_aar" ]]; then
      local_sha="$(shasum -a 1 "$local_aar" | awk '{print $1}')"
      [[ "$remote_sha" == "$local_sha" ]] ||
        die "SHA-1 mismatch for $artifact (remote=$remote_sha local=$local_sha)"
      echo "SHA-1 OK $artifact $local_sha"
    else
      echo "WARN local bundle artifact not found for $artifact; remote checksum is $remote_sha"
    fi

    curl --silent --show-error --location \
      --output "$tmp/$artifact-$version.pom" \
      "$MAVEN_REPOSITORY_BASE/$artifact/$version/$artifact-$version.pom"
    curl --silent --show-error --location \
      --output "$tmp/$artifact-$version.pom.asc" \
      "$MAVEN_REPOSITORY_BASE/$artifact/$version/$artifact-$version.pom.asc"
    gpg --verify "$tmp/$artifact-$version.pom.asc" "$tmp/$artifact-$version.pom" 2>/dev/null ||
      die "PGP verification failed for $artifact POM"
    echo "PGP OK $artifact"
  done
}

usage() {
  cat <<'USAGE'
Usage:
  ./publish-central.sh status <deployment-id>
  ./publish-central.sh publish <deployment-id> [--yes]
  ./publish-central.sh verify <version>

Environment:
  RELEASE_ENV_FILE  Credentials file, default ~/.config/BigImageViewPager/release.env
  CENTRAL_* or OSSRH_USERNAME/OSSRH_PASSWORD
USAGE
}

main() {
  local command="${1:-}"
  case "$command" in
    status)
      [[ -n "${2:-}" ]] || die "Missing deployment ID"
      load_credentials
      print_status "$(deployment_status "$2")"
      ;;
    publish)
      [[ -n "${2:-}" ]] || die "Missing deployment ID"
      load_credentials
      publish_deployment "$2" "${3:-false}"
      ;;
    verify)
      [[ -n "${2:-}" ]] || die "Missing version"
      verify_public_artifacts "$2"
      ;;
    -h | --help | help)
      usage
      ;;
    *)
      usage >&2
      exit 1
      ;;
  esac
}

main "$@"
