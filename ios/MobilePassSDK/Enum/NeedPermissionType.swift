//
//  NeedPermissionType.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.07.2021.
//

import Foundation

public enum NeedPermissionType: Int, Codable {
    case NEED_PERMISSION_CAMERA         = 1
    case NEED_PERMISSION_LOCATION       = 2
    case NEED_PERMISSION_BLUETOOTH      = 3
    case NEED_ENABLE_BLE                = 4
    case NEED_ENABLE_LOCATION_SERVICES  = 5
}
