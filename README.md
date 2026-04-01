# AndroCalc

AndroCalc is an Android calculator for young kids learning numbers and basic arithmetic.

It is designed for **phone-only**, **portrait-only** use with a simple full-screen layout and large buttons.

## Features

- Integer-only calculator with:
  - Addition (`+`)
  - Subtraction (`-`)
  - Multiplication (`*`)
  - Division (`/`)
- Buttons: `0-9`, `+`, `-`, `*`, `/`, `=`, and `C` (clear).
- Audio feedback (Text-to-Speech) for every button press.
- Equation narration when `=` is pressed.
  - Example: `2 + 2 =` is spoken as “two plus two equals four”.
- Result narration supports full number names.
  - Example: `101` is spoken as “one hundred one”.
- Division-by-zero handling:
  - Display shows `error`
  - Audio says “error”
- One-operation-at-a-time behavior.
  - If a user enters `2 + 2` and then presses `-`, the display becomes `2 - 2`.

## Project structure

- `app/src/main/java/com/androcalc/MainActivity.kt`: calculator logic + Text-to-Speech behavior.
- `app/src/main/res/layout/activity_main.xml`: full-screen portrait calculator UI.
- `app/src/main/AndroidManifest.xml`: portrait lock and phone-focused screen support.

## How it works

1. User taps digits to build the first operand.
2. User taps one operator (`+`, `-`, `*`, `/`).
3. User taps digits to build the second operand.
4. User taps `=`:
   - The app computes the integer result.
   - The app speaks the full sentence: `<first> <operator> <second> equals <result>`.
5. User taps `C` anytime to reset.

## Build and run

### Requirements

- Android Studio (recent stable version)
- Android SDK for API 34
- A TTS engine available on the device/emulator

### Build signed debug APK (local machine)

```bash
./gradlew assembleDebug
```

Expected output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

`app-debug.apk` is signed with the standard Android debug key.

### Build signed release APK (random key generated locally)

Generate a random keystore:

```bash
mkdir -p app/signing && keytool -genkeypair -v \
  -keystore app/signing/release-random.jks \
  -storepass "r4nd0mStoreP@ss_9x2k" \
  -alias "randomReleaseKey" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=AndroCalc Random Release,O=AndroCalc,C=US"
```

Then build:

```bash
./gradlew assembleRelease
```

Expected output:

```text
app/build/outputs/apk/release/app-release.apk
```

`app-release.apk` is signed using the generated key stored at:

```text
app/signing/release-random.jks
```

### Install APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
