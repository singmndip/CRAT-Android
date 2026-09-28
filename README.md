# CRAT Android

This package wraps the CRAT kid-video UI in a native Android WebView and implements the two YouTube proxy endpoints inside Android itself. The installed app therefore does **not** need to run the Express server on the phone.

## Android identity

- App name: **CRAT**
- Application ID: `com.crat.kidsstreaming`
- Min SDK: 24
- Target SDK: 35
- Version: 1.0.0

## Build an APK with the Android SDK

Install Android platform 35 and Build Tools 35.0.0, set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`), then run:

```bash
./build-apk.sh
```

The signed development APK is written to:

```text
out/CRAT.apk
```

The included `.github/workflows/build-apk.yml` can also build the APK on GitHub Actions.

## Source context

`web/package.json` contains the package metadata supplied for the CRAT web app, and `web/src/types.ts` contains the supplied CRAT TypeScript data model.

## Content safety note

The YouTube search path keeps the original keyword/title filtering approach. It is not a substitute for YouTube Kids controls or adult supervision.
