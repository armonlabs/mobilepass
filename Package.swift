// swift-tools-version:5.3
import PackageDescription
let package = Package(
    name: "MobilePassSDK",
	platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "MobilePassSDK",
            targets: ["MobilePassSDK"]),
    ],
    targets: [
        .binaryTarget(
            name: "MobilePassSDK",
            url: "https://github.com/armonlabs/mobilepass/raw/main/ios/Distribution/MobilePassSDK.xcframework.1.2.0.zip",
            checksum: "8b630fdb20e46e5459eb0cfed92dee7e340084ffa3a9f3d1de1decd9dbf4bd24"),
    ]
)