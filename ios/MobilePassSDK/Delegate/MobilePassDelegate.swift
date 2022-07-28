//
//  MobilePassDelegate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

public protocol MobilePassDelegate {
    func onPassCancelled(reason: Int)
    func onPassCompleted(result: PassResult)
    func onQRCodeListStateChanged(state: Int)
    func onLogReceived(log: LogItem)
    func onInvalidQRCode(content: String)
}
