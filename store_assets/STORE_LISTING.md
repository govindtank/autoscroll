# AutoScroll Pro — Google Play Store Listing & Compliance

## 📱 Store Listing Metadata

### App Details
- **App Title (Max 30 chars)**: `AutoScroll: Smart Hands-Free`
- **Short Description (Max 80 chars)**: `Hands-free automated scrolling & custom gesture controls for books, reels & web.`
- **Category**: Productivity / Tools
- **Content Rating**: Everyone

### Full Description (Max 4000 chars)
Enjoy smooth, hands-free reading and effortless browsing with **AutoScroll Pro**! Whether you are reading long eBooks, articles, documents, or watching video feeds, AutoScroll takes over manual swiping with precision speed and natural easing.

🚀 **Key Features**:
• **Continuous Hands-Free Scrolling**: Choose vertical or horizontal scrolling with customizable velocity.
• **Per-App Profiles**: Automatically trigger customized scroll speeds and floating widgets when your designated apps (e.g. Chrome, Kindle, YouTube Shorts) open.
• **Custom Gesture Recording**: Record complex swipe paths and playback multi-point touch macros.
• **Floating Quick Control Hub**: Compact translucent overlay to play, pause, reverse, or speed up scrolling without leaving your app.
• **Battery & OLED Friendly**: Ultra-low background CPU impact with dark mode optimization.
• **Zero Latency Engine**: Native Android Accessibility Service implementation with smooth Bezier curve deceleration.

🔒 **Accessibility Service Disclosure & Privacy**:
AutoScroll Pro requires the Android `AccessibilityService` API permission strictly to:
1. Dispatch automated touch gestures (vertical and horizontal screen swipes/scrolls) hands-free according to your speed settings.
2. Replay user-recorded gesture macros and custom touch sequences across applications.
3. Detect foreground app changes to automatically display quick controls and apply per-app scroll profiles.
4. Intercept hardware volume button presses to pause or resume scrolling without opening the app.

**Strict Privacy & Data Safety Guarantee**:
• Zero Data Collection: We NEVER read, inspect, log, store, or transmit any typed text, passwords, messages, contacts, or screen content.
• 100% Offline: The Accessibility Service operates exclusively on your device as a local touch dispatcher. No data is collected or shared with third parties or sent over the internet.

---

## 📋 Google Play Console Declaration Form Responses
When filling out the **Policy -> App content -> Accessibility Services** declaration in Play Console:

- **Why does your app need the Accessibility API?**
  Select: `App functionality / Automation`
- **Detailed justification:**
  `AutoScroll Pro uses the AccessibilityService API solely to perform automated touch gestures (vertical and horizontal screen swipes) on behalf of the user for hands-free reading, replay user-created custom gestures, and detect active foreground applications to apply per-app scrolling profiles. The app does NOT read, collect, store, or transmit any typed text, credentials, messages, personal data, or screen content. All operations execute strictly offline and locally on the device.`
- **Two-Button Consent & Prominent Disclosure Confirmation:**
  The app presents a prominent disclosure dialog before directing the user to system settings, requires explicit affirmative consent via two clear buttons ("Accept & Continue" and "Decline"), and never treats dismissal or back navigation as consent.

---

## 🎨 Verified Visual Assets Inventory
All visual assets are 100% genuine captures directly from physical testing devices (no synthetic or mock images):

1. **App Icon**: `store_assets/icon_512.png` (512 x 512 px PNG)
2. **Feature Graphic**: `store_assets/feature_graphic_1024x500.png` (1024 x 500 px PNG)
3. **Real Device Screenshots (1080 x 2340 px)**:
   - `screenshot_1_home.png`: Home dashboard & scroll engine controls
   - `screenshot_2_disclosure.png`: In-app prominent disclosure dialog with two-button explicit consent
   - `screenshot_3_apps.png`: Per-app automation configuration & installed apps list
   - `screenshot_4_gestures.png`: Gesture cloning & custom swipe macro recorder
   - `screenshot_5_settings.png`: Settings with real-time accessibility status & Light/Dark appearance selector

4. **Compliance Demonstration Video**:
   - `store_assets/accessibility_demo.mp4` (and `store_assets/demo.mp4`): 55-second 1080x2340 60fps real device demonstration video showing clean install, in-flow disclosure modal with two action buttons, system settings delegation, service activation, returning to engine ready, feature walkthrough, and Light/Dark mode switching.
