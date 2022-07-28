//
//  CancelReason.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 21.02.2021.
//

import Foundation

enum CancelReason: Int, Codable {
    case USER_CLOSED                    = 1
    case USING_MOCK_LOCATION_DATA       = 2
    case ERROR                          = 3
    case INVALID_QR_CODE                = 4
}
