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
    
    private var isPassFlowCompleted: Bool = false
    private var isDismissedManual: Bool = false
    private var mobilePassDelegate: MobilePassDelegate?
    private var mobilePassController: UIViewController?
    
    // MARK: Public Functions
    
    func setMainDelegate(delegate: MobilePassDelegate?, viewController: UIViewController) {
        mobilePassDelegate = delegate
        mobilePassController = viewController
        
        isPassFlowCompleted = false
        isDismissedManual = false
    }
    
    func onCompleted(succeed: Bool) {
        isPassFlowCompleted = true
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onPassCompleted(succeed: succeed)
        }
    }
    
    func onCancelled(dismiss: Bool) {
        endFlow(dismiss: dismiss, cancelReason: CancelReason.USER_CLOSED)
    }
    
    func needPermissionLocation() {
        endFlow(dismiss: true, cancelReason: CancelReason.NEED_PERMISSION_LOCATION)
    }
    
    func needPermissionCamera() {
        endFlow(dismiss: true, cancelReason: CancelReason.NEED_PERMISSION_CAMERA)
    }
    
    func needBluetoothEnabled() {
        endFlow(dismiss: true, cancelReason: CancelReason.NEED_ENABLE_BLE)
    }
    
    func errorOccurred() {
        endFlow(dismiss: true, cancelReason: CancelReason.ERROR)
    }
    
    func qrCodeListChanged(state: QRCodeListState) {
        DispatchQueue.main.async {
            self.mobilePassDelegate?.onQRCodeListStateChanged(state: state.rawValue)
        }
    }
    
    func onMockLocationDetected() {
        endFlow(dismiss: true, cancelReason: CancelReason.USING_MOCK_LOCATION_DATA)
    }
    
    private func endFlow(dismiss: Bool, cancelReason: CancelReason) {
        if (dismiss) {
            isDismissedManual = true
            DispatchQueue.main.async {
                self.mobilePassController?.dismiss(animated: true, completion: {
                    if (!self.isPassFlowCompleted) {
                        self.mobilePassDelegate?.onPassCancelled(reason: cancelReason.rawValue)
                    }
                })
            }
        } else if (!isPassFlowCompleted && !isDismissedManual) {
            self.mobilePassDelegate?.onPassCancelled(reason: cancelReason.rawValue)
        }
    }
}
