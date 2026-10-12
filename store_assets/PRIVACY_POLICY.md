# AutoScroll — Privacy Policy

**Effective Date:** October 8, 2026  
**Developer:** Govind Tank (droidtank)  
**Contact:** govindtank600@gmail.com  

## 1. Overview
AutoScroll ("the App") is an Android utility designed to provide automated screen scrolling and gesture macros. We respect your privacy and are committed to protecting your personal data.

## 2. Accessibility Service Usage & Prominent Disclosure
AutoScroll utilizes Android's `AccessibilityService` API strictly to deliver its core functionality:
- **Gesture Dispatching**: To perform automated touch swipes, page turns, and scroll actions on your screen according to your customized speed settings.
- **Custom Gesture Playback**: To replay user-recorded touch sequences and macro gestures on the screen.
- **Foreground App Detection**: To detect which application is currently active in the foreground in order to automatically apply your configured per-app scrolling profiles.
- **Hardware Volume Key Controls**: To intercept volume button presses while scrolling to conveniently pause or resume scrolling hands-free.

### Prominent In-App Disclosure & Consent:
Before requesting the `BIND_ACCESSIBILITY_SERVICE` permission or directing users to Android System Settings, the App presents a clear, unambiguous prominent disclosure modal detailing all features and privacy protections. User consent requires affirmative action via explicit "Accept & Continue" or "Decline" options. Navigating away or dismissing the dialog is never interpreted as consent.

### Strict Privacy Guarantee:
- The Accessibility Service **does NOT** read, intercept, monitor, or record your screen content, typed text, credentials, messages, keystrokes, or personal data.
- The service acts strictly as a local touch dispatcher and runs 100% offline. Zero data is transmitted to external servers.

## 3. Data Collection & Sharing
- **No Personal Data Collected**: The App does not collect, store, transmit, or sell any personally identifiable information (PII).
- **Offline & Local-First**: All your preferences, custom gesture recordings, and per-app settings are stored exclusively on your device using local SQLite/Room database storage.
- **No Third-Party Trackers**: We do not use third-party analytics SDKs, advertising trackers, or telemetry brokers that collect user identity.

## 4. Permissions
- `BIND_ACCESSIBILITY_SERVICE`: To dispatch automated scroll gestures.
- `SYSTEM_ALERT_WINDOW` (Display over other apps): To render the compact floating control overlay.
- `RECEIVE_BOOT_COMPLETED`: To restore background profile listener upon device restart (if enabled).

## 5. Changes to This Policy
We may update this Privacy Policy periodically. Any updates will be reflected on this page with a revised effective date.

## 6. Contact Us
If you have any questions or feedback regarding this Privacy Policy, please contact us at: **govindtank600@gmail.com**.
