# Armon MobilePass SDK

A headless SDK for secure access control via QR codes and Bluetooth Low Energy (BLE). Enables mobile apps to communicate with Armon-compatible access points for seamless entry.

## 🎯 SDK Focus

**MobilePass SDK** is a **headless library** that provides:

- ✅ QR code validation and processing
- ✅ Bluetooth Low Energy (BLE) communication with access points
- ✅ Remote access via server communication
- ✅ Automatic fallback between BLE and remote access
- ✅ State management and flow orchestration
- ✅ Location verification for remote access (when required)

**Your app controls:**

- 🎨 All UI/UX (camera, maps, dialogs, loading states)
- 📸 QR code scanning implementation
- 📍 Location services and verification

---

## ⚙️ Requirements

### Android

- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 34+
- **Permissions:**
  - `ACCESS_FINE_LOCATION` - For BLE scanning
  - `BLUETOOTH_SCAN` (Android 12+) - For BLE device discovery
  - `BLUETOOTH_CONNECT` (Android 12+) - For BLE communication

### iOS

- **Min iOS Version:** 13.0+
- **Swift:** 5.0+
- **Permissions:**
  - `NSBluetoothAlwaysUsageDescription` - For BLE communication

---

## 🚀 Installation

### Android

Add JitPack repository to your root `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Add dependency to app-level `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.armonlabs:mobilepass:v2.0.0'
}
```

### iOS

Add Swift package dependency by using File > Swift Packages > Add Package Dependency

```
https://github.com/armonlabs/mobilepass.git
```

Make sure that your target is selected

---

## 📖 Quick Start

### Android

```java
public class MainActivity extends AppCompatActivity implements MobilePassDelegate {

    private MobilePass sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure SDK
        Configuration config = new Configuration();
        config.apiKey = "your-api-key-here";
        config.memberId = "user123";
        config.serverUrl = "https://api.example.com";
        config.language = "en";
        config.connectionTimeout = 10;
        config.locationVerificationTimeout = 30;
        config.listener = this;
        config.logLevel = LogLevel.INFO;

        // Initialize
        sdk = new MobilePass(this, config);
    }

    // Process QR code when scanned
    public void onQRCodeScanned(String qrData) {
        QRCodeProcessResult result = sdk.processQRCode(qrData);

        if (result.isValid()) {
            Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();
        } else {
            handleError(result.getErrorType());
        }
    }

    // Implement MobilePassDelegate callbacks
    @Override
    public void onPassFlowStateChanged(PassFlowStateUpdate update) {
        // Handle state changes and completion
    }

    @Override
    public void onLocationVerificationRequired(LocationRequirement requirement) {
        // Show location verification UI if needed
    }

    @Override
    public void onPermissionRequired(int type) {
        // Request required permissions
    }

    // ... other delegate methods
}
```

### iOS

```swift
class ViewController: UIViewController, MobilePassDelegate {

    var sdk: MobilePass?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Configure SDK
        let config = Configuration(
            apiKey: "your-api-key-here",
            memberId: "user123",
            serverUrl: "https://api.example.com",
            barcode: nil,
            language: "en",
            connectionTimeout: 10,
            locationVerificationTimeout: 30,
            continueWithoutBLE: false,
            logLevel: .info,
            delegate: self
        )

