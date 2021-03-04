//
//  QRCodeListState.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 21.02.2021.
//

import Foundation

enum QRCodeListState: Int, Codable {
    case EMPTY              = 1
    case USING_STORED_DATA  = 2
    case USING_SYNCED_DATA  = 3
}
