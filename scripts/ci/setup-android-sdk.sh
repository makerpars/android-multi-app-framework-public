#!/usr/bin/env bash
set -euo pipefail

# Bootstraps Android SDK on Linux CI agents.
# Prefers the preinstalled SDK if present; falls back to downloading cmdline-tools.
#
# Inputs (env):
# - ANDROID_SDK_ROOT: where to install/locate the SDK (default: $HOME/android-sdk)
# - ANDROID_API_LEVEL: compileSdk API level to install (default: 36)
# - ANDROID_BUILD_TOOLS_VERSION: optional exact build-tools version (e.g. 36.0.0)

SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
API_LEVEL="${ANDROID_API_LEVEL:-36}"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"

detect_java_major() {
  local v
  v="$(java -version 2>&1 | head -n 1 || true)"
  # Examples:
  # openjdk version "21.0.2" 2024-01-16
  # openjdk version "17.0.10" 2024-01-16
  echo "$v" | sed -n 's/.*version \"\([0-9]\+\).*/\1/p'
}

ensure_java_21() {
  local major
  major="$(detect_java_major)"
  if [[ -n "$major" && "$major" -ge 21 ]]; then
    return 0
  fi

  echo "Java 21 not detected (found: ${major:-unknown}). Installing OpenJDK 21..."
  run_pkg_manager apt-get update -y
  run_pkg_manager apt-get install -y openjdk-21-jdk
  java -version
}

ensure_sdkmanager_on_path() {
  if command -v sdkmanager >/dev/null 2>&1; then
    return 0
  fi

  # Prefer the preinstalled Android SDK on hosted agents when available.
  # (GitHub-hosted Ubuntu images typically keep it under /usr/local/lib/android/sdk.)
  local candidates=(
    "/usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager"
    "/usr/local/lib/android/sdk/cmdline-tools/bin/sdkmanager"
  )

  local p
  for p in "${candidates[@]}"; do
    if [[ -x "$p" ]]; then
      export PATH="$(dirname "$p"):$PATH"
      return 0
    fi
  done

  return 1
}

bootstrap_cmdline_tools() {
  mkdir -p "$ANDROID_SDK_ROOT"

  # Install prerequisites
  if ! command -v unzip >/dev/null 2>&1; then
    run_pkg_manager apt-get update -y
    run_pkg_manager apt-get install -y unzip
  fi
  if ! command -v curl >/dev/null 2>&1; then
    run_pkg_manager apt-get update -y
    run_pkg_manager apt-get install -y curl
  fi

  local tmp
  tmp="$(mktemp -d)"
  trap "rm -rf '$tmp'" EXIT

  # NOTE: Hosted agents usually already have an SDK. This is a fallback.
  local url="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  echo "Downloading Android cmdline-tools (fallback) ..."
  curl -fsSL "$url" -o "$tmp/cmdline-tools.zip"

  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp/unzipped"

  # The zip contains "cmdline-tools/". We want $SDK/cmdline-tools/latest/...
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$tmp/unzipped/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"

  export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
}

pick_build_tools_version() {
  if [[ -n "${ANDROID_BUILD_TOOLS_VERSION:-}" ]]; then
    echo "$ANDROID_BUILD_TOOLS_VERSION"
    return 0
  fi

  # Prefer build-tools matching API level, otherwise latest available.
  local list out major_target
  major_target="$API_LEVEL"

  # sdkmanager --list is noisy; keep parsing resilient.
  list="$(sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --list 2>/dev/null || true)"
  out="$(echo "$list" | sed -n 's/^build-tools;\([0-9.]*\)[[:space:]].*/\1/p' | sort -V | uniq || true)"
  if [[ -z "$out" ]]; then
    echo ""
    return 0
  fi

  local best
  best="$(echo "$out" | awk -v m="$major_target" -F. '$1==m {print}' | tail -n 1 || true)"
  if [[ -n "$best" ]]; then
    echo "$best"
    return 0
  fi

  echo "$out" | tail -n 1
}

pick_installed_build_tools_version() {
  if [[ -n "${ANDROID_BUILD_TOOLS_VERSION:-}" ]]; then
    echo "$ANDROID_BUILD_TOOLS_VERSION"
    return 0
  fi

  if [[ ! -d "$ANDROID_SDK_ROOT/build-tools" ]]; then
    echo ""
    return 0
  fi

  local installed
  installed="$(find "$ANDROID_SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d -printf "%f\n" 2>/dev/null | sort -V || true)"
  if [[ -z "$installed" ]]; then
    echo ""
    return 0
  fi

  local best
  best="$(echo "$installed" | awk -F. -v m="$API_LEVEL" '$1==m {print}' | tail -n 1 || true)"
  if [[ -n "$best" ]]; then
    echo "$best"
    return 0
  fi

  echo "$installed" | tail -n 1
}

is_sdk_ready() {
  local build_tools="$1"

  local has_platform_tools=false
  local has_platform=false
  local has_build_tools=true

  [[ -x "$ANDROID_SDK_ROOT/platform-tools/adb" ]] && has_platform_tools=true
  [[ -f "$ANDROID_SDK_ROOT/platforms/android-${API_LEVEL}/android.jar" ]] && has_platform=true

  if [[ -n "$build_tools" ]]; then
    [[ -d "$ANDROID_SDK_ROOT/build-tools/${build_tools}" ]] || has_build_tools=false
  fi

  if [[ "$has_platform_tools" == "true" && "$has_platform" == "true" && "$has_build_tools" == "true" ]]; then
    return 0
  fi
  return 1
}

main() {
  if ! command -v apt-get >/dev/null 2>&1; then
    echo "apt-get is not available on this agent. Ensure Java 21, curl, unzip and Android SDK cmdline-tools are preinstalled."
  fi

  ensure_java_21

  if ! ensure_sdkmanager_on_path; then
    bootstrap_cmdline_tools
  fi

  echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
  echo "sdkmanager=$(command -v sdkmanager || true)"

  local build_tools
  build_tools="$(pick_installed_build_tools_version)"

  if is_sdk_ready "$build_tools"; then
    echo "Android SDK already ready. Skipping sdkmanager install."
    echo "Using build-tools: ${build_tools:-auto}"
    return 0
  fi

  if [[ -z "$build_tools" ]]; then
    build_tools="$(pick_build_tools_version)"
  fi

  echo "Installing Android SDK packages: api=$API_LEVEL build-tools=${build_tools:-auto}"

  # With `set -o pipefail`, `yes | sdkmanager --licenses` can return 141
  # when sdkmanager closes stdin early after accepting licenses.
  # Run this one command without pipefail and preserve sdkmanager's status.
  set +o pipefail
  yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null
  license_status=$?
  set -o pipefail
  if [[ $license_status -ne 0 ]]; then
    echo "Failed to accept Android SDK licenses (exit=$license_status)."
    exit $license_status
  fi

  # Always ensure platform-tools and the platform for compileSdk exist.
  if [[ -n "$build_tools" ]]; then
    sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
      "platform-tools" \
      "platforms;android-${API_LEVEL}" \
      "build-tools;${build_tools}" >/dev/null
  else
    sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
      "platform-tools" \
      "platforms;android-${API_LEVEL}" >/dev/null
  fi

  echo "Android SDK ready."
}

run_pkg_manager() {
  if command -v sudo >/dev/null 2>&1; then
    sudo "$@"
    return 0
  fi

  if [ "$(id -u)" = "0" ]; then
    "$@"
    return 0
  fi

  echo "Cannot run package manager command without sudo/root: $*"
  exit 1
}

main "$@"
