# CRATT Android

This package wraps the CRATT kid-video UI in a native Android WebView and implements the two YouTube proxy endpoints inside Android itself. The installed app therefore does **not** need to run the Express server on the phone.

## Android identity

- App name: **CRATT**
- Application ID: `com.cratt.kidsstreaming`
- Min SDK: 24
- Target SDK: 35
- Version: 1.0.0

## Build

GitHub Actions builds a development APK named `CRATT.apk`.

## Source context

`web/package.json` contains the package metadata supplied for the CRATT web app, and `web/src/types.ts` contains the supplied CRATT TypeScript data model.

## Content safety note

The YouTube search path keeps the original keyword/title filtering approach. It is not a substitute for YouTube Kids controls or adult supervision.
