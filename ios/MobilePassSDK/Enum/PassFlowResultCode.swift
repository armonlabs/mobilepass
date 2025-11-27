//
//  PassFlowResultCode.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

enum PassFlowResultCode: Int, Codable {
    case CANCEL     = 1
    case SUCCESS    = 2
    case FAIL       = 3
    case FAIL_PERMISSION = 4
    case FAIL_BLE_DISABLED = 5
    case FAIL_LOCATION_TIMEOUT = 6
}