//
//  FlowDelegate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 10.02.2021.
//

public protocol PassFlowDelegate {
    func onQRCodeFound(code: String)
    func onLocationValidated()
    func onNextActionRequired()
    func onConnectionStateChanged(isActive: Bool)
    func onNeedPermissionMessage(type: Int)
}
