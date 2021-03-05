// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "MobilePass",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "MobilePass", 
            targets: ["MobilePass"])
    ],
    targets: [
        .binaryTarget(
            name: "MobilePassSDK", 
            path: "./ios/Distribution/MobilePassSDK.xcframework")
    ]
)
