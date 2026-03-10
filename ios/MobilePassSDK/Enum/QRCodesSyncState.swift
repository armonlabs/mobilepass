//
//  QRCodesSyncState.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

public enum QRCodesSyncState {
    case syncStarted
    case syncCompleted(synced: Bool, count: Int)
    case syncFailed(statusCode: Int)
    case dataEmpty
}

