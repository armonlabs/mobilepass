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
            url: "https://github.com/armonlabs/mobilepass/raw/installationid/ios/Distribution/MobilePassSDK.xcframework.2.0.1-rc.2.zip",
            checksum: "f3cd5f985e8c14a740c0340f767f173b69eb2741eee031e0bfed5e0873e6e0a8"),
    ]
)