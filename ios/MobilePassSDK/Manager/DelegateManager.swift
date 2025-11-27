//
//  DelegateManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

import Foundation

class DelegateManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = DelegateManager()
    private override init() {
        super.init()
    }
    
    // MARK: Private Fields
    
    public var qrCodeListState: QRCodeListState = .INITIALIZING
    private var mobilePassDelegate: MobilePassDelegate?
    
    // MARK: Public Functions
    
    func setMainDelegate(delegate: MobilePassDelegate?) {
        mobilePassDelegate = delegate
    }
    
    func onLogItemCreated(log: LogItem) {
        mobilePassDelegate?.onLogReceived(log: log)
    }
    
    func isQRCodeListRefreshable() -> Bool {
        return qrCodeListState != QRCodeListState.INITIALIZING && qrCodeListState != QRCodeListState.SYNCING
    }
    
    func onMemberIdChanged() {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onMemberIdChanged()
        }
    }
    
    func onMemberIdSyncCompleted(success: Bool, statusCode: Int?) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onSyncMemberIdCompleted(
                success: success,
                statusCode: success ? nil : statusCode
            )
        }
    }
    
    func onQRCodesSyncFailed(statusCode: Int) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onQRCodesSyncStateChanged(state: .syncFailed(statusCode: statusCode))
        }
    }

    func shareAnalytics(result: AnalyticsResult, isRemoteAccess: Bool? = nil, direction: Direction? = nil, clubId: String? = nil) {
        let states = PassFlowManager.shared.getLogStates()
        let startTime = states.first?.datetime ?? Date()
        let duration = Int64(Date().timeIntervalSince(startTime) * 1000)

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]

        let analyticsSteps = states.map { state in
            AnalyticsStep(
                code: state.state,
                message: state.data,
                timestamp: state.datetime != nil ? formatter.string(from: state.datetime!) : formatter.string(from: startTime)
            )
        }

        let request = RequestAnalyticsData(
            accessTime: formatter.string(from: Date()),
            duration: duration,
            result: result,
            method: isRemoteAccess == nil ? nil : (isRemoteAccess! ? .remote : .ble),
            clubId: clubId ?? PassFlowManager.shared.getClubId(),
            qrCodeId: PassFlowManager.shared.getQRCodeId(),
            direction: direction?.rawValue,
            os: .ios,
            steps: analyticsSteps
        )

        AnalyticsService().sendAnalytics(request: request) { _ in }
    }
    
    func onCompleted(resultCode: Int, isRemoteAccess: Bool, direction: Direction?, clubId: String?, clubName: String?) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onPassFlowStateChanged(
                state: .completed(result: PassFlowResult(
                result: PassFlowResultCode(rawValue: resultCode) ?? .FAIL,
                states: PassFlowManager.shared.getStates(),
                direction: direction,
                clubId: clubId,
                    clubName: clubName
                ))
            )
        }
        
        // Map result code to analytics result
        let analyticsResult: AnalyticsResult
        if resultCode == PassFlowResultCode.CANCEL.rawValue {
            analyticsResult = .cancel
        } else if resultCode == PassFlowResultCode.SUCCESS.rawValue {
            analyticsResult = .success
        } else {
            analyticsResult = .fail
        }
        
        shareAnalytics(result: analyticsResult, isRemoteAccess: isRemoteAccess, direction: direction, clubId: clubId)
    }
    
    func needPermission(type: NeedPermissionType) {
        var code: LogCodes? = nil
        
        switch type {
        case .NEED_PERMISSION_BLUETOOTH:
            code = LogCodes.NEED_PERMISSION_BLUETOOTH
            break
        case .NEED_ENABLE_BLE:
            code = LogCodes.NEED_ENABLE_BLE
            break
        }
        
        LogManager.shared.warn(message: "Need permission to continue passing flow, permission type: \(type.rawValue)", code: code)
        
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onPermissionRequired(type: type)
        }
    }
    
    // MARK: Delegate Notification Methods
    
    func notifyStateChanged(state: PassFlowStateCode, data: String?) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onPassFlowStateChanged(
                state: .stateChanged(state: state.rawValue, message: data)
            )
        }
    }
    
    func notifyLocationRequired(requirement: LocationRequirement) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onLocationVerificationRequired(requirement: requirement)
        }
    }
    
    func qrCodeListChanged(state: QRCodeListState, count: Int) {
        qrCodeListState = state
        
        DispatchQueue.main.async {
            if (state == .SYNCING) {
                self.mobilePassDelegate?.onQRCodesSyncStateChanged(state: .syncStarted)
            } else {
                if (count == 0) {
                    self.mobilePassDelegate?.onQRCodesSyncStateChanged(state: .dataEmpty)
                } else {
                    self.mobilePassDelegate?.onQRCodesSyncStateChanged(
                        state: .syncCompleted(
                        synced: state == .USING_SYNCED_DATA,
                            count: count
                        )
                    )
                }
            }
        }
    }
    
    // Note: onMockLocationDetected removed - location verification is now app's responsibility
    
}
