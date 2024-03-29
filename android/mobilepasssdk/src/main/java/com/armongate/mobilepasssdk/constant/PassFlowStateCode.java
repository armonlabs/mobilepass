package com.armongate.mobilepasssdk.constant;

public class PassFlowStateCode {
    public static final int SCAN_QRCODE_NEED_PERMISSION = 1001;
    public static final int SCAN_QRCODE_PERMISSION_GRANTED = 1002;
    public static final int SCAN_QRCODE_PERMISSION_REJECTED = 1003;
    public static final int SCAN_QRCODE_STARTED = 1004;
    public static final int SCAN_QRCODE_INVALID_CONTENT = 1005;
    public static final int SCAN_QRCODE_INVALID_FORMAT = 1006;
    public static final int SCAN_QRCODE_NO_MATCH = 1007;
    public static final int SCAN_QRCODE_FOUND = 1008;
    public static final int SCAN_QRCODE_ERROR = 1009;
    public static final int CANCELLED_BY_USER = 2001;
    public static final int CANCELLED_WITH_INVALID_QRCODE = 2002;
    public static final int CANCELLED_WITH_ERROR = 2003;
    public static final int CANCELLED_WITH_MOCK_LOCATION = 2004;
    public static final int CANCELLED_TO_GO_SETTINGS = 2005;
    public static final int INVALID_QRCODE_TRIGGER_TYPE = 3101;
    public static final int INVALID_QRCODE_MISSING_CONTENT = 3102;
    public static final int INVALID_ACTION_LIST_EMPTY = 3201;
    public static final int INVALID_ACTION_TYPE = 3202;
    public static final int INVALID_BLUETOOTH_QRCODE_DATA = 3301;
    public static final int INVALID_BLUETOOTH_DIRECTION = 3302;
    public static final int INVALID_BLUETOOTH_HARDWARE_ID = 3303;
    public static final int INVALID_BLUETOOTH_RELAY_NUMBER = 3304;
    public static final int INVALID_REMOTE_ACCESS_QRCODE_DATA = 3401;
    public static final int INVALID_REMOTE_ACCESS_QRCODE_ID = 3402;
    public static final int PROCESS_ACTION_STARTED = 4101;
    public static final int PROCESS_ACTION_BLUETOOTH = 4102;
    public static final int PROCESS_ACTION_LOCATION = 4103;
    public static final int PROCESS_ACTION_REMOTE_ACCESS = 4104;
    public static final int PROCESS_ACTION_LOCATION_NEED_PERMISSION = 4105;
    public static final int PROCESS_ACTION_LOCATION_NEED_ENABLED = 4106;
    public static final int PROCESS_ACTION_LOCATION_PERMISSION_GRANTED = 4107;
    public static final int PROCESS_ACTION_LOCATION_PERMISSION_REJECTED = 4108;
    public static final int PROCESS_ACTION_BLUETOOTH_NEED_PERMISSION = 4109;
    public static final int PROCESS_ACTION_BLUETOOTH_NEED_ENABLED = 4110;
    public static final int PROCESS_ACTION_BLUETOOTH_PERMISSION_GRANTED = 4111;
    public static final int PROCESS_ACTION_BLUETOOTH_PERMISSION_REJECTED = 4112;
    public static final int RUN_ACTION_BLUETOOTH_STARTED = 4201;
    public static final int RUN_ACTION_BLUETOOTH_OFF_WAITING = 4202;
    public static final int RUN_ACTION_BLUETOOTH_OFF_NO_WAIT = 4203;
    public static final int RUN_ACTION_BLUETOOTH_START_SCAN = 4204;
    public static final int RUN_ACTION_BLUETOOTH_TIMEOUT = 4205;
    public static final int RUN_ACTION_BLUETOOTH_CONNECTING = 4206;
    public static final int RUN_ACTION_BLUETOOTH_CONNECTED = 4207;
    public static final int RUN_ACTION_BLUETOOTH_CONNECTION_FAILED = 4208;
    public static final int RUN_ACTION_BLUETOOTH_PASS_SUCCEED = 4209;
    public static final int RUN_ACTION_BLUETOOTH_PASS_FAILED = 4210;
    public static final int RUN_ACTION_LOCATION_WAITING = 4301;
    public static final int RUN_ACTION_LOCATION_VALIDATED = 4302;
    public static final int RUN_ACTION_LOCATION_FAILED = 4303;
    public static final int RUN_ACTION_REMOTE_ACCESS_STARTED = 4401;
    public static final int RUN_ACTION_REMOTE_ACCESS_REQUEST = 4402;
    public static final int RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED = 4403;
    public static final int RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED = 4404;
    public static final int RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT = 4405;
    public static final int RUN_ACTION_REMOTE_ACCESS_NO_NETWORK = 4406;
    public static final int RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED = 4407;
    public static final int RUN_ACTION_REMOTE_ACCESS_REQUEST_SUCCEED = 4408;
    public static final int RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED = 4409;
    public static final int RUN_ACTION_REMOTE_ACCESS_PASS_FAILED = 4410;
}
