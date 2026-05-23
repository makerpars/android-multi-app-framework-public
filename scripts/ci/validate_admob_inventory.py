#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APPS_JSON = ROOT / ".ci" / "apps.json"
APP_DIR = ROOT / "app" / "src"
DEBUG_ADS = APP_DIR / "debug" / "res" / "values" / "ads.xml"
GOOGLE_TEST_PREFIX = "ca-app-pub-3940256099942544"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate flavor AdMob inventory against .ci/apps.json and ads.xml",
    )
    parser.add_argument("--mode", choices=["warn", "strict"], default="strict")
    parser.add_argument("--target-flavors", default="all")
    return parser.parse_args()


def load_json(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8") as fh:
        return json.load(fh)


def parse_string_resources(path: Path) -> dict[str, str]:
    tree = ET.parse(path)
    root = tree.getroot()
    resources: dict[str, str] = {}
    for child in root.findall("string"):
        name = child.attrib.get("name")
        if not name:
            continue
        resources[name] = (child.text or "").strip()
    return resources


def normalize_targets(raw: str, known: set[str]) -> list[str]:
    if raw == "all":
        return sorted(known)
    return [item.strip() for item in raw.split(",") if item.strip()]


def check_debug_ads(errors: list[str], warnings: list[str]) -> None:
    if not DEBUG_ADS.exists():
        errors.append(f"missing debug ads.xml: {DEBUG_ADS.relative_to(ROOT)}")
        return

    resources = parse_string_resources(DEBUG_ADS)
    required = (
        "admob_app_id",
        "ad_unit_banner",
        "ad_unit_interstitial",
        "ad_unit_native",
        "ad_unit_rewarded",
        "ad_unit_open_app",
    )
    for key in required:
        value = resources.get(key, "")
        if not value:
            errors.append(f"debug ads.xml missing {key}")
        elif not value.startswith(GOOGLE_TEST_PREFIX):
            errors.append(f"debug ads.xml {key} is not a Google test id")

    for obsolete in ("ad_unit_interstitial_audio_stop", "ad_unit_interstitial_content_mode_switch"):
        if obsolete in resources:
            warnings.append(f"debug ads.xml still defines obsolete placement {obsolete}")


def expected_mappings(app: dict) -> dict[str, str]:
    units = app["ad_units"]
    rewarded = units["rewarded"]
    rewarded_interstitial = units.get("rewarded_interstitial", rewarded)
    return {
        "admob_app_id": app["admob_app_id"],
        "ad_unit_banner": units["banner"],
        "ad_unit_interstitial": units["interstitial"],
        "ad_unit_native": units["native"],
        "ad_unit_rewarded": rewarded,
        "ad_unit_open_app": units["open_app"],
        "ad_unit_rewarded_interstitial": rewarded_interstitial,
        "ad_unit_banner_home": units["banner"],
        "ad_unit_banner_settings": units["banner"],
        "ad_unit_banner_content_list": units["banner"],
        "ad_unit_banner_content_detail": units["banner"],
        "ad_unit_banner_qibla": units["banner"],
        "ad_unit_banner_zikir": units["banner"],
        "ad_unit_native_feed_home": units["native"],
        "ad_unit_native_feed_content": units["native"],
        "ad_unit_native_feed_zikir": units["native"],
        "ad_unit_interstitial_nav_break": units["interstitial"],
        "ad_unit_open_app_resume": units["open_app"],
        "ad_unit_rewarded_rewards_screen": rewarded,
        "ad_unit_rewarded_interstitial_history_unlock": rewarded_interstitial,
    }


def main() -> int:
    args = parse_args()
    apps = load_json(APPS_JSON)
    catalog = {entry["flavor"]: entry for entry in apps}
    errors: list[str] = []
    warnings: list[str] = []

    targets = normalize_targets(args.target_flavors, set(catalog))
    for flavor in targets:
        app = catalog.get(flavor)
        if app is None:
            errors.append(f"flavor {flavor} missing from .ci/apps.json")
            continue

        ads_xml = APP_DIR / flavor / "res" / "values" / "ads.xml"
        if not ads_xml.exists():
            errors.append(f"flavor {flavor} missing ads.xml: {ads_xml.relative_to(ROOT)}")
            continue

        resources = parse_string_resources(ads_xml)
        expected = expected_mappings(app)
        for key, expected_value in expected.items():
            actual = resources.get(key, "")
            if not actual:
                errors.append(f"{flavor}: missing {key}")
                continue
            if actual != expected_value:
                errors.append(f"{flavor}: {key} mismatch with .ci/apps.json")

        for obsolete in ("ad_unit_interstitial_audio_stop", "ad_unit_interstitial_content_mode_switch"):
            if obsolete in resources:
                warnings.append(f"{flavor}: obsolete placement resource still present: {obsolete}")

    check_debug_ads(errors, warnings)

    for warning in warnings:
        print(f"WARN: {warning}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        if args.mode == "strict":
            return 1

    if not errors:
        print(f"OK: validated AdMob inventory for {len(targets)} flavor(s)")
    else:
        print(f"WARN: validated with {len(errors)} error(s) in warn mode")
    return 0


if __name__ == "__main__":
    sys.exit(main())
