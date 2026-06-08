## 2.0.11 - 2026-06-08

* Require Flutter 3.44+ and Dart 3.4+.
* Convert the Android plugin (and the example app's `MainActivity`) from Java to
  Kotlin and adopt Kotlin Gradle Plugin 2.2.20.
* Modernize the Android build for the v2 embedding: bump the example app to the
  declarative Gradle plugin DSL (`pluginManagement` + `dev.flutter.flutter-gradle-plugin`),
  AGP 8.11.1, Gradle 8.14, compileSdk 35, and Java 11.
* Remove the legacy `package` attribute from Android manifests (now set via the
  Gradle `namespace`) and drop the v1 `io.flutter.app.FlutterApplication` from the
  example app's manifest.
* Remove the deprecated v1 `registerWith` embedding API from the Android plugin.
* Add Swift Package Manager support alongside CocoaPods: restructure the iOS
  plugin into the Flutter SPM source layout with a `Package.swift` manifest, bump
  the iOS deployment target to 12.0, and drop the obsolete `VALID_ARCHS` xcconfig
  that blocked arm64 simulator builds.

## 2.0.10 - 2024-06-09

* Update packages

## 2.0.9 - 2023-10-14

* Update packages

## 2.0.8 - 2023-10-14

* Example update.

## 2.0.7 - 2023-04-28

* Packages and plugins update

## 2.0.6 - 2022-03-16

* Packages and plugins update

## 2.0.5 - 2021-11-26

* Packages and plugins update

## 2.0.4 - 2021-05-31

* Pull request from TheoLassonder https://github.com/TheoLassonder/native_shared_preferences

## 2.0.3 - 2021-05-05

* Static analysis fixes


## 2.0.2 - 2021-05-05

* Static analysis fixes


## 2.0.1 - 2021-05-05

* AndroidX migration


## 2.0.0 - 2021-05-05

* Null-safety migration


## 1.0.5 - 2020-12-16

* Dart format
* Update depencendies

## 1.0.4 - 2020-05-18

* Pull request from mgonzalezc
* Pull request from cznico. Android: User can specify name for shared preferences in string resources.

## 1.0.3 - 2020-03-20

* Android: Remove the flutter prefix

## 1.0.2 - 2020-03-19

* iOS: Map dates saved in user defaults as a double of epoch milliseconds

## 1.0.1 - 2020-03-18

* Format and description

## 1.0.0 - 2020-03-18

* Initial release.
