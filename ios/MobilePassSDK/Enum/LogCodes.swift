//
//  LogCodes.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 20.08.2021.
//

import Foundation

enum LogCodes: Int, Codable {
    case CONFIGURATION_VALIDATION                       = 1101;
    case CONFIGURATION_SERVER_SYNC_INFO                 = 1201;
    case CONFIGURATION_SERVER_SYNC_LIST                 = 1202;
    case CONFIGURATION_KEYPAIR                          = 1301;
    case CONFIGURATION_STORAGE                          = 1401;
    case BLUETOOTH_SCANNING_FLOW                        = 2101;
    case BLUETOOTH_ADAPTER_ERROR                        = 2201;
    case BLUETOOTH_SCANNER_FAILED                       = 2202;
    case BLUETOOTH_CONNECTION_FLOW                      = 2301;
    case BLUETOOTH_CONNECTION_NOT_FOUND                 = 2302;
    case BLUETOOTH_CONNECTION_FAILED                    = 2303;
    case BLUETOOTH_CONNECTION_DUPLICATE                 = 2304;
    case BLUETOOTH_COMMUNICATION_FLOW                   = 2401;
    case NEED_PERMISSION_DEFAULT                        = 3100;
    case NEED_PERMISSION_CAMERA                         = 3101;
    case NEED_PERMISSION_LOCATION                       = 3102;
    case NEED_PERMISSION_BLUETOOTH                      = 3103;
    case NEED_ENABLE_BLE                                = 3104;
    case NEED_ENABLE_LOCATION_SERVICES                  = 3105;
    case PASSFLOW_EMPTY_ACTION_LIST                     = 4101;
    case PASSFLOW_EMPTY_QRCODE_CONTENT                  = 4102;
    case PASSFLOW_MAP_ERROR                             = 4103;
    case PASSFLOW_QRCODE_VALIDATION_ID                  = 4201;
    case PASSFLOW_QRCODE_VALIDATION_DATA                = 4202;
    case PASSFLOW_QRCODE_VALIDATION_ACCESSPOINT_ID      = 4203;
    case PASSFLOW_QRCODE_VALIDATION_LOCATION            = 4204;
    case PASSFLOW_QRCODE_VALIDATION_TERMINAL            = 4205;
    case PASSFLOW_QRCODE_VALIDATION_CONFIG              = 4206;
    case PASSFLOW_QRCODE_VALIDATION_TRIGGERTYPE         = 4207;
    case PASSFLOW_QRCODE_VALIDATION_DOOR_DETAILS        = 4208;
    case PASSFLOW_PROCESS_QRCODE_TRIGGERTYPE            = 4301;
    case PASSFLOW_PROCESS_QRCODE_EMPTY_ACTION           = 4302;
    case PASSFLOW_QRCODE_READER_INVALID_FORMAT          = 4401;
    case PASSFLOW_QRCODE_READER_INVALID_CONTENT         = 4402;
    case PASSFLOW_QRCODE_READER_NO_MATCHING             = 4403;
    case PASSFLOW_ACTION_EMPTY_CONFIG                   = 4501;
    case PASSFLOW_ACTION_INVALID_TYPE                   = 4502;
    case PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT           = 4503;
    case PASSFLOW_ACTION_EMPTY_QRCODE_ID                = 4504;
    case PASSFLOW_ACTION_EMPTY_DIRECTION                = 4505;
    case PASSFLOW_ACTION_EMPTY_HARDWAREID               = 4506;
    case PASSFLOW_ACTION_EMPTY_RELAYNUMBER              = 4507;
    case PASSFLOW_ACTION_INVALID_NEXT_ACTION            = 4508;
    case UI_SWITCH_CAMERA_FAILED                        = 5101;
    case UI_CAMERA_SETUP_FAILED                         = 5102;
}
