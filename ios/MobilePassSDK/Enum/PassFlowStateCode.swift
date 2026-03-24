//
//  PassFlowStateCode.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

/// State codes exposed to app via delegate callbacks
/// Includes all meaningful milestones that app should track for complete flow understanding
public enum PassFlowStateCode: Int, Codable {
    // QR Code States
    case QRCODE_INVALID_FORMAT  = 1006  // QR has invalid format
    case QRCODE_NO_MATCH        = 1007  // QR not in authorized list
    case QRCODE_FOUND           = 1008  // QR validated successfully
    case QRCODE_CONTENT_INVALID = 1009  // QR has invalid/incomplete configuration
    
    // Data States
    case DATA_INVALID_MEMBER_ID                         = 1101  // Member has invalid id; zero or empty
    case DATA_QRCODE_LIST_INITIALIZING                  = 1201  // QR Code list state: Initializing
    case DATA_QRCODE_LIST_SYNCING                       = 1202  // QR Code list state: Sync active
    case DATA_QRCODE_LIST_USING_STORED_DATA             = 1203  // QR Code list state: Using stored data before sync completed
    case DATA_QRCODE_LIST_USING_STORED_DATA_AFTER_ERROR = 1204  // QR Code list state: Using stored data because of sync error
    case DATA_QRCODE_LIST_USING_SYNCED_DATA             = 1205  // QR Code list state: Using synced data
    
    // Action Tracking
    case PROCESS_ACTION_BLUETOOTH       = 4102  // Starting Bluetooth flow
    case PROCESS_ACTION_REMOTE_ACCESS   = 4104  // Starting remote access flow
    
    // Bluetooth States
    case RUN_ACTION_BLUETOOTH_OFF_WAITING       = 4202  // Waiting for user to enable BT
    case RUN_ACTION_BLUETOOTH_CONNECTING        = 4206  // Connecting to BLE device
    case RUN_ACTION_BLUETOOTH_TIMEOUT           = 4205  // BLE scan timeout
    case RUN_ACTION_BLUETOOTH_CONNECTION_FAILED = 4208  // BLE connection failed
    case RUN_ACTION_BLUETOOTH_PASS_SUCCEED      = 4209  // BLE pass successful!
    
    // Location States
    case RUN_ACTION_LOCATION_WAITING    = 4301  // Waiting for location verification
    case RUN_ACTION_LOCATION_VALIDATED  = 4302  // Location verified, proceeding
    
    // Remote Access States
    case RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED          = 4403  // 401 error
    case RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED  = 4404  // 404 error
    case RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT        = 4405  // 408 error
    case RUN_ACTION_REMOTE_ACCESS_NO_NETWORK            = 4406  // Network error
    case RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED        = 4407  // Other HTTP error
    case RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED          = 4409  // Remote success
}
