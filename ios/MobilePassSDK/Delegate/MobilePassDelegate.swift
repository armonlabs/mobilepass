//
//  MobilePassDelegate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

public protocol MobilePassDelegate {
    func onLogReceived(log: LogItem)
    
    func onMemberIdChanged()
    func onSyncMemberIdCompleted(success: Bool, statusCode: Int?)
    func onQRCodesSyncStateChanged(state: QRCodesSyncState)
    func onPassFlowStateChanged(state: PassFlowStateUpdate)
    
    func onLocationVerificationRequired(requirement: LocationRequirement)
    
    func onPermissionRequired(type: NeedPermissionType)    
}
