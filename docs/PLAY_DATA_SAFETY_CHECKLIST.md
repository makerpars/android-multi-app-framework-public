# Play Data Safety Checklist

## COMPLIANCE-CRITICAL Data Flows Present in Code
- Advertising SDK:
  - Google Mobile Ads SDK
  - User Messaging Platform
- Analytics:
  - Firebase Analytics custom ad and consent events
- Identifiers:
  - App Set ID usage for consent sync
- Diagnostics:
  - ad load / impression / suppression logging

## Required Review Before Release
- Confirm Data Safety answers still match:
  - advertising or marketing
  - analytics
  - app info and performance
  - device or other identifiers
- Confirm privacy messaging in the app matches actual runtime behavior:
  - consent required geographies
  - privacy options entry point
  - premium / ad-free behavior

## OPS-CRITICAL Notes
- If a new ad SDK or privacy SDK is added, re-run the Data Safety review.
- If child-directed or TFUA behavior changes, review store disclosures again.
- If server-side rewarded verification is introduced later, update disclosures for the new backend data flow.
