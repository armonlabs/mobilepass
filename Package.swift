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
            url: "https://github.com/armonlabs/mobilepass/raw/without-ui/ios/Distribution/MobilePassSDK.xcframework.2.0.0-rc.1.zip",
            checksum: "8dad094c92e6d8bbf8cfcb7ce03c2e6363daf33e32c579a5eb8face136cb393e"),
    ]
)