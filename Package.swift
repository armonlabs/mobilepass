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
            targets: ["MobilePassSDK"])
    ],
    targets: [
        .binaryTarget(
            name: "MobilePassSDK", 
            url: "https://github.com/armonlabs/mobilepass/raw/main/ios/Distribution/MobilePassSDK.xcframework.0.0.1.zip",
            checksum: "29d6401823bd64c27084eaf5144a866f3f1e0bb77dbe8354b07b611708244d5d"
        ),
    ]
)
