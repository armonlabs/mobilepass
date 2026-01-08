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
            url: "https://github.com/armonlabs/mobilepass/raw/non-ui-installationid-release/ios/Distribution/MobilePassSDK.xcframework.2.0.0.zip",
            checksum: "d31db5f1f42ca4b9075c4e23e036e05ebe65e0ed4cb23d6613f6d8c63e18f711"),
    ]
)