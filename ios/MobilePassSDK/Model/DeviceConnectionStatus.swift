//
//  DeviceConnectionStatus.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.02.2021.
//

import Foundation

public struct DeviceConnectionStatus {
    enum ConnectionState: Int {
        case connecting         = 1
        case connected          = 2
        case failed             = 3
        case disconnected       = 4
        case notFound           = 5
    }
    
    var id:             String
    var state:          ConnectionState
    var failReason:     Int?
    var failMessage:    String?
}
