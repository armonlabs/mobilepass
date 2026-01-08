// swift-tools-version:5.3
import PackageDescription
let package = Package(
    name: "MobilePassSDK",
	platforms: [
        .iOS(.v12)
    ],
    products: [
        .library(
            name: "MobilePassSDK",
            targets: ["MobilePassSDK"]),
    ],
    targets: [
        .binaryTarget(
            name: "MobilePassSDK",
            url: "https://github.com/armonlabs/mobilepass/raw/installationid-release/ios/Distribution/MobilePassSDK.xcframework.1.8.0.zip",
            checksum: "18b9fe9e6d7627bba27e66258ac4747698e6530acf57ea2f035815fa2a48d070"),
    ]
)