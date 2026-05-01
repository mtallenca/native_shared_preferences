// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
  name: "native_shared_preferences",
  platforms: [
    .iOS("12.0"),
  ],
  products: [
    .library(name: "native-shared-preferences", targets: ["native_shared_preferences"]),
  ],
  dependencies: [],
  targets: [
    .target(
      name: "native_shared_preferences",
      dependencies: [],
      resources: [],
      publicHeadersPath: "include/native_shared_preferences"
    ),
  ]
)
