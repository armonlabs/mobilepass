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
            url: "https://github.com/armonlabs/mobilepass/raw/without-ui/ios/Distribution/MobilePassSDK.xcframework.2.0.1-rc.1.zip",
            checksum: "8ecc84aa3904f7e3acd1611e2f0b869720fc747e2a2ca111765a0fed3cd23e22"),
    ]
)