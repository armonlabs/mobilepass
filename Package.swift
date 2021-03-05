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
            path: "ios/Distribution/MobilePassSDK.xcframework")
    ]
)
