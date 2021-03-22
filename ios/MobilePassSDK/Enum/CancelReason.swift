//
//  CancelReason.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 21.02.2021.
//

import Foundation

enum CancelReason: Int, Codable {
    case NEED_PERMISSION_CAMERA         = 1
    case NEED_PERMISSION_LOCATION       = 2
    case USER_CLOSED                    = 3
    case NEED_ENABLE_BLE                = 4
    case NEED_ENABLE_LOCATION_SERVICES  = 5
    case USING_MOCK_LOCATION_DATA       = 6
    case ERROR                          = 7
    case NEED_PERMISSION_BLUETOOTH      = 8
}
