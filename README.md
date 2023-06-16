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
	implementation 'com.github.armonlabs:mobilepass:1.5.0'
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
	// Call next line where you initialized manager or give delegate instance in configuration
	// passManager.setDelegate(this);

    @Override
    public void onLogReceived(LogItem log) {
      // To follow SDK logs according to given configuration about logging
    }

    @Override
    public void onInvalidQRCode(String content) {
      // Informs about invalid qr code found while scanning
      // to process this content outside of SDK
    }

    @Override
    public void onMemberIdChanged() {
      // Stored member id changed or new member id received to SDK
    }

    @Override
    public void onSyncMemberIdCompleted() {
      // Share member id and related data to server has been completed successfully
    }

    @Override
    public void onSyncMemberIdFailed(int statusCode) {
      // Share member id and related data to server has been failed!
      // This may cause the pass flow to fail
    }

    @Override
    public void onQRCodesDataLoaded(int count) {
      // Stored QR Code data is ready but not synced yet
    }

    @Override
    public void onQRCodesSyncStarted() {
      // Started to sync with server for up-to-date QR Code list
    }

    @Override
    public void onQRCodesSyncFailed(int statusCode) {
      // Sync of up-to-date QR Code list failed
    }

    @Override
    public void onQRCodesReady(boolean synced, int count) {
      // Returns when sync of QR Code list completed
      // If "synced" value is true, up-to-date list will be used.
      // Otherwise stored list is active for pass flow
    }

    @Override
    public void onQRCodesEmpty() {
      // Sync QR Code list with server has been failed and there is no stored QR Code list yet
    }

    @Override
    public void onScanFlowCompleted(PassFlowResult result) {
      // Returns when each pass flow completed after QR Code scanning started
      // See API reference for PassFlowResult types
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
	// Call next line where you initialized manager or give delegate instance in configuration
	// passManager.delegate = self

	...

    func onLogReceived(log: LogItem) {
        // To follow SDK logs according to given configuration about logging
    }

    func onInvalidQRCode(content: String) {
        // Informs about invalid qr code found while scanning
        // to process this content outside of SDK
    }

    func onMemberIdChanged() {
        // Stored member id changed or new member id received to SDK
    }

    func onSyncMemberIdCompleted() {
        // Share member id and related data to server has been completed successfully
    }

    func onSyncMemberIdFailed(statusCode: Int) {
        // Share member id and related data to server has been failed!
        // This may cause the pass flow to fail
    }

    func onQRCodesDataLoaded(count: Int) {
        // Stored QR Code data is ready but not synced yet
    }

    func onQRCodesSyncStarted() {
        // Started to sync with server for up-to-date QR Code list
    }

    func onQRCodesSyncFailed(statusCode: Int) {
        // Sync of up-to-date QR Code list failed
    }

    func onQRCodesReady(synced: Bool, count: Int) {
        // Returns when sync of QR Code list completed
        // If "synced" value is true, up-to-date list will be used.
        // Otherwise stored list is active for pass flow
    }

    func onQRCodesEmpty() {
        // Sync QR Code list with server has been failed and there is no stored QR Code list yet
    }

    func onScanFlowCompleted(result: MobilePassSDK.PassFlowResult) {
        // Returns when each pass flow completed after QR Code scanning started
        // See API reference for PassFlowResult types
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

| Parameter         | Description                                                                                                                                                       | Type                  | Required |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- | -------- |
| memberId          | Member id that will be used for validation to pass                                                                                                                | String                | Yes      |
| serverUrl         | URL of server that communicate between SDK, devices and validation server                                                                                         | String                | Yes      |
| qrCodeMessage     | Information message for QR Code reader that will be shown at top of screen                                                                                        | String                | No       |
| token             | OAuth token value of current user's session to validate                                                                                                           | String                | No       |
| language          | Language code to localize texts                                                                                                                                   | String                | No       |
| allowMockLocation | Allow unreliable locations or not                                                                                                                                 | String                | No       |
| connectionTimeout | Bluetooth connection timeout in seconds. **Default: 5 seconds**                                                                                                   | Integer               | No       |
| autoCloseTimeout  | Auto close timeout for screen after pass completed, null means stay opened                                                                                        | Integer               | No       |
| waitBLEEnabled    | Flag to decide action for disabled Bluetooth state. "true" means wait user to enable Bluetooth, "false" means continue to next step if exists. **Default: false** | Bool                  | No       |
| closeColor        | [iOS] Color selection for close button. **Default: SystemBlue**                                                                                                   | UIColor               | No       |
| logLevel          | Minimum log level to follow. **Default: Info**                                                                                                                    | [LogLevel](#LogLevel) | No       |
| delegate          | Listener instance to get information about SDK events. Giving this instance in configuration allows to get logs with start of SDK initialization                  | MobilePassDelegate    | No       |

<br />

### `LogItem`

Details of received log item

| Parameter | Description                                   | Type                  | Required |
| --------- | --------------------------------------------- | --------------------- | -------- |
| level     | Type of log item                              | [LogLevel](#LogLevel) | Yes      |
| code      | Code value of log item for warnings and error | [LogCode](#LogCode)   | No       |
| message   | Information message about log action          | String                | Yes      |
| time      | Create time of log item                       | Date                  | Yes      |

<br />

### `PassFlowResult`

| Parameter | Description          | Type                                      | Required |
| --------- | -------------------- | ----------------------------------------- | -------- |
| result    | Result of flow       | [PassFlowResultCode](#PassFlowResultCode) | Yes      |
| direction | Pass direction       | [Direction](#Direction)                   | No       |
| clubId    | Id of matched club   | String                                    | No       |
| clubName  | Name of matched club | String                                    | No       |
| states    | Trail codes of flow  | \[[PassFlowState](#PassFlowState)\]       | Yes      |

<br />

### `PassFlowState` 

| Parameter | Description        | Type                                    | Required |
| --------- | ------------------ | --------------------------------------- | -------- |
| state     | Trail code         | [PassFlowStateCode](#PassFlowStateCode) | Yes      |
| data      | Additional content | String                                  | No       |

<br />
<br />

### **Events**

| Name                     | Description                                                                                                                           | Type                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| onLogReceived            | Receive log items                                                                                                                     | [LogItem](#LogItem)               |
| onInvalidQRCode          | QR code content when invalid format found                                                                                             | String                            |
| onMemberIdChanged        | Stored member id changed or new member id received                                                                                    |                                   |
|  onSyncMemberIdCompleted | Sync of member id with server completed                                                                                               |                                   |
|  onSyncMemberIdFailed    |  Sync of member id with server failed! Returns HTTPS status code or 0 (zero) for network error                                        |  Integer                          |
| onQRCodesDataLoaded      |  Stored QR Code list is ready to use, returns counts                                                                                  |  Integer                          |
|  onQRCodesSyncStarted    | Sync of QR Code list with server started                                                                                              |                                   |
| onQRCodesSyncFailed      |  QR Code list synchronization failed! Returns HTTPS status code or 0 (zero) for network error                                         |  Integer                          |
|  onQRCodesReady          | QR Code list is ready to use after synchronization. "synced" flag says list is up-to-date if true, otherwise stored list will be used |  Boolean, Integer                 |
| onQRCodesEmpty           |  QR Code list is empty means there is no record to use in pass flow                                                                   |                                   |
| onScanFlowCompleted      |  Returns for each pass flow that started with QR Code scan                                                                            | [PassFlowResult](#PassFlowResult) |

<br />
<br />

### **Constants**

<br />

### `LogLevel`

| Name  | Value |
| ----- | ----- |
| Debug | 1     |
| Info  | 2     |
| Warn  | 3     |
| Error | 4     |

<br />

### `Direction`

| Name     | Value |
| -------- | ----- |
| All      | 0     |
| Entrance | 1     |
| Exit     | 2     |

<br />

### `PassFlowResultCode`

| Name    | Value |
| ------- | ----- |
| Cancel  | 1     |
| Fail    | 2     |
| Success | 3     |

<br />

### `PassFlowStateCode`

| Value | Name                                           | Description                                                                                                            |
| ----- | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| 1001  |  SCAN_QRCODE_NEED_PERMISSION                   | Needs camera permission                                                                                                |
| 1002  |  SCAN_QRCODE_PERMISSION_GRANTED                | Camera permission is granted                                                                                           |
| 1003  |  SCAN_QRCODE_PERMISSION_REJECTED               | SDK has not been authorized to use camera                                                                              |
| 1004  |  SCAN_QRCODE_STARTED                           | QR Code scanning is started                                                                                            |
| 1005  |  SCAN_QRCODE_INVALID_CONTENT                   | Scanner has found valid QR Code but has invalid content                                                                |
| 1006  |  SCAN_QRCODE_INVALID_FORMAT                    | Scanner has found QR Code but has invalid format                                                                       |
| 1007  |  SCAN_QRCODE_NO_MATCH                          | Found QR Code is valid but has no matching definition in SDK                                                           |
| 1008  |  SCAN_QRCODE_FOUND                             | Valid QR Code has been found                                                                                           |
| 1009  |  SCAN_QRCODE_ERROR                             | An error occurred during QR Code scanning or opening Camera                                                            |
| 2001  |  CANCELLED_BY_USER                             | Pass flow has been cancelled by user                                                                                   |
| 2002  |  CANCELLED_WITH_INVALID_QRCODE                 | Pass flow has been cancelled beacuse of invalid QR Code data                                                           |
| 2003  |  CANCELLED_WITH_ERROR                          | Pass flow has been cancelled with error, check data for details                                                        |
| 2004  |  CANCELLED_WITH_MOCK_LOCATION                  | Pass flow has been cancelled beacuse of mock location data                                                             |
| 2005  |  CANCELLED_TO_GO_SETTINGS                      | Pass flow has been cancelled, user opened settings of device                                                           |
| 3101  |  INVALID_QRCODE_TRIGGER_TYPE                   | QR Code has invalid trigger type, definition need to be checked                                                        |
| 3102  |  INVALID_QRCODE_MISSING_CONTENT                | QR Code content is missing, definition need to be checked                                                              |
| 3201  |  INVALID_ACTION_LIST_EMPTY                     | QR Code available action list to trigger is empty, definition need to be checked                                       |
| 3202  |  INVALID_ACTION_TYPE                           | QR Code action type is invalid for this SDK version, definition need to be checked                                     |
| 3301  |  INVALID_BLUETOOTH_QRCODE_DATA                 | QR Code data is invalid to run Bluetooth action                                                                        |
| 3302  |  INVALID_BLUETOOTH_DIRECTION                   | QR Code direction value is invalid, definition need to be checked                                                      |
| 3303  |  INVALID_BLUETOOTH_HARDWARE_ID                 | QR Code hardware id value is invalid, definition need to be checked                                                    |
| 3304  |  INVALID_BLUETOOTH_RELAY_NUMBER                | QR Code relay number value is invalid, definition need to be checked                                                   |
| 3401  |  INVALID_REMOTE_ACCESS_QRCODE_DATA             | QR Code data is invalid to run Remote Access action                                                                    |
| 3402  |  INVALID_REMOTE_ACCESS_QRCODE_ID               | QR Code id is invalid, definition need to be checked                                                                   |
| 4101  |  PROCESS_ACTION_STARTED                        | Process QR Code action is started                                                                                      |
| 4102  |  PROCESS_ACTION_BLUETOOTH                      | Process QR Code for pass via Bluetooth                                                                                 |
| 4103  |  PROCESS_ACTION_LOCATION                       | Process QR Code for validation of user's location                                                                      |
| 4104  |  PROCESS_ACTION_REMOTE_ACCESS                  | Process QR Code for pass via Remote Access                                                                             |
| 4105  |  PROCESS_ACTION_LOCATION_NEED_PERMISSION       | Needs location permission                                                                                              |
| 4106  |  PROCESS_ACTION_LOCATION_NEED_ENABLED          | Needs location services be enabled                                                                                     |
| 4107  |  PROCESS_ACTION_LOCATION_PERMISSION_GRANTED    | Location permission is granted                                                                                         |
| 4108  |  PROCESS_ACTION_LOCATION_PERMISSION_REJECTED   | SDK has not been authorized to use location                                                                            |
| 4109  |  PROCESS_ACTION_BLUETOOTH_NEED_PERMISSION      | Needs Bluetooth scan permission                                                                                        |
| 4110  |  PROCESS_ACTION_BLUETOOTH_NEED_ENABLED         | Needs Bluetooth be enabled                                                                                             |
| 4111  |  PROCESS_ACTION_BLUETOOTH_PERMISSION_GRANTED   | Bluetooth scan permission is granted                                                                                   |
| 4112  |  PROCESS_ACTION_BLUETOOTH_PERMISSION_REJECTED  | SDK has not been authorized to use Bluetooth                                                                           |
| 4201  |  RUN_ACTION_BLUETOOTH_STARTED                  | Pass via Bluetooth is started                                                                                          |
| 4202  |  RUN_ACTION_BLUETOOTH_OFF_WAITING              | Waiting for Bluetooth to be activated                                                                                  |
| 4203  |  RUN_ACTION_BLUETOOTH_OFF_NO_WAIT              | Bluetooth is not enabled and configuration says no need to wait for Bluetooth to be activated                          |
| 4204  |  RUN_ACTION_BLUETOOTH_START_SCAN               | Started to scan nearby devices                                                                                         |
| 4205  |  RUN_ACTION_BLUETOOTH_TIMEOUT                  | Scanning has been cancelled because of timeout                                                                         |
| 4206  |  RUN_ACTION_BLUETOOTH_CONNECTING               | Connecting to found device                                                                                             |
| 4207  |  RUN_ACTION_BLUETOOTH_CONNECTED                | Bluetooth connection with device has been completed successfully                                                       |
| 4208  |  RUN_ACTION_BLUETOOTH_CONNECTION_FAILED        | Bluetooth connection could not be established. User authentication problem or PerfectGym access error can be the cause |
| 4209  |  RUN_ACTION_BLUETOOTH_PASS_SUCCEED             | Pass via Bluetooth completed successfully and user can pass now                                                        |
| 4210  |  RUN_ACTION_BLUETOOTH_PASS_FAILED              | Pass via Bluetooth failed, so user can not pass now or remote access action will be tried if exists                    |
| 4301  |  RUN_ACTION_LOCATION_WAITING                   | Validation of user's location with GPS is started                                                                      |
| 4302  |  RUN_ACTION_LOCATION_VALIDATED                 | User's location has been validated                                                                                     |
| 4303  |  RUN_ACTION_LOCATION_FAILED                    | Location validation has been failed, check data for details                                                            |
| 4401  |  RUN_ACTION_REMOTE_ACCESS_STARTED              | Pass via Remote Access is started                                                                                      |
| 4402  |  RUN_ACTION_REMOTE_ACCESS_REQUEST              | Remote access request is sending to server to communicate with device over Network                                     |
| 4403  |  RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED         | User is unauthorized to pass from this point                                                                           |
| 4404  |  RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED | Device is not connected to server now, so request could not be sent                                                    |
| 4405  |  RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT       | Device connection has failed with timeout                                                                              |
| 4406  |  RUN_ACTION_REMOTE_ACCESS_NO_NETWORK           | User has network problem, so request could not be sent to server                                                       |
| 4407  |  RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED       | An error occurred on server while processing remote access request                                                     |
| 4408  |  RUN_ACTION_REMOTE_ACCESS_REQUEST_SUCCEED      | Remote access request has completed successfully                                                                       |
| 4409  |  RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED         | Pass via Remote Access completed successfully and user can pass now                                                    |
| 4410  |  RUN_ACTION_REMOTE_ACCESS_PASS_FAILED          | Pass via Remote Access failed, so user can not pass now if no more action exists                                       |

<br />

### `LogCode`

| Name                                                         | Value |
| ------------------------------------------------------------ | ----- |
| Invalid configuration instance to start SDK                  | 1101  |
| Error on synchronization of member info with server          | 1201  |
| Error on synchronization of access points list               | 1202  |
| Error on generating key pair for member                      | 1301  |
| Storage problem occurred                                     | 1401  |
| Unexpected interrupt on Bluetooth scanning flow              | 2101  |
| Error on getting Adapter instance from service [Android]     | 2201  |
| Failure on starting Bluetooth scanner                        | 2202  |
| Unexpected interrupt on Bluetooth connection flow            | 2301  |
| Device not found during Bluetooth connection flow            | 2302  |
| Failure on Bluetooth connection                              | 2303  |
| New connection request received while another one is active  | 2304  |
| Unexpected interrupt on Bluetooth communication flow         | 2401  |
| Informing about permission requirement of Camera             | 3101  |
| Informing about permission requirement of Location           | 3102  |
| Informing about permission requirement of Bluetooth          | 3103  |
| Informing about need enable Bluetooth                        | 3104  |
| Informing about need enable Location Services                | 3105  |
| Pass flow interrupted with empty action list after qr code   | 4101  |
| Found qr code has empty content definition                   | 4102  |
| Error on showing map for location validation                 | 4103  |
| Invalid id on qr code content validation                     | 4201  |
| Invalid content data on qr code validation                   | 4202  |
| Invalid access point id on qr code validation                | 4203  |
| Invalid location data on qr code validation                  | 4204  |
| Invalid terminal data on qr code validation                  | 4205  |
| Invalid configuration on qr code validation                  | 4206  |
| Invalid trigger type on qr code validation                   | 4207  |
| Invalid door details on qr code validation                   | 4208  |
| Invalid trigger type found during processing qr code         | 4301  |
| Empty action data found during processing qr code            | 4302  |
| QR reader found invalid format                               | 4401  |
| QR reader found data but it has invalid content              | 4402  |
| QR reader could not find matching of valid qr code in list   | 4403  |
| Empty config found during action of qr code                  | 4501  |
| Invalid type found during action of qr code                  | 4502  |
| Empty qr code content found during action of qr code         | 4503  |
| Empty qr code id found during action of qr code              | 4504  |
| Empty direction value found during action of qr code         | 4505  |
| Empty hardware id found during action of qr code             | 4506  |
| Empty relay number value found during action of qr code      | 4507  |
| Checking next action has been cancelled due to invalid value | 4508  |
| Switch camera for qr code scanner is failed                  | 5101  |

<br />
