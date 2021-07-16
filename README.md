# Armon MobilePass Library

This library has been developed to allow Android and iOS apps to be used for passing from compatible **armon** access points. Users can pass from these points securely by using application that installed on their phones

When the user arrives at the point, the flow will be as follows;

- The user opens the application and starts flow according to the triggering method provided by the application, like tapping a button.
- The application gives user's information to the library. This information will be used to validate user during communication
- Library opens the phone camera to read the QR code of the access point
- If the QR code is valid, according to pass method of access point, library starts communication with device or server. These methods can be;

  - Remote Access
    - Library communicates with server and server validates user. If user has access to pass, related device will be triggerred by server
  - Bluetooth
    - Library starts scanning of related device via Bluetooth Low Energy. If device is in range, library communicates with device in encrypted protocol. Device validates user and opens door, turnstile, barrier, etc.
  - Remote Access First, Then Bluetooth
    - Library tries remote access flow first. If this flow fails, bluetooth communication will be started
  - Bluetooth First, Then Remote Access
    - Library tries bluetooth flow first. If this flow fails, bluetooth communication will be started

- Remote Access method can validates user's location also. If the method is defined with geolocation validation requirement, library checks if the user is at the specified location before remote access request
- After flow completed, returns the result to the application.

<br />

## Content