        // Initialize
        sdk = MobilePass(config: config)
    }

    // Process QR code when scanned
    func onQRCodeScanned(data: String) {
        let result = sdk?.processQRCode(data: data)

        if result?.isValid == true {
            showProgress("Processing...")
        } else {
            handleError(result?.errorType)
        }
    }

    // Implement MobilePassDelegate callbacks
    func onPassFlowStateChanged(state: PassFlowStateUpdate) {
        // Handle state changes and completion
    }

    func onLocationVerificationRequired(requirement: LocationRequirement) {
        // Show location verification UI if needed
    }

    func onPermissionRequired(type: NeedPermissionType) {
        // Request required permissions
    }

    // ... other delegate methods
}
```

> **Note:** See [API Reference](#-api-reference) section below for complete delegate callback implementations and detailed examples.

---

## 📚 API Reference

### Core Methods

#### `processQRCode(data: String)`

Process a scanned QR code and automatically start the pass flow.

**Returns:** `QRCodeProcessResult`

- `isValid: Bool` - Whether QR code is valid
- `errorType: QRCodeErrorType?` - Error type if invalid

**QRCodeErrorType Values:**

- `INVALID_FORMAT` (1) - Malformed QR data
- `NOT_FOUND` (2) - Not in authorized list
- `EXPIRED` (3) - QR code expired _(future use)_
- `UNAUTHORIZED` (4) - User not authorized _(future use)_

#### `sync()`

Synchronize authorized QR codes from server. Call on app launch or when refreshing access list.

#### `confirmLocationVerified()`

Confirm that location verification passed. Call after verifying user is within the required radius.

#### `cancelPassFlow()`

Cancel the current pass flow operation.

---

### Delegate Callbacks

#### `onPassFlowStateChanged(state: PassFlowStateUpdate)`

Called when pass flow state changes.

**PassFlowStateUpdate Types:**

- `stateChanged(state: Int, message: String?)` - Intermediate state
- `completed(result: PassFlowResult)` - Final result

**State Codes:**

```
QR Code States:
- 1006: SCAN_QRCODE_INVALID_FORMAT
- 1007: SCAN_QRCODE_NO_MATCH
- 1008: SCAN_QRCODE_FOUND

Action Tracking:
- 4102: PROCESS_ACTION_BLUETOOTH
- 4104: PROCESS_ACTION_REMOTE_ACCESS

Bluetooth States:
- 4202: RUN_ACTION_BLUETOOTH_OFF_WAITING
- 4206: RUN_ACTION_BLUETOOTH_CONNECTING
- 4205: RUN_ACTION_BLUETOOTH_TIMEOUT
- 4208: RUN_ACTION_BLUETOOTH_CONNECTION_FAILED
- 4209: RUN_ACTION_BLUETOOTH_PASS_SUCCEED

Location States:
- 4301: RUN_ACTION_LOCATION_WAITING
- 4302: RUN_ACTION_LOCATION_VALIDATED

