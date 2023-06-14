//
//  MobilePassDelegate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

public protocol MobilePassDelegate {
    func onLogReceived(log: LogItem)
    func onInvalidQRCode(content: String)
    
    func onMemberIdChanged()
    func onSyncMemberIdCompleted()
    func onSyncMemberIdFailed(statusCode: Int)
    func onQRCodesDataLoaded(count: Int)
    func onQRCodesSyncStarted()
    func onQRCodesSyncFailed(statusCode: Int)
    func onQRCodesReady(synced: Bool, count: Int)
    func onQRCodesEmpty()
    func onScanFlowCompleted(result: PassFlowResult)
}
