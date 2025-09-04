//
//  DelegateManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

import Foundation
import SwiftUI

class DelegateManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = DelegateManager()
    private override init() {
        super.init()
    }
    
    // MARK: Private Fields
    
    public var isPassFlowCompleted: Bool = false
    public var isPassConnectionActive: Bool = false
    public var qrCodeListState: QRCodeListState = .INITIALIZING
    private var isDismissedManual: Bool = false
    private var mobilePassDelegate: MobilePassDelegate?
    private var mobilePassController: UIViewController?
    private var passFlowDelegate: PassFlowDelegate?
    private var qrCodeListStateDelegate: QRCodeListStateDelegate?
    private var qrCodeScannerDelegate: QRCodeScannerDelegate?
    private var timerAutoClose: Timer? = nil
    
    // MARK: Public Functions
    
    func clearFlags() {
        isPassFlowCompleted = false
        isPassConnectionActive = false
        isDismissedManual = false
    }
    
    func setPassFlowDelegate(delegate: PassFlowDelegate) {
        passFlowDelegate = delegate
    }

    func setMainDelegate(delegate: MobilePassDelegate?, viewController: UIViewController? = nil) {
        mobilePassDelegate = delegate
        mobilePassController = viewController
    }
    
    func setQRCodeStateDelegate(delegate: QRCodeListStateDelegate?) {
        qrCodeListStateDelegate = delegate
    }
    
    func setQRCodeScannerDelegate(delegate: QRCodeScannerDelegate) {
        qrCodeScannerDelegate = delegate
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
            if (success) {
                self.mobilePassDelegate?.onSyncMemberIdCompleted()
            } else {
                self.mobilePassDelegate?.onSyncMemberIdFailed(statusCode: (statusCode ?? -1))
            }
        }
    }
    
    func onQRCodesDataLoaded(count: Int) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onQRCodesDataLoaded(count: count)
        }
    }
    
    func onQRCodesSyncFailed(statusCode: Int) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onQRCodesSyncFailed(statusCode: statusCode)
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
    
    func onCompleted(success: Bool, isRemoteAccess: Bool, direction: Direction?, clubId: String?, clubName: String?) {
        isPassFlowCompleted = true

        DispatchQueue.main.async {
            self.mobilePassDelegate?.onScanFlowCompleted(result: PassFlowResult(
                result: success ? PassFlowResultCode.SUCCESS : PassFlowResultCode.FAIL,
                states: PassFlowManager.shared.getStates(),
                direction: direction,
                clubId: clubId,
                clubName: clubName))
        }
        
        startAutoCloseTimer()
        shareAnalytics(result: success ? .success : .fail, isRemoteAccess: isRemoteAccess, direction: direction, clubId: clubId)
    }
    
    func onCancelled(dismiss: Bool) {
        endFlow(dismiss: dismiss, reason: PassFlowStateCode.CANCELLED_BY_USER)
    }
    
    func flowLocationValidated() {
        passFlowDelegate?.onLocationValidated()
    }

    func flowQRCodeFound(code: String) {
        DispatchQueue.main.async {
            self.passFlowDelegate?.onQRCodeFound(code: code)
        }
    }
    
    func flowCloseWithInvalidQRCode(code: String) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onInvalidQRCode(content: code)
        }
        
        endFlow(dismiss: true,
                reason: PassFlowStateCode.CANCELLED_WITH_INVALID_QRCODE,
                data: code)
    }

    func flowNextActionRequired() {
        if (!isPassFlowCompleted) {
            passFlowDelegate?.onNextActionRequired()
        }
    }

    func flowConnectionStateChanged(isActive: Bool) {
        isPassConnectionActive = isActive
    }

    func needPermission(type: NeedPermissionType, showMessage: Bool) {
        var code: LogCodes? = nil
        
        switch type {
        case .NEED_PERMISSION_BLUETOOTH:
            code = LogCodes.NEED_PERMISSION_BLUETOOTH
            break
        case .NEED_PERMISSION_CAMERA:
            code = LogCodes.NEED_PERMISSION_CAMERA
            break
        case .NEED_PERMISSION_LOCATION:
            code = LogCodes.NEED_PERMISSION_LOCATION
            break
        case .NEED_ENABLE_BLE:
            code = LogCodes.NEED_ENABLE_BLE
            break
        case .NEED_ENABLE_LOCATION_SERVICES:
            code = LogCodes.NEED_ENABLE_LOCATION_SERVICES
            break
        }
        
        LogManager.shared.warn(message: "Need permission to continue passing flow, permission type: \(type.rawValue)", code: code)
        
        if (showMessage) {
            passFlowDelegate?.onNeedPermissionMessage(type: type.rawValue)
        }
    }
    
    func goToSettings() {
        self.endFlow(dismiss: true, reason: PassFlowStateCode.CANCELLED_TO_GO_SETTINGS)
    }
    
    func errorOccurred(message: String? = nil) {
        endFlow(dismiss: true, reason: PassFlowStateCode.CANCELLED_WITH_ERROR, data: message)
    }
    
    func qrCodeListChanged(state: QRCodeListState, count: Int) {
        qrCodeListState = state
        
        DispatchQueue.main.async {
            if (state == .SYNCING) {
                self.mobilePassDelegate?.onQRCodesSyncStarted()
            } else {
                if (count == 0) {
                    self.mobilePassDelegate?.onQRCodesEmpty()
                } else {
                    self.mobilePassDelegate?.onQRCodesReady(
                        synced: state == .USING_SYNCED_DATA,
                        count: count)
                }
            }
            
            self.qrCodeListStateDelegate?.onStateChanged(state: state.rawValue)
        }
    }
    
    func qrCodeScannerSwitchCamera() {
        self.qrCodeScannerDelegate?.onSwitchCamera()
    }
    
    func onMockLocationDetected() {
        endFlow(dismiss: true, reason: PassFlowStateCode.CANCELLED_WITH_MOCK_LOCATION)
    }
    
    private func endFlow(dismiss: Bool, reason: PassFlowStateCode?, data: String? = nil) {
        endAutoCloseTimer()
        
        if (dismiss) {
            isDismissedManual = true
            DispatchQueue.main.async {
                self.mobilePassController?.dismiss(animated: true, completion: {
                    if (!self.isPassFlowCompleted) {
                        if (reason != nil) {
                            self.cancelFlow(reason: reason!, data: data)
                        }
                        self.isPassFlowCompleted = true;
                    }
                })
            }
        } else if (!isPassFlowCompleted && !isDismissedManual) {
            if (reason != nil) {
                self.cancelFlow(reason: reason!, data: data)
            }
            self.isPassFlowCompleted = true;
        }
    }
    
    private func cancelFlow(reason: PassFlowStateCode, data: String? = nil) {
        PassFlowManager.shared.addToStates(state: reason, data: data)
        
        self.mobilePassDelegate?.onScanFlowCompleted(result: PassFlowResult(
            result: PassFlowResultCode.CANCEL,
            states: PassFlowManager.shared.getStates(),
            direction: nil, clubId: nil, clubName: nil))

        shareAnalytics(result: .cancel)
    }
    
    private func startAutoCloseTimer() {
        if (ConfigurationManager.shared.autoCloseTimeout() != nil) {
            DispatchQueue.main.async {
                self.timerAutoClose = Timer.scheduledTimer(withTimeInterval: Double(ConfigurationManager.shared.autoCloseTimeout()!) , repeats: false, block: { timer in
                    self.endFlow(dismiss: true, reason: nil)
                })
            }
        }
    }
    
    private func endAutoCloseTimer() {
        if (timerAutoClose != nil) {
            timerAutoClose!.invalidate()
            timerAutoClose = nil
        }
    }
    
}