Remote Access States:
- 4403: RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED
- 4404: RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED
- 4405: RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT
- 4406: RUN_ACTION_REMOTE_ACCESS_NO_NETWORK
- 4407: RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED
- 4409: RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED
```

**PassFlowResult:**

- `result: Int` - Result code:
  - `1` - CANCEL: Flow cancelled by app
  - `2` - SUCCESS: Pass succeed
  - `3` - FAIL: General failure
  - `4` - FAIL_PERMISSION: Permission required (Bluetooth/Location)
  - `5` - FAIL_BLE_DISABLED: Bluetooth disabled
  - `6` - FAIL_LOCATION_TIMEOUT: Location verification timeout
- `direction: Int?` - Direction if available
- `clubId: String?` - Club ID if available
- `clubName: String?` - Club name if available
- `states: [PassFlowState]` - History of states

#### `onLocationVerificationRequired(requirement: LocationRequirement)`

Called when remote access requires location verification.

**LocationRequirement:**

- `latitude: Double` - Target latitude
- `longitude: Double` - Target longitude
- `radius: Int` - Required radius in meters

**Action:** Show map UI and verify user is within radius, then call `sdk.confirmLocationVerified()`

#### `onPermissionRequired(type: NeedPermissionType)`

Called when SDK needs a permission or system capability to proceed with BLE access.

**NeedPermissionType Values:**

- `NEED_PERMISSION_BLUETOOTH` (1) - Bluetooth permission required (includes location on Android 11-)
- `NEED_ENABLE_BLE` (2) - Bluetooth needs to be enabled

**Behavior:**

- If `continueWithoutBLE = true` **and** there's a fallback action (e.g., remote access), the SDK will **skip** to the next action
- If `continueWithoutBLE = false` **or** there's no fallback action, the SDK will **wait** for the permission/capability
- For missing **permissions** (`NEED_PERMISSION_BLUETOOTH`), the app should request permission and retry the flow after granted
- For disabled **Bluetooth state** (`NEED_ENABLE_BLE`), the SDK will auto-retry when Bluetooth is enabled (if waiting)

**Note:** On Android 11 and below, location permission is required by the system for BLE scanning. The SDK automatically checks this and will notify with `NEED_PERMISSION_BLUETOOTH` if missing.

**Action:** Show appropriate permission dialog or settings prompt based on the type

#### `onQRCodesSyncStateChanged(state: QRCodesSyncState)`

Called when QR code sync state changes.

**QRCodesSyncState Values:**

- `syncStarted` - Sync started
- `syncCompleted(synced: Bool, count: Int)` - Sync finished
- `syncFailed(statusCode: Int)` - Sync failed with HTTP status
- `dataEmpty` - No QR codes available

#### `onMemberIdChanged()`

Called when member ID is updated.

#### `onSyncMemberIdCompleted(success: Bool, statusCode: Int?)`

Called when member ID sync completes.

#### `onLogReceived(log: LogItem)`

Called for SDK log messages (optional debugging).

---

## 🔧 Configuration Options

### Android Configuration

| Property                      | Type               | Required | Default | Description                                                    |
| ----------------------------- | ------------------ | -------- | ------- | -------------------------------------------------------------- |
| `apiKey`                      | String             | ✅ Yes   | -       | Unique API key for SDK authentication                          |
| `memberId`                    | String             | ✅ Yes   | -       | User identifier                                                |
| `serverUrl`                   | String             | ✅ Yes   | -       | API server URL                                                 |
| `listener`                    | MobilePassDelegate | ✅ Yes   | -       | Callback handler                                               |
| `language`                    | String             | No       | `"en"`  | Language code (`"en"` or `"tr"`)                               |
| `barcode`                     | String             | No       | `null`  | Benefits Barcode ID                                            |
| `continueWithoutBLE`          | Boolean            | No       | `false` | Continue to next action if BLE unavailable (disabled/no perms) |
| `connectionTimeout`           | Integer            | No       | `10`    | BLE connection timeout in seconds                              |
| `locationVerificationTimeout` | Integer            | No       | `30`    | Location verification timeout in seconds                       |
| `logLevel`                    | LogLevel           | No       | `INFO`  | Log verbosity level                                            |

**Example:**

```java
Configuration config = new Configuration();
config.apiKey = "your-api-key-here";
config.memberId = "user123";
config.serverUrl = "https://api.example.com";
config.language = "en";
config.continueWithoutBLE = false;
config.connectionTimeout = 10;
config.locationVerificationTimeout = 30;
config.listener = this;
config.logLevel = LogLevel.INFO;
```

### iOS Configuration

| Property                      | Type                | Required | Default | Description                                                    |
| ----------------------------- | ------------------- | -------- | ------- | -------------------------------------------------------------- |
| `apiKey`                      | String              | ✅ Yes   | -       | Unique API key for SDK authentication                          |
| `memberId`                    | String              | ✅ Yes   | -       | User identifier                                                |
| `serverUrl`                   | String              | ✅ Yes   | -       | API server URL                                                 |
| `delegate`                    | MobilePassDelegate? | No       | `nil`   | Callback handler                                               |
| `language`                    | String?             | No       | `"en"`  | Language code (`"en"` or `"tr"`)                               |
| `barcode`                     | String?             | No       | `nil`   | Benefits Barcode ID                                            |
| `continueWithoutBLE`          | Bool?               | No       | `false` | Continue to next action if BLE unavailable (disabled/no perms) |
| `connectionTimeout`           | Int?                | No       | `10`    | BLE connection timeout in seconds                              |
| `locationVerificationTimeout` | Int?                | No       | `30`    | Location verification timeout in seconds                       |
| `logLevel`                    | LogLevel?           | No       | `.info` | Log verbosity level                                            |

**Example:**

```swift
let config = Configuration(
    apiKey: "your-api-key-here",
    memberId: "user123",
    serverUrl: "https://api.example.com",
    barcode: nil,
    language: "en",
    connectionTimeout: 10,
    locationVerificationTimeout: 30,
    continueWithoutBLE: false,
    logLevel: .info,
    delegate: self
)
```

---

## 📄 License

**⚠️ Commercial SDK Notice:**
This SDK is proprietary software intended for authorized commercial use only. Redistribution, sharing with third parties, or unauthorized use is strictly prohibited without explicit written permission from Armongate.

---

## 🆘 Support

For technical support, integration assistance, or feature requests:

- **SDK Related Technical Support:** mobile@armongate.com
- **Access Control System General Support:** support@armongate.com
