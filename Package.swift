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
            url: "https://github.com/armonlabs/mobilepass/raw/main/ios/Distribution/MobilePassSDK.xcframework.0.0.3.zip",
            checksum: "5db099f922c2a08103e452110c1a2778f0434d83dc74bb7fb5595a1b008fc8df"),
    ]
)