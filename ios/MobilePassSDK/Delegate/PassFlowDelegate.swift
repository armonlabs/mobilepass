//
//  FlowDelegate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 10.02.2021.
//

public protocol PassFlowDelegate {
    func onQRCodeFound(code: String)
    func onLocationValidated()
    func onPassCompleted(succeed: Bool)
    func onNextActionRequired()
    func onConnectionStateChanged(isActive: Bool)
    func needPermissionCamera()
    func needPermissionLocation()
    func needEnableBluetooth()
    func onError()
}
