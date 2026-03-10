package com.armongate.mobilepasssdk.constant;

/**
 * State codes exposed to app via delegate callbacks
 * Includes all meaningful milestones that app should track for complete flow understanding
 */
public class PassFlowStateCode {
    // QR Code States (3)
    public static final int SCAN_QRCODE_INVALID_FORMAT = 1006;      // QR has invalid format
    public static final int SCAN_QRCODE_NO_MATCH = 1007;            // QR not in authorized list
    public static final int SCAN_QRCODE_FOUND = 1008;               // QR validated successfully
    
    // Action Tracking (2)
    public static final int PROCESS_ACTION_BLUETOOTH = 4102;        // Starting Bluetooth flow
    public static final int PROCESS_ACTION_REMOTE_ACCESS = 4104;    // Starting remote access flow
    
    // Bluetooth States (5)
    public static final int RUN_ACTION_BLUETOOTH_OFF_WAITING = 4202;    // Waiting for user to enable BT
    public static final int RUN_ACTION_BLUETOOTH_CONNECTING = 4206;     // Connecting to BLE device
    public static final int RUN_ACTION_BLUETOOTH_TIMEOUT = 4205;        // BLE scan timeout
    public static final int RUN_ACTION_BLUETOOTH_CONNECTION_FAILED = 4208;  // BLE connection failed
    public static final int RUN_ACTION_BLUETOOTH_PASS_SUCCEED = 4209;   // BLE pass successful!
    
    // Location States (2)
    public static final int RUN_ACTION_LOCATION_WAITING = 4301;      // Waiting for location verification
    public static final int RUN_ACTION_LOCATION_VALIDATED = 4302;    // Location verified, proceeding
    
    // Remote Access States (6)
    public static final int RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED = 4403;           // 401 error
    public static final int RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED = 4404;  // 404 error
    public static final int RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT = 4405;        // 408 error
    public static final int RUN_ACTION_REMOTE_ACCESS_NO_NETWORK = 4406;            // Network error
    public static final int RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED = 4407;        // Other HTTP error
    public static final int RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED = 4409;          // Remote success
}
