//
//  SDKFlowState.swift
//  MobilePassSDK
//
//  Created by Mobile Pass SDK
//

import Foundation

enum SDKFlowState {
    case idle
    case qrProcessed(QRCodeContent)
    case awaitingLocationVerification(LocationRequirement)
    case locationVerified
    case executingBluetoothAction
    case executingRemoteAction
    case completed(PassFlowResult)
    case cancelled
    case error(String)
}

