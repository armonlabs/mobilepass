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

    
    func setMainDelegate(delegate: MobilePassDelegate?, viewController: UIViewController) {
        mobilePassDelegate = delegate
        mobilePassController = viewController
    }
    
    func setQRCodeStateDelegate(delegate: QRCodeListStateDelegate?) {
        qrCodeListStateDelegate = delegate
    }
    
    func isQRCodeListRefreshable() -> Bool {
        return qrCodeListState != QRCodeListState.INITIALIZING && qrCodeListState != QRCodeListState.SYNCING
    }
    
    func onCompleted(succeed: Bool) {
        isPassFlowCompleted = true
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onPassCompleted(succeed: succeed)
        }
        
        startAutoCloseTimer()
    }
    
    func onCancelled(dismiss: Bool) {
        endFlow(dismiss: dismiss, cancelReason: CancelReason.USER_CLOSED)
    }
    
    func flowLocationValidated() {
        passFlowDelegate?.onLocationValidated()
    }

    func flowQRCodeFound(code: String) {
        passFlowDelegate?.onQRCodeFound(code: code)
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
        self.mobilePassDelegate?.onNeedPermission(type: type.rawValue)
        
        if (showMessage) {
            passFlowDelegate?.onNeedPermissionMessage(type: type.rawValue)
        }
    }
    
    func goToSettings() {
        self.endFlow(dismiss: true, cancelReason: nil)
    }
    
    func errorOccurred() {
        endFlow(dismiss: true, cancelReason: CancelReason.ERROR)
    }
    
    func qrCodeListChanged(state: QRCodeListState) {
        qrCodeListState = state
        
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onQRCodeListStateChanged(state: state.rawValue)
        }
        
        self.qrCodeListStateDelegate?.onStateChanged(state: state.rawValue)
    }
    
    func onMockLocationDetected() {
        endFlow(dismiss: true, cancelReason: CancelReason.USING_MOCK_LOCATION_DATA)
    }
    
    private func endFlow(dismiss: Bool, cancelReason: CancelReason?) {
        endAutoCloseTimer()
        
        if (dismiss) {
            isDismissedManual = true
            DispatchQueue.main.async {
                self.mobilePassController?.dismiss(animated: true, completion: {
                    if (!self.isPassFlowCompleted) {
                        if (cancelReason != nil) {
                            self.mobilePassDelegate?.onPassCancelled(reason: cancelReason!.rawValue)
                        }
                        self.isPassFlowCompleted = true;
                    }
                })
            }
        } else if (!isPassFlowCompleted && !isDismissedManual) {
            if (cancelReason != nil) {
                mobilePassDelegate?.onPassCancelled(reason: cancelReason!.rawValue)
            }
            self.isPassFlowCompleted = true;
        }
    }
    
    private func startAutoCloseTimer() {
        if (ConfigurationManager.shared.autoCloseTimeout() != nil) {
            DispatchQueue.main.async {
                self.timerAutoClose = Timer.scheduledTimer(withTimeInterval: Double(ConfigurationManager.shared.autoCloseTimeout()!) , repeats: false, block: { timer in
                    self.endFlow(dismiss: true, cancelReason: nil)
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
