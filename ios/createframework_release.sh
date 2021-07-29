NEW_VERSION="$1";

rm -rf build/;

xcodebuild archive -scheme MobilePassSDK -configuration Release -destination 'generic/platform=iOS' -archivePath './build/MobilePassSDK.framework-iphoneos.xcarchive' SKIP_INSTALL=NO BUILD_LIBRARY_FOR_DISTRIBUTION=YES;

xcodebuild archive -scheme MobilePassSDK -configuration Release -destination 'generic/platform=iOS Simulator' -archivePath './build/MobilePassSDK.framework-iphonesimulator.xcarchive' SKIP_INSTALL=NO BUILD_LIBRARY_FOR_DISTRIBUTION=YES;

xcodebuild -create-xcframework -framework './build/MobilePassSDK.framework-iphonesimulator.xcarchive/Products/Library/Frameworks/MobilePassSDK.framework' -framework './build/MobilePassSDK.framework-iphoneos.xcarchive/Products/Library/Frameworks/MobilePassSDK.framework' -output './build/MobilePassSDK.xcframework';

cd build && zip -r -X MobilePassSDK.xcframework.$NEW_VERSION.zip MobilePassSDK.xcframework && cd ..;

cd Distribution && rm -rf *.$NEW_VERSION.zip && cd ..;

mv build/MobilePassSDK.xcframework.$NEW_VERSION.zip ./Distribution;

swift package compute-checksum Distribution/MobilePassSDK.xcframework.$NEW_VERSION.zip 