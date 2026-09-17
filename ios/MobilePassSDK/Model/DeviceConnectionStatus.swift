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
    /**
     * Displayable text sent by the device.
     *
     * Present on rejections; on success only once the firmware sends one, which
     * is why it is not named after failure. May be truncated when the packet does
     * not fit the negotiated MTU - the result code never is.
     */
    var message:        String?
    /**
     * Result code sent by the device, e.g. A-1001, B-2, C-1234
     *
     * Nil for firmware that does not support result codes, which is expected to
     * be the common case for a while. `failReason` stays the only signal there.
     */
    var resultCode:     String?
}