- [Android](#android)
- [iOS](#ios)
- [API Reference](#api-reference)

<br />

## **Android**

### Installation

- Add the JitPack repository to your root build.gradle at the end of repositories

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

- Add the depedency to app level build.gradle

```
dependencies {
	implementation 'com.github.armonlabs:mobilepass:1.1.1'
}
```

- Change your application's min sdk version with `21` if required

- Specify your Google Maps API Key. This will be required to show user's and access points' location on map if QR code definition has geolocation validation requirement. Add your API key to your manifest file (android/app/src/main/AndroidManifest.xml)

```
<application>
   <!-- Make sure it's a child of application -->
   <meta-data
     android:name="com.google.android.geo.API_KEY"
     android:value="[Your Google maps API Key Here]"/>
</application>
```

For more information about Google Maps API Key; https://developers.google.com/maps/documentation/android-api/signup

<br />

### Permissions

- Library needs some permissions; camera, fine location and bluetooth. To continue flow without interrupt, you should request permissions from user. Otherwise flow will be cancelled with related reason code.

- Camera permission is required to read QR codes at the start of flow.

- Location permission is required for geolocation validation and Bluetooth scanning

- Also Bluetooth should be enabled on phone

<br />

### Usage

- First you should initialize `MobilePass` like below

```
import com.armongate.mobilepasssdk.MobilePass;
import com.armongate.mobilepasssdk.model.Configuration;

...

// Setup configuration, see API reference for more details
Configuration config = new Configuration();

// Initialize manager
MobilePass passManager = new MobilePass(getApplicationContext(), config);
```

- To handle events of MobilePass, implement related Delegate class like below

```
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;

public class MainActivity extends AppCompatActivity implements MobilePassDelegate {

	...

	// ! Important
	// Call next line where you initialized manager
	// passManager.setDelegate(this);


	@Override
	public void onPassCancelled(int reason) {
		// See API reference for CancelReason types
	}

	@Override
	public void onPassCompleted(boolean succeed) {
		// Returns when library-server or library-device communication ended
		// 'true' means user passed successfully
	}

	@Override
	public void onQRCodeListStateChanged(int state) {
		// Available QR code definitions will be fetched from server.
		// Change on state of definitions triggers this event
		// See API reference for QRCodeState types
	}

	...

}
```

- If you have not provided user token before in configuration model or token value has changed after initializing, you can call `updateToken` method like below

```
passManager.updateToken("${newTokenValue}", "${language}");

// Langugage > "tr" | "en"
```

- After initialization you can start flow like below. You need to provide token before this step.
  <br/>

```
passManager.triggerQRCodeRead();
```

<br />

## **iOS**

### Installation

- Add swift packagege dependency by using File > Swift Packages > Add Package Dependency

```
https://github.com/armonlabs/mobilepass.git
```

- Make sure that your target is selected and General tab is open and package is added to `Frameworks, Libraries, and Embedded Content` section

- Navigate to the Build Phases tab and make sure your framework is included in the `Link Binary With Libraries` list. It should already be included by default after following the steps above, however in case it’s not; click on the + button and add it.

<br />

### Permissions

- Library needs some permissions; camera, location and bluetooth. To continue flow without interrupt, you should request permissions from user. Otherwise flow will be cancelled with related reason code. You must update Info.plist with a usage description for these requirements;

```
...

<key>NSCameraUsageDescription</key>
<string>Your own description of the purpose</string>

<key>NSLocationWhenInUseUsageDescription</key>
<string>Your own description of the purpose</string>

<key>NSBluetoothAlwaysUsageDescription</key>
<string>Your own description of the purpose</string>

...
```

- Camera permission is required to read QR codes at the start of flow.

- Location permission is required for geolocation validation

- Also Bluetooth should be enabled on phone

<br />

### Usage

- Examples are given in SwiftUI sample

- Create MobilePass manager class to bridge with your UI

```
struct MobilePassManager : UIViewControllerRepresentable {
    typealias UIViewControllerType = UIViewController

    func makeCoordinator() -> MobilePassManager.Coordinator {
        Coordinator(self)
    }

    func makeUIViewController(context: UIViewControllerRepresentableContext<MobilePassManager>) -> UIViewController {
		// Setup configuration, see API reference for more details
		let config: Configuration = Configuration(...)

		// Initialize manager
        let passManager: MobilePass = MobilePass(config: config)

		// Call trigger method that returns ViewController
        return passManager.triggerQRCodeRead()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<MobilePassManager>) {

    }

    class Coordinator : NSObject {
        var parent : MobilePassManager
        init(_ viewController : MobilePassManager){
            self.parent = viewController
        }
    }
}
```

- To handle events of MobilePass, implement related Delegate class like below

```
struct MobilePassManager : UIViewControllerRepresentable, MobilePassDelegate {

	// ! Important
	// Call next line where you initialized manager
	// passManager.delegate = self

	...

    func onPassCancelled(reason: Int) {
        // See API reference for CancelReason types
    }

    func onPassCompleted(succeed: Bool) {
        // Returns when library-server or library-device communication ended
		// 'true' means user passed successfully
    }

    func onQRCodeListStateChanged(state: Int) {
        // Available QR code definitions will be fetched from server.
		// Change on state of definitions triggers this event
		// See API reference for QRCodeState types
    }

	...

}
```

- If you have not provided user token before in configuration model or token value has changed after initializing, you can call `updateToken` method like below. Token must be given before trigger the QR code reading.

```

// Langugage > "tr" | "en"
passManager.updateToken(token: "${newTokenValue}", language: "${language}");

```

<br />
<br />
<br />

## **API Reference**

### **Methods**

### `updateToken(token, language)`

Update or set OAuth token of user and current language code for localization

| Parameter | Description                                                                  |
| --------- | ---------------------------------------------------------------------------- |
| token     | OAuth token value that will be used by server to validate user with memberId |
| language  | Language code to localize messages in library [tr/en]                        |

<br/>

### `triggerQRCodeRead()`

Triggers QR Code reading and related pass flow.

<br />
<br/>

### **Objects**

<br/>

### `Configuration`

Library is configurable while initialization. Available props are listed below

| Parameter         | Description                                                                                                                                                       | Type    | Required |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- | -------- |
| memberId          | Member id that will be used for validation to pass                                                                                                                | String  | Yes      |
| serverUrl         | URL of server that communicate between SDK, devices and validation server                                                                                         | String  | Yes      |
| qrCodeMessage     | Information message for QR Code reader that will be shown at top of screen                                                                                        | String  | No       |
| token             | OAuth token value of current user's session to validate                                                                                                           | String  | No       |
| language          | Language code to localize texts                                                                                                                                   | String  | No       |
| allowMockLocation | Allow unreliable locations or not                                                                                                                                 | String  | No       |
| connectionTimeout | Bluetooth connection timeout in seconds. **Default: 5 seconds**                                                                                                   | Integer | No       |
| autoCloseTimeout  | Auto close timeout for screen after pass completed, null means stay opened                                                                                        | Integer | No       |
| waitBLEEnabled    | Flag to decide action for disabled Bluetooth state. "true" means wait user to enable Bluetooth, "false" means continue to next step if exists. **Default: false** | Bool    | No       |

<br />
<br />

### **Events**

| Name                      | Description                                         | Type                          |
| ------------------------- | --------------------------------------------------- | ----------------------------- |
| onPassCompleted           | Passing operation completed                         | boolean                       |
| onPassCancelled           | Operation has been cancelled with given reason code | [CancelReason](#CancelReason) |
| onQRCodeListStateChanged  | QR code definition list updated                     | [QRCodeState](QRCodeState)    |

<br />
<br />

### **Constants**

<br />

### `CancelReason`

| Name                          | Value |
| ----------------------------- | ----- |
| Need Permission for Camera    | 1     |
| Need Permission for Location  | 2     |
| User Closed                   | 3     |
| Need Enable Bluetooth         | 4     |
| Need Enable Location Services | 5     |
| Using Mock Location           | 6     |
| Error                         | 7     |
| Need Permission Bluetooth     | 8     |
| Auto Close                    | 9     |

<br />

### `QRCodeState`

| Name              | Value |
| ----------------- | ----- |
| Empty             | 1     |
| Using Stored Data | 2     |
| Using Synced Data | 3     |
