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
            url: "https://github.com/armonlabs/mobilepass/raw/main/ios/Distribution/MobilePassSDK.xcframework.2.2.0.zip",
            checksum: "5889a27f9270023f9de3bb72d0c8c1810770d69d62c46b932b51c60ce37dcdc3"),
    ]
)