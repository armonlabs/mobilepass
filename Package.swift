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
            url: "https://github.com/armonlabs/mobilepass/raw/main/ios/Distribution/MobilePassSDK.xcframework.1.7.6.zip",
            checksum: "e4374b31c139d647f06baafd5736419a2ff236365ad7e6ae858a943466683e95"),
    ]
)