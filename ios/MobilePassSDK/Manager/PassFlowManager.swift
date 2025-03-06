//
//  PassFlowManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

class PassFlowManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = PassFlowManager()
    private override init() {
        super.init()
    }
    
    // MARK: Fields
    
    private var states: [PassFlowState] = [];
    private var logStates: [PassFlowState] = [];
    private var lastQRCodeId: String? = nil;
    private var lastClubId: String? = nil;
    
    private var ignore: [PassFlowStateCode] = [
        PassFlowStateCode.SCAN_QRCODE_NEED_PERMISSION,
        PassFlowStateCode.SCAN_QRCODE_PERMISSION_GRANTED,
        PassFlowStateCode.SCAN_QRCODE_STARTED,
        PassFlowStateCode.SCAN_QRCODE_INVALID_CONTENT,
        PassFlowStateCode.SCAN_QRCODE_INVALID_FORMAT,
        PassFlowStateCode.SCAN_QRCODE_ERROR,
        PassFlowStateCode.INVALID_QRCODE_TRIGGER_TYPE,
        PassFlowStateCode.INVALID_QRCODE_MISSING_CONTENT,
        PassFlowStateCode.INVALID_ACTION_LIST_EMPTY,
        PassFlowStateCode.INVALID_ACTION_TYPE,
        PassFlowStateCode.INVALID_BLUETOOTH_QRCODE_DATA,
        PassFlowStateCode.INVALID_BLUETOOTH_DIRECTION,
        PassFlowStateCode.INVALID_BLUETOOTH_HARDWARE_ID,
        PassFlowStateCode.INVALID_BLUETOOTH_RELAY_NUMBER,
        PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_DATA,
        PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_ID,
        PassFlowStateCode.PROCESS_ACTION_STARTED,
        PassFlowStateCode.PROCESS_ACTION_LOCATION_NEED_PERMISSION,
        PassFlowStateCode.PROCESS_ACTION_LOCATION_NEED_ENABLED,
        PassFlowStateCode.PROCESS_ACTION_LOCATION_PERMISSION_GRANTED,
        PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_PERMISSION,
        PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_ENABLED,
        PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_PERMISSION_GRANTED,
        PassFlowStateCode.RUN_ACTION_BLUETOOTH_STARTED,
        PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_NO_WAIT,
        PassFlowStateCode.RUN_ACTION_BLUETOOTH_START_SCAN,
        PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTING,
        PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTED,
        PassFlowStateCode.RUN_ACTION_LOCATION_WAITING,
        PassFlowStateCode.RUN_ACTION_LOCATION_VALIDATED,
        PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_STARTED,
        PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST,
        PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_STARTED,
        PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST,
        PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_SUCCEED
    ];
    
    // MARK: Public Functions
    
    func clearStates() {
        self.states.removeAll()
        self.logStates.removeAll()
        
        self.lastClubId = nil;
        self.lastQRCodeId = nil;
    }
    
    func getStates() -> [PassFlowState] {
        return self.states
    }

    func getLogStates() -> [PassFlowState] {
        return self.logStates
    }
    
    func getClubId() -> String? {
        return self.lastClubId
    }
    
    func getQRCodeId() -> String? {
        return self.lastQRCodeId
    }
    
    func addToStates(state: PassFlowStateCode, data: String? = nil) {
        if (self.ignore.contains(state)) {
            LogManager.shared.debug(message: "State has been ignored for code: \(state.rawValue)")
        } else {
            if (state == PassFlowStateCode.SCAN_QRCODE_NO_MATCH
                && self.states.contains(where: { $0.state == PassFlowStateCode.SCAN_QRCODE_NO_MATCH.rawValue })) {
                LogManager.shared.debug(message: "No match QR Code state has been added before")
            } else {
                self.states.append(PassFlowState(state: state, data: data))
            }
        }
        
        self.logStates.append(PassFlowState(state: state, data: data, datetime: Date()))
    }
    
    func setQRData(qrId: String?, clubId: String?) {
        self.lastQRCodeId = qrId
        self.lastClubId = clubId
    }
    
}
