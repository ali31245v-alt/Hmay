# Strict Locker Safe (Kotlin)

A minimal Android Studio project for a demo in-app PIN screen.

## Safety scope
This project intentionally does **not** use Accessibility to monitor Settings, block uninstall/force-stop, hide itself, or resist removal. Android device owners remain able to control and uninstall the app. The PIN (1234) is a demo only and is not device-level security.

## Build
The GitHub Actions workflow builds a debug APK when files are pushed to `main`, or can be run manually from the Actions tab.

After the workflow succeeds, open its run and download the `strict-locker-debug-apk` artifact.

For local builds, open the repository in Android Studio, sync Gradle, then choose Build > Build Bundle(s) / APK(s) > Build APK(s).
